package org.tasks.compose.pricing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.viewmodel.StripeCheckoutViewModel

@Composable
fun cardCheckout(
    suggestedPrice: Int?,
    openUrl: (String) -> Unit,
): StoreBilling {
    val viewModel = koinViewModel<StripeCheckoutViewModel>()
    val state by viewModel.viewState.collectAsState()
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.checkoutUrl) {
        val url = state.checkoutUrl
        if (url != null && url != opened) {
            opened = url
            openUrl(url)
        }
    }
    CheckoutProgressDialog(
        visible = state.checkoutUrl != null,
        onReopen = { state.checkoutUrl?.let(openUrl) },
        onCancel = { viewModel.cancel() },
    )
    CheckoutNoticeDialog(
        notice = state.notice,
        onDismiss = { viewModel.dismissNotice() },
    )
    return StoreBilling(
        catalog = state.catalog,
        loadingPrices = state.loadingPrices,
        busySku = state.busySku,
        currentSku = state.plan?.sku,
        suggestedPrice = suggestedPrice,
        onSelect = { viewModel.select(it) },
    )
}
