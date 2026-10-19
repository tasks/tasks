package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.tasks.auth.PurchaseClaim
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.StripeSubscriptionResult
import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.caldav.TasksAccountResponse
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.tasksAccount
import org.tasks.http.basicCredentials
import org.tasks.preferences.TasksPreferences
import org.tasks.security.Encryption

class StripePurchases(
    private val client: StripeClient,
    private val entitlement: PurchaseEntitlement,
    private val caldavDao: CaldavDao,
    private val clientFactory: CaldavClientFactory?,
    private val accountData: TasksAccountDataRepository,
    private val tasksPreferences: TasksPreferences,
    private val logOnboarding: (String) -> Unit = {},
) : StripeCheckouts, PurchaseClaim {
    private val logger = Logger.withTag(TAG)

    override suspend fun prices(): List<StripePrice>? = client.prices()

    override suspend fun createCheckout(price: StripePrice?): StripeCheckout =
        client.createCheckout(price?.priceId)

    override suspend fun changePlan(price: StripePrice): StripeChange {
        claimStoredPurchase()
        val result = client.changePlan(price.priceId, purchaseJwt = entitlement.getJwt())
        if (result is StripeChange.Changed) {
            entitlement.checkTier()
            accountData.fetchAndCache()
            maybeCelebrate(hasTasksSubscription = result.entitles)
        }
        return result
    }

    override suspend fun currentPlan(): StripePlan? {
        entitlement.checkTier()
        return if (entitlement.provider.first() == EntitlementProvider.STRIPE) {
            StripePlan(entitlement.sku.first(), entitlement.terms.first())
        } else {
            accountData.getAccountResponse()
                ?.takeIf { it.hasStripeSubscription }
                ?.let { StripePlan(it.stripeProduct, it.stripeTerms()) }
        }
    }

    override suspend fun collectCheckout(code: String): StripeCollection =
        when (val status = client.pollCheckout(code)) {
            StripeCheckoutStatus.Pending -> StripeCollection.Pending
            is StripeCheckoutStatus.Complete -> {
                val outcome = keep(status.purchase)
                maybeCelebrate(
                    hasTasksSubscription =
                        SubscriptionSkus.isTasksSubscription(status.purchase.sku),
                )
                StripeCollection.Collected(outcome)
            }
            StripeCheckoutStatus.Gone -> StripeCollection.Gone
            StripeCheckoutStatus.SubscriptionInvalid -> StripeCollection.SubscriptionInvalid
            is StripeCheckoutStatus.RateLimited ->
                StripeCollection.RateLimited(status.retryAfterMillis)
            StripeCheckoutStatus.Unavailable, StripeCheckoutStatus.Failed -> StripeCollection.Pending
        }

    private suspend fun maybeCelebrate(hasTasksSubscription: Boolean) =
        maybeTriggerCloudOnboarding(
            hasTasksSubscription = hasTasksSubscription,
            caldavDao = caldavDao,
            tasksPreferences = tasksPreferences,
            logStep = logOnboarding,
        )

    suspend fun requestRestoreCode(email: String): StripeRestoreRequest =
        client.requestRestoreCode(email)

    suspend fun redeemRestoreCode(email: String, code: String): StripeRestore {
        val result = client.verifyRestoreCode(email, code)
        if (result is StripeRestore.Restored) {
            keep(result.purchase)
        }
        return result
    }

    suspend fun keep(purchase: StripePurchase): StripeRestoreOutcome {
        val stored = entitlement.storeEntitlement(
            jwt = purchase.purchaseJwt,
            refreshToken = purchase.refreshToken,
            sku = purchase.sku,
            formattedPrice = purchase.formattedPrice,
            provider = EntitlementProvider.STRIPE,
        )
        if (!stored) {
            logger.e { "Could not store the purchase" }
            return StripeRestoreOutcome.StorageFailed
        }
        return when (val claim = claim(purchase.purchaseJwt)) {
            null, StripeSubscriptionResult.Claimed -> StripeRestoreOutcome.Restored
            StripeSubscriptionResult.InsufficientSubscription -> StripeRestoreOutcome.NeedsUpgrade
            is StripeSubscriptionResult.TokenInUse ->
                StripeRestoreOutcome.AccountInUse(claim.existingAccount)
            else -> {
                logger.w { "Stored the purchase but could not claim it: $claim" }
                StripeRestoreOutcome.Restored
            }
        }
    }

    suspend fun claimStoredPurchase(): StripeSubscriptionResult? {
        val jwt = entitlement.getJwt() ?: return null
        val account = accountData.fetchAndCache() ?: accountData.getAccountResponse()
        if (account?.hasStripeSubscription == true) {
            return null
        }
        return claim(jwt)
    }

    override suspend fun claimHeldPurchase(username: String, token: String) {
        val jwt = entitlement.getJwt() ?: return
        val factory = clientFactory ?: return
        val result = try {
            factory.forTasksCredentials(username, token).use { it.claimStripeSubscription(jwt) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to claim the purchase at sign-in" }
            return
        }
        logger.i { "Claimed at sign-in: $result" }
    }

    suspend fun portalUrl(): String? = client.portalUrl(entitlement.getJwt())

    fun claimOnSignIn(scope: CoroutineScope) {
        scope.launch {
            try {
                caldavDao.watchAccounts()
                    .map { accounts -> accounts.firstOrNull { it.isTasksOrg }?.uuid }
                    .distinctUntilChanged()
                    .filterNotNull()
                    .collect {
                        try {
                            claimStoredPurchase()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.e(e) { "Failed to claim on sign-in" }
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Stopped watching for sign-ins" }
            }
        }
    }

    private suspend fun claim(purchaseJwt: String): StripeSubscriptionResult? {
        val factory = clientFactory ?: return null
        val account = caldavDao.tasksAccount() ?: return null
        return try {
            factory.forTasksAccount(account).use { it.claimStripeSubscription(purchaseJwt) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to claim the purchase" }
            StripeSubscriptionResult.Failed
        }
    }

    companion object {
        private const val TAG = "StripePurchases"
    }
}

sealed interface StripeRestoreOutcome {
    data object Restored : StripeRestoreOutcome

    data object NeedsUpgrade : StripeRestoreOutcome

    data class AccountInUse(val existingAccount: String) : StripeRestoreOutcome

    data object StorageFailed : StripeRestoreOutcome
}

interface StripeCheckouts {
    suspend fun prices(): List<StripePrice>?

    suspend fun createCheckout(price: StripePrice?): StripeCheckout

    suspend fun changePlan(price: StripePrice): StripeChange

    suspend fun currentPlan(): StripePlan?

    suspend fun collectCheckout(code: String): StripeCollection
}

data class StripePlan(val sku: String?, val terms: PurchaseTerms? = null) {
    val trialEnd: Long? get() = terms?.takeIf { it.isTrialing }?.trialEnd
}

fun TasksAccountResponse.stripeTerms(): PurchaseTerms? {
    val cloud = subscriptions.firstOrNull { it.provider == TasksAccountResponse.PROVIDER_STRIPE }
    val supporter = supporter.firstOrNull { it.provider == TasksAccountResponse.PROVIDER_STRIPE }
    return if (cloud != null) {
        PurchaseTerms(
            trialEnd = cloud.trialEnd,
            creditCents = creditCents,
            creditCurrency = creditCurrency,
        )
    } else {
        PurchaseTerms(
            status = supporter?.status,
            trialEnd = supporter?.trialEnd,
            creditCents = creditCents,
            creditCurrency = creditCurrency,
        )
    }.takeIf { !it.isEmpty }
}

sealed interface StripeCollection {
    data object Pending : StripeCollection

    data class RateLimited(val retryAfterMillis: Long?) : StripeCollection

    data class Collected(val outcome: StripeRestoreOutcome) : StripeCollection

    data object Gone : StripeCollection

    data object SubscriptionInvalid : StripeCollection
}

suspend fun tasksSessionAuthorization(
    caldavDao: CaldavDao,
    encryption: Encryption,
): String? {
    val account = caldavDao.tasksAccount() ?: return null
    val user = account.username?.takeIf { it.isNotBlank() } ?: return null
    val password = encryption.decrypt(account.password)?.takeIf { it.isNotBlank() } ?: return null
    return basicCredentials(user, password)
}
