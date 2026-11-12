package org.tasks.billing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    val orderId: String?
        get() = try {
            Json.parseToJsonElement(originalJson).jsonObject["orderId"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }

    override fun toString(): String = "PlayPurchase(sku=$sku, purchased=$isPurchased, " +
            "acknowledged=$isAcknowledged, autoRenewing=$isAutoRenewing)"

    companion object {
        const val SKU_VIP = "vip"
        const val SKU_THEMES = "themes"
    }
}
