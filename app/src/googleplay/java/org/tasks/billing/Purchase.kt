package org.tasks.billing

import com.android.billingclient.api.Purchase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tasks.billing.BillingClientImpl.Companion.STATE_PURCHASED
import java.util.regex.Pattern

class Purchase(private val purchase: Purchase) {

    constructor(json: String) : this(
        Json.parseToJsonElement(json).jsonObject.let {
            Purchase(it["zza"]!!.jsonPrimitive.content, it["zzb"]!!.jsonPrimitive.content)
        }
    )

    fun toJson(): String {
        return Json.encodeToString(mapOf("zza" to purchase.originalJson, "zzb" to purchase.signature))
    }

    override fun toString(): String {
        return "Purchase(purchase=$purchase)"
    }

    val originalJson: String
        get() = purchase.originalJson

    val signature: String
        get() = purchase.signature

    val sku: String
        get() = purchase.skus.first()

    val purchaseToken: String
        get() = purchase.purchaseToken

    val isProSubscription: Boolean
        get() = PATTERN.matcher(sku).matches()

    val isMonthly: Boolean
        get() = sku.startsWith("monthly")

    val isCanceled: Boolean
        get() = !purchase.isAutoRenewing

    val needsAcknowledgement: Boolean
        get() = purchase.needsAcknowledgement

    val isPurchased: Boolean
        get() = purchase.isPurchased

    val subscriptionPrice: Int?
        get() {
            val matcher = PATTERN.matcher(sku)
            if (matcher.matches()) {
                val price = matcher.group(2).toInt()
                return if (price == 499) 5 else price
            }
            return null
        }

    val isTasksSubscription: Boolean
        get() {
            return subscriptionPrice
                    ?.let {
                        if (isMonthly) {
                            it >= 3
                        } else {
                            it >= 30
                        }
                    }
                    ?: false
        }

    companion object {
        private val PATTERN = Pattern.compile("^(annual|monthly)_([0-3][0-9]|499)$")

        val Purchase.isPurchased: Boolean
            get() = purchaseState == STATE_PURCHASED

        val Purchase.needsAcknowledgement: Boolean
            get() = isPurchased && !isAcknowledged
    }
}