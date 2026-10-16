package org.tasks.billing

data class StripeCatalog(
    val cloud: List<StripePrice> = emptyList(),
    val support: List<StripePrice> = emptyList(),
) {
    val isEmpty: Boolean get() = cloud.isEmpty() && support.isEmpty()

    val cloudMonthly: StripePrice? get() = cloud.firstOrNull { it.isMonthly }

    val cloudAnnual: StripePrice? get() = cloud.firstOrNull { !it.isMonthly }

    fun price(sku: String?): StripePrice? =
        (cloud + support).firstOrNull { it.sku == sku }

    companion object {
        fun from(prices: List<StripePrice>): StripeCatalog {
            val (cloud, support) = prices
                .sortedWith(compareByDescending<StripePrice> { it.isMonthly }.thenBy { it.amountCents })
                .partition { it.entitles }
            return StripeCatalog(cloud = cloud, support = support)
        }
    }
}

data class StripePrice(
    val priceId: String,
    val sku: String,
    val amountCents: Int,
    val currency: String = "USD",
    val interval: String = INTERVAL_YEAR,
    val entitles: Boolean = false,
    val trialDays: Int = 0,
) {
    val isMonthly: Boolean get() = interval == INTERVAL_MONTH

    val formattedPrice: String get() = formatAmount(amountCents, currency)

    val formattedMonthlyPrice: String
        get() = if (isMonthly) {
            formattedPrice
        } else {
            formatAmount((amountCents + MONTHS_PER_YEAR / 2) / MONTHS_PER_YEAR, currency)
        }

    companion object {
        const val INTERVAL_MONTH = "month"
        const val INTERVAL_YEAR = "year"
        private const val MONTHS_PER_YEAR = 12
    }
}

internal fun formatAmount(cents: Int, currency: String): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    val amount = "$whole.$fraction"
    return if (currency == "USD") "$$amount" else "$amount $currency"
}
