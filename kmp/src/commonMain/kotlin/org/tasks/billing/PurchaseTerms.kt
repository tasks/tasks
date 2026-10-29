package org.tasks.billing

import kotlinx.serialization.Serializable
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@Serializable
data class PurchaseTerms(
    val status: String? = null,
    val trialEnd: Long? = null,
    val expires: Long? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val creditCents: Int? = null,
    val creditCurrency: String? = null,
) {
    val isTrialing: Boolean
        get() = status == STATUS_TRIALING ||
            (status == null && trialEnd != null && trialEnd > currentTimeMillis())

    val isBilling: Boolean?
        get() = when (status) {
            STATUS_ACTIVE, STATUS_TRIALING, STATUS_PAST_DUE -> true
            STATUS_CANCELED, STATUS_INCOMPLETE, STATUS_INCOMPLETE_EXPIRED,
            STATUS_UNPAID, STATUS_PAUSED -> false
            else -> null
        }

    val isEmpty: Boolean
        get() = status == null &&
            trialEnd == null &&
            expires == null &&
            !cancelAtPeriodEnd &&
            creditCents == null &&
            creditCurrency == null

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_TRIALING = "trialing"
        const val STATUS_PAST_DUE = "past_due"
        const val STATUS_CANCELED = "canceled"
        const val STATUS_INCOMPLETE = "incomplete"
        const val STATUS_INCOMPLETE_EXPIRED = "incomplete_expired"
        const val STATUS_UNPAID = "unpaid"
        const val STATUS_PAUSED = "paused"
    }
}

fun PurchaseTerms.orNull(): PurchaseTerms? = takeIf { !it.isEmpty }

sealed interface Renewal {
    data class Trial(val until: Long, val price: String) : Renewal

    data object PaymentFailed : Renewal

    data class Ends(val on: Long) : Renewal

    data class Ended(val on: Long) : Renewal

    data class Renews(val on: Long) : Renewal
}

fun PurchaseTerms.renewal(price: String?): Renewal? = when {
    isTrialing -> trialEnd?.let { until -> price?.let { Renewal.Trial(until, it) } }
    status == PurchaseTerms.STATUS_PAST_DUE -> Renewal.PaymentFailed
    isBilling == false || cancelAtPeriodEnd -> expires?.endsOrEnded()
    else -> expires?.let { Renewal.Renews(it) }
}

private fun Long.endsOrEnded(): Renewal =
    if (this > currentTimeMillis()) Renewal.Ends(this) else Renewal.Ended(this)

internal fun formatCredit(cents: Int?, currency: String?): String? {
    if (cents == null || cents <= 0 || currency == null) {
        return null
    }
    return formatAmount(cents, currency)
}
