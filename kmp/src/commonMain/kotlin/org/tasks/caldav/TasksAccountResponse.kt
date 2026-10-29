package org.tasks.caldav

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TasksAccountResponse(
    val createdAt: Long? = null,
    val subscription: Subscription? = null,
    val subscriptions: List<SubscriptionSource> = emptyList(),
    val supporter: List<SupporterSource> = emptyList(),
    @SerialName("inbound_email") val inboundEmail: InboundEmail? = null,
    @SerialName("app_passwords") val appPasswords: List<AppPassword> = emptyList(),
    val guest: Boolean = false,
    @SerialName("shared_with_me") val sharedWithMe: List<String> = emptyList(),
    val guests: List<Guest> = emptyList(),
    @SerialName("max_guests") val maxGuests: Int = 5,
    @SerialName("credit_cents") val creditCents: Int? = null,
    @SerialName("credit_currency") val creditCurrency: String? = null,
) {
    val stripeCloudRow: SubscriptionSource?
        get() = subscriptions.firstOrNull { it.provider == PROVIDER_STRIPE }

    val stripeSupporterRow: SupporterSource?
        get() = supporter.firstOrNull { it.provider == PROVIDER_STRIPE }

    val stripeRow: StripeRow?
        get() = stripeCloudRow ?: stripeSupporterRow

    val hasStripeSubscription: Boolean
        get() = stripeRow != null || subscription?.provider == PROVIDER_STRIPE

    val hasAnySubscription: Boolean
        get() = subscriptions.isNotEmpty() || supporter.isNotEmpty() || subscription?.free == false

    val hasCloudSubscription: Boolean
        get() = subscriptions.isNotEmpty()

    val stripeProduct: String?
        get() = stripeRow?.product

    sealed interface StripeRow {
        val provider: String?
        val product: String?
        val status: String?
        val expires: Long?
        val cancelAtPeriodEnd: Boolean
        val trialEnd: Long?
    }

    @Serializable
    data class SubscriptionSource(
        override val provider: String? = null,
        override val expires: Long? = null,
        val isActive: Boolean = false,
        override val status: String? = null,
        @SerialName("cancel_at_period_end") override val cancelAtPeriodEnd: Boolean = false,
        @SerialName("trial_end") override val trialEnd: Long? = null,
        override val product: String? = null,
    ) : StripeRow

    @Serializable
    data class SupporterSource(
        override val provider: String? = null,
        override val product: String? = null,
        @SerialName("formatted_price") val formattedPrice: String? = null,
        override val status: String? = null,
        override val expires: Long? = null,
        @SerialName("cancel_at_period_end") override val cancelAtPeriodEnd: Boolean = false,
        @SerialName("trial_end") override val trialEnd: Long? = null,
    ) : StripeRow

    @Serializable
    data class Subscription(
        val free: Boolean = true,
        val provider: String? = null,
        val expiration: Long? = null,
    )

    @Serializable
    data class InboundEmail(
        val email: String? = null,
        val calendar: String? = null,
    )

    @Serializable
    data class AppPassword(
        val description: String? = null,
        @SerialName("session_id") val sessionId: Int = -1,
        @SerialName("created_at") val createdAt: Long? = null,
        @SerialName("last_access") val lastAccess: Long? = null,
    )

    @Serializable
    data class Guest(
        @SerialName("display_name") val displayName: String? = null,
        val email: String? = null,
    )

    companion object {
        const val PROVIDER_STRIPE = "stripe"
    }
}
