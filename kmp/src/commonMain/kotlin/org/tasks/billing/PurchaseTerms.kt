package org.tasks.billing

import kotlinx.serialization.Serializable
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@Serializable
data class PurchaseTerms(
    val status: String? = null,
    val trialEnd: Long? = null,
    val creditCents: Int? = null,
    val creditCurrency: String? = null,
) {
    val isTrialing: Boolean
        get() = status == STATUS_TRIALING ||
            (status == null && trialEnd != null && trialEnd > currentTimeMillis())

    val isEmpty: Boolean
        get() = status == null &&
            trialEnd == null &&
            creditCents == null &&
            creditCurrency == null

    companion object {
        const val STATUS_TRIALING = "trialing"
    }
}

internal fun formatCredit(cents: Int?, currency: String?): String? {
    if (cents == null || cents <= 0 || currency == null) {
        return null
    }
    return formatAmount(cents, currency)
}
