package org.tasks.billing

interface StripeClient {
    suspend fun prices(): List<StripePrice>?

    suspend fun createCheckout(priceId: String?): StripeCheckout

    suspend fun pollCheckout(code: String): StripeCheckoutStatus
}

data class StripePurchase(
    val purchaseJwt: String,
    val refreshToken: String,
    val sku: String? = null,
    val formattedPrice: String? = null,
)

sealed interface StripeCheckout {
    data class Started(
        val code: String,
        val url: String,
        val expiresAt: Long,
    ) : StripeCheckout

    data object UnknownPrice : StripeCheckout

    data class SubscriptionExists(val product: String? = null) : StripeCheckout

    data object RateLimited : StripeCheckout

    data object Unavailable : StripeCheckout

    data object Failed : StripeCheckout
}

sealed interface StripeCheckoutStatus {
    data object Pending : StripeCheckoutStatus

    data class Complete(val purchase: StripePurchase) : StripeCheckoutStatus

    data object Gone : StripeCheckoutStatus

    data object SubscriptionInvalid : StripeCheckoutStatus

    data class RateLimited(val retryAfterMillis: Long?) : StripeCheckoutStatus

    data object Unavailable : StripeCheckoutStatus

    data object Failed : StripeCheckoutStatus
}
