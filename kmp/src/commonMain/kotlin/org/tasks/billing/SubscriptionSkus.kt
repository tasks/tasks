package org.tasks.billing

object SubscriptionSkus {
    const val CLOUD_ANNUAL = "annual_30"
    const val CLOUD_MONTHLY = "monthly_03"

    const val CLOUD_MONTHLY_FLOOR = 3
    const val CLOUD_ANNUAL_FLOOR = 30

    private val PATTERN = Regex("^(annual|monthly)_(\\d+)$")

    fun sku(dollars: Int, monthly: Boolean = false): String =
        (if (monthly) "monthly_" else "annual_") + dollars.toString().padStart(2, '0')

    val SUPPORTER_LADDER: IntRange = 1..25

    fun isProSubscription(sku: String?): Boolean = sku != null && PATTERN.matches(sku)

    fun isMonthly(sku: String?): Boolean = sku?.startsWith("monthly") == true

    fun subscriptionPrice(sku: String?): Int? = sku
        ?.let { PATTERN.matchEntire(it) }
        ?.groupValues
        ?.get(2)
        ?.toIntOrNull()
        ?.let { if (it == 499) 5 else it }

    fun isTasksSubscription(sku: String?): Boolean {
        val price = subscriptionPrice(sku) ?: return false
        return price >= if (isMonthly(sku)) CLOUD_MONTHLY_FLOOR else CLOUD_ANNUAL_FLOOR
    }
}
