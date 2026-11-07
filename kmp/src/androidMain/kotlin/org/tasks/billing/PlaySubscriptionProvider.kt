package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaySubscriptionProvider(
    private val purchases: PlayPurchases,
    private val billing: PlayBilling,
) : StoreSubscriptionProvider {
    override val current: SubscriptionProvider.SubscriptionInfo?
        get() = purchases.state.value.subscription?.toSubscriptionInfo()

    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
        purchases.subscription.map { it?.toSubscriptionInfo() }

    override suspend fun getFormattedPrice(sku: String): String? = billing.formattedPrice(sku)

    override suspend fun awaitVerification(): Boolean {
        purchases.load()
        return true
    }

    private fun PlayPurchase.toSubscriptionInfo() = SubscriptionProvider.SubscriptionInfo(
        sku = sku,
        isMonthly = isMonthly,
        isTasksSubscription = isTasksSubscription,
        purchaseToken = purchaseToken,
        provider = EntitlementProvider.PLAY,
    )
}
