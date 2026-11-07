package org.tasks.billing

import kotlinx.coroutines.flow.Flow

interface SubscriptionProvider {
    data class SubscriptionInfo(
        val sku: String,
        val isMonthly: Boolean,
        val isTasksSubscription: Boolean,
        val purchaseToken: String? = null,
        val provider: EntitlementProvider? = null,
        val terms: PurchaseTerms? = null,
    ) {
        val isSupporterSubscription: Boolean
            get() = SubscriptionSkus.supporterPrice(sku) != null
    }

    val subscription: Flow<SubscriptionInfo?>

    val current: SubscriptionInfo? get() = null

    suspend fun getFormattedPrice(sku: String): String?

    suspend fun awaitVerification(): Boolean = true

    suspend fun checkTier(): Boolean = false

    val googleAndMicrosoftRequirePro: Boolean get() = false
}

enum class ModifySubscription {
    NONE,
    CHANGE_PLAN,
    OPEN_SPONSOR_PAGE,
}

fun EntitlementProvider?.modifySubscription(
    unattributed: ModifySubscription = ModifySubscription.NONE,
): ModifySubscription = when (this) {
    EntitlementProvider.GITHUB_SPONSOR -> ModifySubscription.OPEN_SPONSOR_PAGE
    EntitlementProvider.PLAY, EntitlementProvider.STRIPE -> ModifySubscription.CHANGE_PLAN
    null -> unattributed
}
