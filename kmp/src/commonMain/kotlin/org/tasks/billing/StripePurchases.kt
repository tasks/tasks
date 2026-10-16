package org.tasks.billing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.StripeSubscriptionResult
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
    private val tasksPreferences: TasksPreferences,
    private val logOnboarding: (String) -> Unit = {},
) : StripeCheckouts {
    private val logger = Logger.withTag(TAG)

    override suspend fun prices(): List<StripePrice>? = client.prices()

    override suspend fun createCheckout(price: StripePrice?): StripeCheckout =
        client.createCheckout(price?.priceId)

    override suspend fun collectCheckout(code: String): StripeCollection =
        when (val status = client.pollCheckout(code)) {
            StripeCheckoutStatus.Pending -> StripeCollection.Pending
            is StripeCheckoutStatus.Complete -> {
                val outcome = keep(status.purchase)
                maybeTriggerCloudOnboarding(
                    hasTasksSubscription = SubscriptionSkus.isTasksSubscription(status.purchase.sku),
                    caldavDao = caldavDao,
                    tasksPreferences = tasksPreferences,
                    logStep = logOnboarding,
                )
                StripeCollection.Collected(outcome)
            }
            StripeCheckoutStatus.Gone -> StripeCollection.Gone
            StripeCheckoutStatus.SubscriptionInvalid -> StripeCollection.SubscriptionInvalid
            is StripeCheckoutStatus.RateLimited ->
                StripeCollection.RateLimited(status.retryAfterMillis)
            StripeCheckoutStatus.Unavailable, StripeCheckoutStatus.Failed -> StripeCollection.Pending
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

    suspend fun collectCheckout(code: String): StripeCollection
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
