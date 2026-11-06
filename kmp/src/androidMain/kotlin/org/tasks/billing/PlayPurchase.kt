package org.tasks.billing

import kotlinx.serialization.Serializable

@Serializable
data class PlayPurchase(
    val sku: String,
    val purchaseToken: String,
    val originalJson: String,
    val signature: String,
    val isPurchased: Boolean,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
) {
    val isProSubscription: Boolean
        get() = SubscriptionSkus.isProSubscription(sku)

    val isMonthly: Boolean
        get() = SubscriptionSkus.isMonthly(sku)

    val subscriptionPrice: Int?
        get() = SubscriptionSkus.subscriptionPrice(sku)

    val isTasksSubscription: Boolean
        get() = SubscriptionSkus.isTasksSubscription(sku)

    val isCanceled: Boolean
        get() = !isAutoRenewing

    val needsAcknowledgement: Boolean
        get() = isPurchased && !isAcknowledged

    override fun toString(): String = "PlayPurchase(sku=$sku, purchased=$isPurchased, " +
            "acknowledged=$isAcknowledged, autoRenewing=$isAutoRenewing)"

    companion object {
        const val SKU_VIP = "vip"
        const val SKU_THEMES = "themes"
    }
}
