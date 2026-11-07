package org.tasks.billing

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.pricing.ActionDialog
import org.tasks.compose.pricing.StoreBilling
import org.tasks.compose.settings.MessageDialog
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_billing_unavailable
import tasks.kmp.generated.resources.retry

private const val TAG = "PlayCheckout"

fun PlayCheckoutViewModel.ViewState.toStoreBilling(
    suggestedPrice: Int?,
    onSelect: (BillingOption) -> Unit,
): StoreBilling = StoreBilling(
    catalog = catalog,
    loadingPrices = loadingPrices,
    busySku = busySku,
    currentSku = currentSku,
    sellsCurrentSku = currentCanceled,
    suggestedPrice = suggestedPrice,
    knownCustomer = false,
    coveredElsewhere = coveredElsewhere,
    coveredBy = coveredBy,
    onSelect = onSelect,
)

@Composable
fun playStoreBilling(
    viewModel: PlayCheckoutViewModel,
    suggestedPrice: Int?,
    onPurchased: () -> Unit,
): StoreBilling {
    val state by viewModel.viewState.collectAsState()
    val activity = LocalActivity.current
    val purchased by rememberUpdatedState(onPurchased)
    LaunchedEffect(state.purchased) {
        if (state.purchased) {
            viewModel.purchaseHandled()
            purchased()
        }
    }
    val onSelect = remember(viewModel, activity) {
        { price: BillingOption ->
            withActivity(activity, price.sku) { viewModel.select(price, it) }
        }
    }
    PlayBillingErrorDialog(
        error = state.error,
        onRetry = { withActivity(activity, state.error?.price?.sku) { viewModel.retry(it) } },
        onDismiss = { viewModel.dismissError() },
    )
    return state.toStoreBilling(suggestedPrice = suggestedPrice, onSelect = onSelect)
}

private fun withActivity(activity: Activity?, sku: String?, block: (Activity) -> Unit) {
    if (activity == null) {
        Logger.e(TAG) { "No activity to launch the purchase flow for $sku" }
    } else {
        block(activity)
    }
}

@Composable
private fun PlayBillingErrorDialog(
    error: PlayCheckoutViewModel.Error?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val unavailable = stringResource(Res.string.error_billing_unavailable)
    when (error) {
        null -> {}
        is PlayCheckoutViewModel.Error.Unavailable -> ActionDialog(
            message = unavailable,
            actionLabel = stringResource(Res.string.retry),
            onAction = onRetry,
            onDismiss = onDismiss,
        )
        is PlayCheckoutViewModel.Error.Failed -> MessageDialog(
            message = error.message ?: unavailable,
            onDismiss = onDismiss,
        )
    }
}
