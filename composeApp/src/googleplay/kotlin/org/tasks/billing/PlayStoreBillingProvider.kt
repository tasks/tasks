package org.tasks.billing

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.pricing.StoreBilling
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.dismiss
import tasks.kmp.generated.resources.error_billing_unavailable
import tasks.kmp.generated.resources.retry

class PlayStoreBillingProvider : StoreBillingProvider {
    @Composable
    override fun storeBilling(suggestedPrice: Int?): StoreBilling {
        val viewModel = koinViewModel<PlayCheckoutViewModel>()
        val state by viewModel.viewState.collectAsState()
        val activity = LocalActivity.current
        PlayBillingErrorDialog(
            error = state.error,
            onRetry = { viewModel.retry() },
            onDismiss = { viewModel.dismissError() },
        )
        return state.toStoreBilling(suggestedPrice = suggestedPrice) { price ->
            activity?.let { viewModel.select(price, it) }
        }
    }
}

@Composable
private fun PlayBillingErrorDialog(
    error: PlayCheckoutViewModel.Error?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (error == null) {
        return
    }
    val unavailable = stringResource(Res.string.error_billing_unavailable)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                when (error) {
                    PlayCheckoutViewModel.Error.Unavailable -> unavailable
                    is PlayCheckoutViewModel.Error.Failed -> error.message ?: unavailable
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dismiss))
            }
        },
    )
}
