package org.tasks.billing

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidSubscriptionProvider(
    private val inventory: Inventory,
    private val billingClient: BillingClient,
) : SubscriptionProvider {
    override val subscription: Flow<SubscriptionProvider.SubscriptionInfo?>
        get() = inventory.subscription.asFlow().map { purchase ->
            purchase?.let {
                SubscriptionProvider.SubscriptionInfo(
                    sku = it.sku,
                    isMonthly = it.isMonthly,
                    isTasksSubscription = it.isTasksSubscription,
                    purchaseToken = it.purchaseToken,
                )
            }
        }

    override suspend fun getFormattedPrice(sku: String): String? =
        billingClient.getSku(sku)?.price
}
