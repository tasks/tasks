package org.tasks.caldav

sealed interface StripeSubscriptionResult {
    data object Claimed : StripeSubscriptionResult

    data object InsufficientSubscription : StripeSubscriptionResult

    data class TokenInUse(val existingAccount: String) : StripeSubscriptionResult

    data object NoSubscription : StripeSubscriptionResult

    data object InvalidPurchaseJwt : StripeSubscriptionResult

    data object UnknownPurchase : StripeSubscriptionResult

    data object AlreadyLinked : StripeSubscriptionResult

    data object Unavailable : StripeSubscriptionResult

    data object Failed : StripeSubscriptionResult
}
