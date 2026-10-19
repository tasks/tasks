package org.tasks.billing

import kotlinx.coroutines.flow.Flow

interface SubscriptionProvider {
    data class SubscriptionInfo(
        val sku: String,
        val isMonthly: Boolean,
        val isTasksSubscription: Boolean,
        val purchaseToken: String? = null,
        val isGitHubSponsor: Boolean = false,
        val isCardSubscription: Boolean = false,
        val terms: PurchaseTerms? = null,
    )

    val subscription: Flow<SubscriptionInfo?>
    suspend fun getFormattedPrice(sku: String): String?

    suspend fun awaitVerification(): Boolean = true

    suspend fun checkTier(): Boolean = false

    val googleAndMicrosoftRequirePro: Boolean get() = false
}
