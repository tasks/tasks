package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.tasks.viewmodel.StripeCheckoutViewModel

@Composable
fun cardCheckout(
    viewModel: StripeCheckoutViewModel,
    suggestedPrice: Int?,
    openUrl: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onPlanChanged: () -> Unit,
    portalUrl: suspend () -> String?,
): StoreBilling {
    val state by viewModel.viewState.collectAsState()
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.checkoutUrl) {
        val url = state.checkoutUrl
        if (url != null && url != opened) {
            opened = url
            openUrl(url)
        }
    }
    LaunchedEffect(state.planChanged) {
        if (state.planChanged) {
            viewModel.planChangeHandled()
            onPlanChanged()
        }
    }
    CheckoutProgressDialog(
        visible = state.checkoutUrl != null,
        onReopen = { state.checkoutUrl?.let(openUrl) },
        onCancel = { viewModel.cancel() },
    )
    val openPortal = cardPortalLauncher(portalUrl = portalUrl, openUrl = openUrl)
    CheckoutNoticeDialog(
        notice = state.notice,
        onDismiss = { viewModel.dismissNotice() },
        onRetry = { viewModel.retryChange() },
        onRestore = {
            viewModel.dismissNotice()
            onRestorePurchases()
        },
        onOpenPortal = {
            viewModel.dismissNotice()
            openPortal()
        },
    )
    ChangePlanConfirmDialog(
        pending = state.confirmChange,
        onConfirm = { viewModel.confirmChange() },
        onDismiss = { viewModel.dismissChange() },
    )
    return StoreBilling(
        catalog = state.catalog,
        loadingPrices = state.loadingPrices,
        busySku = state.busySku,
        currentSku = state.plan?.sku,
        sellsCurrentSku = state.plan?.sellsSameSku == true,
        suggestedPrice = suggestedPrice,
        knownCustomer = state.knownCustomer,
        coveredElsewhere = state.coveredElsewhere,
        coveredBy = state.coveredBy,
        onSelect = { viewModel.select(it) },
    )
}
