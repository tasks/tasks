package org.tasks.billing

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.tasks.PlatformConfiguration
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Firebase
import org.tasks.auth.SignInActivity
import org.tasks.compose.accounts.Platform
import org.tasks.compose.pricing.PricingMode
import org.tasks.compose.pricing.PricingScreen
import org.tasks.compose.pricing.cardCheckout
import org.tasks.compose.pricing.rememberPaywallSatisfied
import org.tasks.compose.settings.RestorePurchasesScreen
import org.tasks.compose.settings.StripeRestoreCallbacks
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_platform_account
import tasks.kmp.generated.resources.caldav
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.gtasks_GPr_header

@Composable
fun Paywall(
    nameYourPrice: Boolean,
    source: String,
    configuration: PlatformConfiguration,
    inventory: Inventory,
    subscriptionProvider: SubscriptionProvider,
    stripePurchases: StripePurchases,
    storeBillingProvider: StoreBillingProvider?,
    gitHubSponsorClient: GitHubSponsorClient?,
    firebase: Firebase,
    completion: PaywallCompletion,
    onBack: () -> Unit,
    onPurchased: () -> Unit,
) {
    val context = LocalContext.current
    var showRestore by rememberSaveable { mutableStateOf(false) }
    val subscription by inventory.subscription.collectAsStateWithLifecycle()
    val hasTasksAccount by inventory.tasksAccount.collectAsStateWithLifecycle()
    val purchaseFinished by rememberUpdatedState(onPurchased)
    var finished by rememberSaveable { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            completion.purchased()
            purchaseFinished()
        }
    }
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            finish()
        } else {
            completion.signInFailed()
            result.data
                ?.getStringExtra(SignInActivity.EXTRA_ERROR)
                ?.let { context.toast(it) }
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
        LaunchedEffect(Unit) {
            firebase.logEvent(AnalyticsEvents.SCREEN_RESTORE_PURCHASES)
        }
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
            onGitHubSignIn = {
                gitHubSponsorClient?.signIn() ?: GitHubSponsorClient.VerifyResult.Failed
            },
            onGitHubSelected = {
                firebase.logEvent(
                    AnalyticsEvents.RESTORE_SELECTION,
                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                )
            },
            onStripeSelected = {
                firebase.logEvent(
                    AnalyticsEvents.RESTORE_SELECTION,
                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_STRIPE,
                )
            },
            onNotSponsor = { firebase.logEvent(AnalyticsEvents.RESTORE_NOT_SPONSOR) },
            onGitHubSuccess = {
                firebase.logEvent(
                    AnalyticsEvents.RESTORE_SUCCESS,
                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                )
            },
            onGitHubFailed = {
                firebase.logEvent(
                    AnalyticsEvents.RESTORE_ERROR,
                    AnalyticsEvents.PARAM_SELECTION to AnalyticsEvents.SELECTION_GITHUB,
                )
            },
            successButtonText = when (source) {
                Platform.CALDAV.name ->
                    stringResource(Res.string.add_platform_account, stringResource(Res.string.caldav))
                Platform.ETEBASE.name ->
                    stringResource(Res.string.add_platform_account, stringResource(Res.string.etesync))
                Platform.GOOGLE_TASKS.name ->
                    stringResource(Res.string.add_platform_account, stringResource(Res.string.gtasks_GPr_header))
                else -> null
            },
        )
        return
    }
    PricingScreen(
        mode = mode,
        onBack = onBack,
        onSignIn = {
            firebase.logEvent(AnalyticsEvents.PRICING_SIGN_IN_CLICK)
            signInLauncher.launch(Intent(context, SignInActivity::class.java))
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
        hasSupporterSubscription = subscription?.isSupporterSubscription == true,
        showSignIn = !hasTasksAccount,
        store = when (configuration.billingProvider) {
            BillingProvider.STRIPE -> cardCheckout(
                viewModel = hiltViewModel<StripeCheckoutHiltViewModel>(),
                suggestedPrice = suggestedPrice,
                openUrl = { context.openUri(it) },
                onRestorePurchases = { showRestore = true },
                onPlanChanged = finish,
                portalUrl = { stripePurchases.portalUrl() },
            )
            BillingProvider.GOOGLE_PLAY -> storeBillingProvider?.storeBilling(suggestedPrice, finish)
            null -> null
        },
    )
}
