package org.tasks.billing

data class PlayProductInfo(
    val productId: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val trialPeriod: String? = null,
)

object PlayCatalog {
    val ALL_SKUS: List<String> =
        SubscriptionSkus.SUPPORTER_LADDER.map { SubscriptionSkus.sku(it) }
            .plus((1..SubscriptionSkus.CLOUD_MONTHLY_FLOOR).map { SubscriptionSkus.sku(it, monthly = true) })
            .plus(SubscriptionSkus.sku(SubscriptionSkus.CLOUD_ANNUAL_FLOOR))

    private val PERIOD = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$")

    fun periodDays(period: String?): Int {
        val match = period?.let { PERIOD.matchEntire(it) } ?: return 0
        val (years, months, weeks, days) = match.destructured
        return years.toIntOrZero() * 365 +
                months.toIntOrZero() * 30 +
                weeks.toIntOrZero() * 7 +
                days.toIntOrZero()
    }

    fun PlayProductInfo.toBillingOption() = BillingOption(
        priceId = productId,
        sku = productId,
        amountCents = (priceAmountMicros / MICROS_PER_CENT).toInt(),
        currency = priceCurrencyCode,
        interval = if (SubscriptionSkus.isMonthly(productId)) {
            BillingOption.INTERVAL_MONTH
        } else {
            BillingOption.INTERVAL_YEAR
        },
        entitles = SubscriptionSkus.isTasksSubscription(productId),
        trialDays = periodDays(trialPeriod),
        formatted = formattedPrice.takeIf { it.isNotBlank() },
    )

    fun toCatalog(products: List<PlayProductInfo>): BillingCatalog =
        BillingCatalog.from(products.map { it.toBillingOption() })

    private const val MICROS_PER_CENT = 10_000L

    private fun String.toIntOrZero() = toIntOrNull() ?: 0
}
