package org.tasks.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.tasks.billing.BillingProvider
import org.tasks.compose.pricing.PricingMode

class PaywallAnalytics(
    private val analytics: Analytics,
    private val source: String,
    private val mode: PricingMode,
    private val provider: BillingProvider?,
) : ViewModel() {
    private var completed = false

    fun shown() = analytics.logEvent(
        AnalyticsEvents.SHOWED_PURCHASE_DIALOG,
        *purchaseDialogParams(source, mode, provider),
    )

    fun completed() {
        completed = true
    }

    override fun onCleared() {
        if (!completed) {
            analytics.logEvent(
                AnalyticsEvents.PRICING_CLOSE,
                *purchaseDialogParams(source, mode, provider),
            )
        }
    }

    fun signInClicked() = analytics.logEvent(
        AnalyticsEvents.PRICING_SIGN_IN_CLICK,
        *purchaseDialogParams(AnalyticsEvents.SOURCE_PAYWALL, mode, provider),
    )

    fun subscribeClicked(cloud: Boolean) = analytics.logEvent(
        AnalyticsEvents.PRICING_SUBSCRIBE_CLICK,
        AnalyticsEvents.PARAM_TIER to if (cloud) AnalyticsEvents.TIER_CLOUD else AnalyticsEvents.TIER_NYP,
    )

    fun billingToggled(isAnnual: Boolean) = analytics.logEvent(
        AnalyticsEvents.PRICING_BILLING_TOGGLE,
        AnalyticsEvents.PARAM_PERIOD to
            if (isAnnual) AnalyticsEvents.PERIOD_ANNUAL else AnalyticsEvents.PERIOD_MONTHLY,
    )

    fun nameYourPriceSelected(selection: String, price: Int?) = analytics.logEvent(
        AnalyticsEvents.PRICING_NYP_SELECT,
        *listOfNotNull(
            AnalyticsEvents.PARAM_SELECTION to selection,
            price?.let { AnalyticsEvents.PARAM_PRICE to it },
        ).toTypedArray(),
    )
}

private fun purchaseDialogParams(
    source: String,
    mode: PricingMode,
    provider: BillingProvider?,
): Array<Pair<String, Any>> = listOfNotNull(
    AnalyticsEvents.PARAM_SOURCE to source,
    AnalyticsEvents.PARAM_TYPE to mode.name,
    provider?.let { AnalyticsEvents.PARAM_PROVIDER to it.analyticsName },
).toTypedArray()

@Composable
fun rememberPaywallAnalytics(
    analytics: Analytics,
    source: String,
    mode: PricingMode,
    provider: BillingProvider?,
): PaywallAnalytics {
    val paywall = viewModel { PaywallAnalytics(analytics, source, mode, provider) }
    LogOnce { paywall.shown() }
    return paywall
}

@Composable
fun LogOnce(log: () -> Unit) {
    var logged by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!logged) {
            logged = true
            log()
        }
    }
}
