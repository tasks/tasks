package org.tasks.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.billing.BillingProvider
import org.tasks.billing.StripePurchase
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest
import org.tasks.billing.logStripeRestore
import org.tasks.billing.logStripeRestoreCode
import org.tasks.billing.logStripeRestoreRequest
import org.tasks.preferences.TasksPreferences

class BillingAnalyticsTest {
    private class RecordingAnalytics : Analytics {
        override val tasksPreferences = TasksPreferences(InMemoryDataStore())
        override val analyticsEnabled = true
        val events = mutableListOf<Pair<String, Map<String, Any>>>()

        override fun logEvent(event: String, vararg params: Pair<String, Any>) {
            events.add(event to params.toMap())
        }

        override fun addTask(source: String) = Unit
        override fun completeTask(source: String) = Unit
        override fun setSku(sku: String?) = Unit
    }

    private val analytics = RecordingAnalytics()

    @Test
    fun billingFlowResults() {
        analytics.logBillingFlow(
            BillingProvider.GOOGLE_PLAY,
            AnalyticsEvents.BillingFlow.LAUNCHED,
            sku = "annual_30",
            result = "x".repeat(150),
        )
        analytics.logBillingFlow(BillingProvider.STRIPE, AnalyticsEvents.BillingFlow.PURCHASED, sku = "monthly_03")
        analytics.logBillingFlow(BillingProvider.STRIPE, AnalyticsEvents.BillingFlow.PURCHASED, sku = "vip")

        assertEquals(
            listOf(
                mapOf(
                    AnalyticsEvents.PARAM_PROVIDER to "google_play",
                    AnalyticsEvents.PARAM_STATE to "LAUNCHED",
                    AnalyticsEvents.PARAM_SKU to "annual_30",
                    AnalyticsEvents.PARAM_PERIOD to AnalyticsEvents.PERIOD_ANNUAL,
                    AnalyticsEvents.PARAM_RESULT to "x".repeat(100),
                ),
                mapOf(
                    AnalyticsEvents.PARAM_PROVIDER to "stripe",
                    AnalyticsEvents.PARAM_STATE to "PURCHASED",
                    AnalyticsEvents.PARAM_SKU to "monthly_03",
                    AnalyticsEvents.PARAM_PERIOD to AnalyticsEvents.PERIOD_MONTHLY,
                ),
                mapOf(
                    AnalyticsEvents.PARAM_PROVIDER to "stripe",
                    AnalyticsEvents.PARAM_STATE to "PURCHASED",
                    AnalyticsEvents.PARAM_SKU to "vip",
                ),
            ),
            analytics.events.onEach { assertEquals(AnalyticsEvents.BILLING_FLOW_RESULT, it.first) }.map { it.second },
        )
    }

    @Test
    fun stripeRestoreLogsOutcomesAndRefusalsOnly() {
        analytics.logStripeRestoreRequest(StripeRestoreRequest.Sent)
        analytics.logStripeRestoreRequest(StripeRestoreRequest.RateLimited)
        analytics.logStripeRestoreCode(StripeRestore.Restored(purchase = PURCHASE))
        analytics.logStripeRestoreCode(StripeRestore.NothingToRestore)
        analytics.logStripeRestore(StripeRestoreOutcome.Restored)
        analytics.logStripeRestore(StripeRestoreOutcome.NeedsUpgrade)

        assertEquals(
            listOf(
                AnalyticsEvents.RESTORE_ERROR to AnalyticsEvents.Checkout.TOO_MANY_ATTEMPTS,
                AnalyticsEvents.RESTORE_ERROR to AnalyticsEvents.Checkout.NOTHING_TO_RESTORE,
                AnalyticsEvents.RESTORE_SUCCESS to AnalyticsEvents.Checkout.RESTORED,
                AnalyticsEvents.RESTORE_ERROR to AnalyticsEvents.Checkout.NEEDS_UPGRADE,
            ),
            analytics.events.map { (event, params) ->
                assertEquals(AnalyticsEvents.SELECTION_STRIPE, params[AnalyticsEvents.PARAM_SELECTION])
                event to params[AnalyticsEvents.PARAM_RESULT]
            },
        )
    }

    private companion object {
        val PURCHASE = StripePurchase(purchaseJwt = "jwt", refreshToken = "refresh", sku = "annual_30")
    }
}
