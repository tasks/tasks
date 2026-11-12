package org.tasks.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class PlaySubscriptionProvider(
    private val purchases: PlayPurchases,
    private val billing: PlayBilling,
) : StoreSubscriptionProvider {
    override val current: SubscriptionProvider.SubscriptionInfo?
        get() = purchases.state.value.subscription?.toSubscriptionInfo()

    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?> =
        purchases.subscription.map { it?.toSubscriptionInfo() }

    private val prices = ConcurrentHashMap<String, String>()

    override suspend fun getFormattedPrice(sku: String): String? =
        prices[sku] ?: billing.formattedPrice(sku)?.also { prices[sku] = it }

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
