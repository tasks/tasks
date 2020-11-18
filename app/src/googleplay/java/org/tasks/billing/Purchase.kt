package org.tasks.billing

import com.android.billingclient.api.Purchase
import com.google.gson.GsonBuilder
import java.util.regex.Pattern

class Purchase(private val purchase: Purchase) {

    constructor(json: String?) : this(GsonBuilder().create().fromJson<Purchase>(json, Purchase::class.java))

    fun toJson(): String {
        return GsonBuilder().create().toJson(purchase)
    }

    override fun toString(): String {
        return "Purchase(purchase=$purchase)"
    }

    val originalJson: String
        get() = purchase.originalJson

    val signature: String
        get() = purchase.signature

    val sku: String
        get() = purchase.sku

    val purchaseToken: String
        get() = purchase.purchaseToken

    val isProSubscription: Boolean
        get() = PATTERN.matcher(sku).matches()

    val isMonthly: Boolean
        get() = sku.startsWith("monthly")

    val isCanceled: Boolean
        get() = !purchase.isAutoRenewing

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
    }
}