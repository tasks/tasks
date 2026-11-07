package org.tasks.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import org.tasks.PlatformConfiguration
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Firebase
import org.tasks.compose.pricing.PricingMode
import org.tasks.compose.pricing.PricingScreen
import org.tasks.compose.pricing.cardCheckout
import org.tasks.compose.pricing.rememberPaywallSatisfied
import org.tasks.compose.settings.RestorePurchasesScreen
import org.tasks.compose.settings.StripeRestoreCallbacks

private const val SPONSOR_URL = "https://github.com/sponsors/abaker"

@Composable
fun Paywall(
    nameYourPrice: Boolean,
    configuration: PlatformConfiguration,
    inventory: Inventory,
    subscriptionProvider: SubscriptionProvider,
    stripePurchases: StripePurchases,
    storeBillingProvider: StoreBillingProvider?,
    firebase: Firebase,
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    onSignIn: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showRestore by rememberSaveable { mutableStateOf(false) }
    val subscription by inventory.subscription.observeAsState()
    val purchaseFinished by rememberUpdatedState(onPurchased)
    var finished by rememberSaveable { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            purchaseFinished()
        }
    }
    val suggestedPrice = subscription?.let { SubscriptionSkus.supporterPrice(it.sku) }
    val mode = if (nameYourPrice) PricingMode.BOTH else PricingMode.CLOUD_ONLY
    val satisfied = rememberPaywallSatisfied(mode, subscriptionProvider.subscription)
    LaunchedEffect(satisfied, showRestore) {
        if (satisfied && !showRestore) {
            finish()
        }
    }
    if (showRestore) {
        RestorePurchasesScreen(
            onBack = { showRestore = false },
            sources = remember(configuration) { restoreSources(configuration) },
            stripe = StripeRestoreCallbacks(
                onRequestCode = { email -> stripePurchases.requestRestoreCode(email) },
                onVerifyCode = { email, code -> stripePurchases.verifyRestoreCode(email, code) },
                onRestored = { purchase, covered, terms ->
                    stripePurchases.restore(
                        purchase = purchase,
                        covered = covered,
                        terms = terms,
                        requireCloud = !nameYourPrice,
                    )
                },
                onUpgrade = { showRestore = false },
            ),
            onGitHubSignIn = { GitHubSponsorClient.VerifyResult.Failed },
            onOpenSponsorPage = {
                firebase.logEvent(AnalyticsEvents.RESTORE_SPONSOR_CLICK)
                uriHandler.openUri(SPONSOR_URL)
            },
        )
        return
    }
    PricingScreen(
        mode = mode,
        onBack = onBack,
        onSignIn = {
            firebase.logEvent(AnalyticsEvents.PRICING_SIGN_IN_CLICK)
            onSignIn()
        },
        onRestorePurchases = { showRestore = true },
        onCloudSubscribeClick = {
            firebase.logEvent(
                AnalyticsEvents.PRICING_SUBSCRIBE_CLICK,
                AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_CLOUD,
            )
        },
        onNypSubscribeClick = {
            firebase.logEvent(
                AnalyticsEvents.PRICING_SUBSCRIBE_CLICK,
                AnalyticsEvents.PARAM_TIER to AnalyticsEvents.TIER_NYP,
            )
        },
        hasSupporterSubscription = inventory.hasSubscription && !inventory.hasTasksSubscription,
        showSignIn = !inventory.hasTasksAccount,
        store = when (configuration.billingProvider) {
            BillingProvider.STRIPE -> cardCheckout(
                viewModel = hiltViewModel<StripeCheckoutHiltViewModel>(),
                suggestedPrice = suggestedPrice,
                openUrl = { uriHandler.openUri(it) },
                onRestorePurchases = { showRestore = true },
                onPlanChanged = finish,
                portalUrl = { stripePurchases.portalUrl() },
            )
            BillingProvider.GOOGLE_PLAY -> storeBillingProvider?.storeBilling(suggestedPrice, finish)
            null -> null
        },
    )
}
