package org.tasks.billing

import kotlinx.coroutines.flow.Flow

interface SubscriptionProvider {
    data class SubscriptionInfo(
        val sku: String,
        val isMonthly: Boolean,
        val isTasksSubscription: Boolean,
        val purchaseToken: String? = null,
    )

    val subscription: Flow<SubscriptionInfo?>
    suspend fun getFormattedPrice(sku: String): String?
}
