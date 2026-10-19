package org.tasks.billing

interface StripeClient {
    suspend fun prices(): List<StripePrice>?

    suspend fun createCheckout(priceId: String?): StripeCheckout

    suspend fun pollCheckout(code: String): StripeCheckoutStatus

    suspend fun changePlan(priceId: String, purchaseJwt: String? = null): StripeChange

    suspend fun requestRestoreCode(email: String): StripeRestoreRequest

    suspend fun verifyRestoreCode(email: String, code: String): StripeRestore

    suspend fun portalUrl(purchaseJwt: String? = null): String?
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

sealed interface StripeChange {
    data class Changed(val entitles: Boolean) : StripeChange

    data object UnknownPrice : StripeChange

    data object NotLinked : StripeChange

    data object NoProofOfPurchase : StripeChange

    data object PurchaseGone : StripeChange

    data object Cancelled : StripeChange

    data object RateLimited : StripeChange

    data object Unavailable : StripeChange

    data object Failed : StripeChange
}

sealed interface StripeRestoreRequest {
    data object Sent : StripeRestoreRequest

    data object InvalidEmail : StripeRestoreRequest

    data object RateLimited : StripeRestoreRequest

    data object Unavailable : StripeRestoreRequest

    data object Failed : StripeRestoreRequest
}

sealed interface StripeRestore {
    data class Restored(val purchase: StripePurchase) : StripeRestore

    data object InvalidCode : StripeRestore

    data object InvalidEmail : StripeRestore

    data object TooManyAttempts : StripeRestore

    data object SubscriptionInvalid : StripeRestore

    data object Unavailable : StripeRestore

    data object Failed : StripeRestore
}
