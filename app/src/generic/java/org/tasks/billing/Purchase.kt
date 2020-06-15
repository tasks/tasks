package org.tasks.billing

@Suppress("UNUSED_PARAMETER")
class Purchase(json: String?) {
    val sku: String?
        get() = null

    fun toJson(): String? {
        return null
    }

    val isCanceled: Boolean
        get() = false

    val subscriptionPrice: Int
        get() = 0

    val isMonthly: Boolean
        get() = false

    val isProSubscription: Boolean
        get() = false
}