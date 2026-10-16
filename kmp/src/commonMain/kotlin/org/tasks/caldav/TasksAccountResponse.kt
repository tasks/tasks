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
) {
    val hasStripeSubscription: Boolean
        get() = subscriptions.any { it.provider == PROVIDER_STRIPE } ||
            supporter.any { it.provider == PROVIDER_STRIPE } ||
            subscription?.provider == PROVIDER_STRIPE

    val hasAnySubscription: Boolean
        get() = subscriptions.isNotEmpty() || supporter.isNotEmpty() || subscription?.free == false

    val hasCloudSubscription: Boolean
        get() = subscriptions.isNotEmpty()

    val stripeProduct: String?
        get() = subscriptions.firstOrNull { it.provider == PROVIDER_STRIPE }?.product
            ?: supporter.firstOrNull { it.provider == PROVIDER_STRIPE }?.product

    @Serializable
    data class SubscriptionSource(
        val provider: String? = null,
        val expires: Long? = null,
        val isActive: Boolean = false,
        @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
        val product: String? = null,
    )

    @Serializable
    data class SupporterSource(
        val provider: String? = null,
        val product: String? = null,
        @SerialName("formatted_price") val formattedPrice: String? = null,
        val status: String? = null,
        val expires: Long? = null,
        @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
    )

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
