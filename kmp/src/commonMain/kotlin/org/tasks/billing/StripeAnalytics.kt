package org.tasks.billing

import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents

val StripeRestoreOutcome.analyticsResult: String
    get() = when (this) {
        StripeRestoreOutcome.Restored -> AnalyticsEvents.Checkout.RESTORED
        StripeRestoreOutcome.Ended -> AnalyticsEvents.Checkout.ENDED
        StripeRestoreOutcome.NeedsUpgrade -> AnalyticsEvents.Checkout.NEEDS_UPGRADE
        is StripeRestoreOutcome.AccountInUse -> AnalyticsEvents.Checkout.ACCOUNT_IN_USE
        is StripeRestoreOutcome.StorageFailed ->
            if (encryptionFailed) {
                AnalyticsEvents.Checkout.ENCRYPTION_FAILED
            } else {
                AnalyticsEvents.Checkout.STORAGE_FAILED
            }
    }

private val StripeRestore.analyticsResult: String?
    get() = when (this) {
        is StripeRestore.Restored -> null
        StripeRestore.NothingToRestore -> AnalyticsEvents.Checkout.NOTHING_TO_RESTORE
        StripeRestore.InvalidCode -> AnalyticsEvents.Checkout.INVALID_CODE
        StripeRestore.InvalidEmail -> AnalyticsEvents.Checkout.INVALID_EMAIL
        StripeRestore.TooManyAttempts -> AnalyticsEvents.Checkout.TOO_MANY_ATTEMPTS
        StripeRestore.SubscriptionInvalid -> AnalyticsEvents.Checkout.SUBSCRIPTION_INVALID
        StripeRestore.Unavailable -> AnalyticsEvents.Checkout.UNAVAILABLE
        StripeRestore.Failed -> AnalyticsEvents.Checkout.FAILED
    }

private val StripeRestoreRequest.analyticsResult: String?
    get() = when (this) {
        StripeRestoreRequest.Sent -> null
        StripeRestoreRequest.InvalidEmail -> AnalyticsEvents.Checkout.INVALID_EMAIL
        StripeRestoreRequest.RateLimited -> AnalyticsEvents.Checkout.TOO_MANY_ATTEMPTS
        StripeRestoreRequest.Unavailable -> AnalyticsEvents.Checkout.UNAVAILABLE
        StripeRestoreRequest.Failed -> AnalyticsEvents.Checkout.FAILED
    }

private fun Analytics.logStripeRestoreError(result: String) = logEvent(
    AnalyticsEvents.RESTORE_ERROR,
    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_STRIPE,
    AnalyticsEvents.PARAM_RESULT to result,
)

fun Analytics.logStripeRestoreRequest(request: StripeRestoreRequest) {
    request.analyticsResult?.let { logStripeRestoreError(it) }
}

fun Analytics.logStripeRestoreCode(restore: StripeRestore) {
    restore.analyticsResult?.let { logStripeRestoreError(it) }
}

fun Analytics.logStripeRestore(outcome: StripeRestoreOutcome) = logEvent(
    if (outcome == StripeRestoreOutcome.Restored) {
        AnalyticsEvents.RESTORE_SUCCESS
    } else {
        AnalyticsEvents.RESTORE_ERROR
    },
    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_STRIPE,
    AnalyticsEvents.PARAM_RESULT to outcome.analyticsResult,
)

