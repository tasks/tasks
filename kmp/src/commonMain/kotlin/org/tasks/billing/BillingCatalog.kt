package org.tasks.billing

import kotlin.math.roundToInt

const val DEFAULT_NAME_YOUR_PRICE = 10

data class BillingCatalog(
    val cloud: List<BillingOption> = emptyList(),
    val support: List<BillingOption> = emptyList(),
) {
    val isEmpty: Boolean get() = cloud.isEmpty() && support.isEmpty()

    val cloudMonthly: BillingOption? get() = cloud.firstOrNull { it.isMonthly }

    val cloudAnnual: BillingOption? get() = cloud.firstOrNull { !it.isMonthly }

    fun price(sku: String?): BillingOption? =
        (cloud + support).firstOrNull { it.sku == sku }

    val supporterRungs: List<BillingOption> = support.filterNot { it.isMonthly }
        .sortedWith(compareBy({ it.rungDollars }, { it.amountCents }))

    fun supporterRungAt(position: Float): BillingOption? {
        val rungs = supporterRungs
        return if (rungs.isEmpty()) {
            null
        } else {
            rungs[position.roundToInt().coerceIn(0, rungs.lastIndex)]
        }
    }

    fun supporterRungFor(dollars: Int?): Int {
        val rungs = supporterRungs
        val tier = dollars ?: DEFAULT_NAME_YOUR_PRICE
        return rungs.indexOfFirst { it.rungDollars >= tier }
            .takeIf { it >= 0 }
            ?: rungs.lastIndex
    }

    fun supporterMonthlyFor(rung: BillingOption): BillingOption? =
        support.firstOrNull { it.isMonthly && it.rungDollars == rung.rungDollars }

    companion object {
        fun from(prices: List<BillingOption>): BillingCatalog {
            val (cloud, support) = prices
                .sortedWith(compareByDescending<BillingOption> { it.isMonthly }.thenBy { it.amountCents })
                .partition { it.entitles }
            return BillingCatalog(cloud = cloud, support = support)
        }
    }
}

data class BillingOption(
    val priceId: String,
    val sku: String,
    val amountCents: Int,
    val currency: String = "USD",
    val interval: String = INTERVAL_YEAR,
    val entitles: Boolean = false,
    val trialDays: Int = 0,
    val formatted: String? = null,
) {
    val isMonthly: Boolean get() = interval == INTERVAL_MONTH

    internal val rungDollars: Int =
        SubscriptionSkus.subscriptionPrice(sku) ?: ((amountCents + 99) / 100)

    val formattedPrice: String get() = formatted ?: formatAmount(amountCents, currency)

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
