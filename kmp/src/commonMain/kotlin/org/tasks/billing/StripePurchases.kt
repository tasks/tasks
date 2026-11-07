package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.tasks.auth.HeldPurchaseClaim
import org.tasks.auth.PurchaseClaim
import org.tasks.billing.PurchaseEntitlement.StoreResult
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

    override suspend fun prices(): List<BillingOption>? = client.prices()

    override suspend fun createCheckout(price: BillingOption?): StripeCheckout {
        val result = client.createCheckout(price?.priceId, purchaseJwt = entitlement.getJwt())
        if (result !is StripeCheckout.SubscriptionExists) return result
        val renewed = result.receipt ?: return recoverLapsedRow() ?: result
        logger.i { "The subscription was renewed elsewhere, keeping the receipt it came with" }
        return StripeCheckout.Collected(keep(renewed))
    }

    private suspend fun recoverLapsedRow(): StripeCheckout? {
        if (entitlement.hasPro.first()) return null
        if (entitlement.provider.first() != EntitlementProvider.STRIPE) return null
        logger.i { "Locked on a subscription the server says is live, retrying the refresh" }
        if (!entitlement.retryRefresh()) {
            logger.i { "Still refused: the live subscription is on another of their customers" }
            return StripeCheckout.RestoreRequired
        }
        return StripeCheckout.Collected(StripeRestoreOutcome.Restored)
    }

    override suspend fun changePlan(price: BillingOption): StripeChange {
        claimStoredPurchase()
        val result = client.changePlan(price.priceId, purchaseJwt = entitlement.getJwt())
        if (result is StripeChange.Changed) {
            entitlement.checkTier()
            accountData.fetchAndCache()
            maybeCelebrate(hasTasksSubscription = result.entitles)
        }
        return result
    }

    override suspend fun hasStripeCustomer(): Boolean =
        entitlement.provider.first() == EntitlementProvider.STRIPE

    override suspend fun coveredElsewhere(): ForeignCoverage {
        val account = accountData.getAccountResponse() ?: return ForeignCoverage()
        if (!account.hasForeignCloudSubscription) {
            return ForeignCoverage()
        }
        return ForeignCoverage(
            covered = true,
            source = when (val provider = account.foreignCloudProvider) {
                TasksAccountResponse.PROVIDER_PLAY -> UnlockSource.GOOGLE_PLAY
                TasksAccountResponse.PROVIDER_GITHUB_SPONSOR -> UnlockSource.GITHUB
                else -> {
                    logger.w { "Unknown coverage provider: $provider" }
                    null
                }
            },
        )
    }

    override suspend fun currentPlan(refresh: Boolean): StripePlan? {
        if (refresh) {
            entitlement.checkTier()
        }
        return if (entitlement.hasPro.first() &&
            entitlement.provider.first() == EntitlementProvider.STRIPE
        ) {
            StripePlan(entitlement.sku.first(), entitlement.terms.first())
        } else {
            accountData.getAccountResponse()
                ?.takeIf { it.hasStripeSubscription }
                ?.let {
                    StripePlan(
                        sku = it.stripeProduct,
                        terms = it.stripeTerms(),
                        covered = it.stripeCovered ?: true,
                    )
                }
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
            StripeCheckoutStatus.Unavailable, StripeCheckoutStatus.Failed ->
                StripeCollection.Unreachable
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

    suspend fun verifyRestoreCode(email: String, code: String): StripeRestore =
        client.verifyRestoreCode(email, code)

    suspend fun restore(
        purchase: StripePurchase,
        covered: Boolean = true,
        terms: PurchaseTerms? = null,
        requireCloud: Boolean = true,
    ): StripeRestoreOutcome {
        val outcome = keep(purchase, covered, terms)
        if (outcome != StripeRestoreOutcome.Restored) return outcome
        if (!requireCloud || SubscriptionSkus.isTasksSubscription(purchase.sku)) return outcome
        logger.i { "Restored a subscription that does not reach the cloud tier" }
        return StripeRestoreOutcome.NeedsUpgrade
    }

    suspend fun keep(
        purchase: StripePurchase,
        covered: Boolean = true,
        terms: PurchaseTerms? = null,
    ): StripeRestoreOutcome = keep(
        jwt = purchase.purchaseJwt,
        refreshToken = purchase.refreshToken,
        sku = purchase.sku,
        formattedPrice = purchase.formattedPrice,
        covered = covered,
        terms = terms,
    )

    private suspend fun keep(receipt: StripeReceipt): StripeRestoreOutcome = keep(
        jwt = receipt.purchaseJwt,
        refreshToken = null,
        sku = receipt.sku,
        formattedPrice = receipt.formattedPrice,
    )

    private suspend fun keep(
        jwt: String,
        refreshToken: String?,
        sku: String?,
        formattedPrice: String?,
        covered: Boolean = true,
        terms: PurchaseTerms? = null,
    ): StripeRestoreOutcome {
        val stored = entitlement.storeEntitlement(
            jwt = jwt,
            refreshToken = refreshToken,
            sku = sku,
            formattedPrice = formattedPrice,
            provider = EntitlementProvider.STRIPE,
            terms = terms,
            covered = covered,
        )
        if (stored != StoreResult.STORED) {
            logger.e { "Could not store the purchase: $stored" }
            return StripeRestoreOutcome.StorageFailed(
                encryptionFailed = stored == StoreResult.ENCRYPT_FAILED,
            )
        }
        val claim = claim(jwt)
        if (claim is StripeSubscriptionResult.TokenInUse) {
            return StripeRestoreOutcome.AccountInUse(claim.existingAccount)
        }
        if (!covered) {
            return StripeRestoreOutcome.Ended
        }
        return when (claim) {
            null, StripeSubscriptionResult.Claimed -> StripeRestoreOutcome.Restored
            StripeSubscriptionResult.InsufficientSubscription -> StripeRestoreOutcome.NeedsUpgrade
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

    override suspend fun claimHeldPurchase(username: String, token: String): HeldPurchaseClaim {
        val receipt = entitlement.heldReceipt() ?: return HeldPurchaseClaim.NotClaimed
        val factory = clientFactory ?: return HeldPurchaseClaim.NotClaimed
        if (receipt.provider != EntitlementProvider.STRIPE) {
            logger.i { "Holding a ${receipt.provider} receipt, Stripe has nothing to claim" }
            return HeldPurchaseClaim.NotClaimed
        }
        val result = try {
            factory.forTasksCredentials(username, token).use {
                it.claimStripeSubscription(receipt.jwt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Failed to claim the purchase at sign-in" }
            return HeldPurchaseClaim.NotClaimed
        }
        return when (result) {
            StripeSubscriptionResult.Claimed -> {
                logger.i { "Claimed at sign-in" }
                HeldPurchaseClaim.Claimed
            }
            is StripeSubscriptionResult.TokenInUse -> if (receipt.covers) {
                logger.i { "The subscription held here funds ${result.existingAccount}" }
                HeldPurchaseClaim.CoveringAnotherAccount(result.existingAccount)
            } else {
                logger.i {
                    "A receipt that covers nothing funds ${result.existingAccount}, " +
                        "signing in without it"
                }
                HeldPurchaseClaim.NotClaimed
            }
            else -> {
                logger.w { "Not claimed at sign-in: $result" }
                HeldPurchaseClaim.NotClaimed
            }
        }
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

    data object Ended : StripeRestoreOutcome

    data object NeedsUpgrade : StripeRestoreOutcome

    data class AccountInUse(val existingAccount: String) : StripeRestoreOutcome

    data class StorageFailed(val encryptionFailed: Boolean) : StripeRestoreOutcome
}

interface StripeCheckouts {
    suspend fun prices(): List<BillingOption>?

    suspend fun createCheckout(price: BillingOption?): StripeCheckout

    suspend fun changePlan(price: BillingOption): StripeChange

    suspend fun currentPlan(refresh: Boolean = true): StripePlan?

    suspend fun hasStripeCustomer(): Boolean

    suspend fun coveredElsewhere(): ForeignCoverage

    suspend fun collectCheckout(code: String): StripeCollection
}

data class ForeignCoverage(
    val covered: Boolean = false,
    val source: UnlockSource? = null,
)

data class StripePlan(
    val sku: String?,
    val terms: PurchaseTerms? = null,
    val covered: Boolean = true,
) {
    val trialEnd: Long? get() = terms?.takeIf { it.isTrialing }?.trialEnd

    val canChange: Boolean get() = terms?.isBilling ?: covered

    val resumable: Boolean get() = terms?.cancelAtPeriodEnd == true

    val sellsSameSku: Boolean get() = !canChange || resumable
}

sealed interface StripeCollection {
    data object Pending : StripeCollection

    data object Unreachable : StripeCollection

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

fun TasksAccountResponse.stripeTerms(): PurchaseTerms? = stripeRow.let { row ->
    PurchaseTerms(
        status = row?.status,
        trialEnd = row?.trialEnd,
        expires = row?.expires,
        cancelAtPeriodEnd = row?.cancelAtPeriodEnd ?: false,
        creditCents = creditCents,
        creditCurrency = creditCurrency,
    ).orNull()
}
