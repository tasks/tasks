package org.tasks.compose.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.compose.settings.MessageDialog
import org.tasks.viewmodel.StripeCheckoutViewModel.Notice
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.action_open
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.checkout_expired
import tasks.kmp.generated.resources.checkout_finish_in_browser
import tasks.kmp.generated.resources.error_billing_unavailable
import tasks.kmp.generated.resources.error_contact_support
import tasks.kmp.generated.resources.error_encryption_failed
import tasks.kmp.generated.resources.error_purchase_not_verified
import tasks.kmp.generated.resources.error_too_many_requests
import tasks.kmp.generated.resources.upgrade_subscription_banner
import tasks.kmp.generated.resources.wrong_account_message
import tasks.kmp.generated.resources.your_subscription_expired

@Composable
fun CheckoutProgressDialog(
    visible: Boolean,
    onReopen: () -> Unit,
    onCancel: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.checkout_finish_in_browser)) },
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onReopen) {
                Text(stringResource(Res.string.action_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
fun CheckoutNoticeDialog(
    notice: Notice?,
    onDismiss: () -> Unit,
) {
    if (notice == null) return
    val message = when (notice) {
        Notice.NeedsUpgrade -> stringResource(Res.string.upgrade_subscription_banner)
        Notice.PurchaseGone -> stringResource(Res.string.error_purchase_not_verified)
        Notice.SubscriptionCancelled -> stringResource(Res.string.your_subscription_expired)
        is Notice.AccountInUse -> stringResource(
            Res.string.wrong_account_message,
            notice.existingAccount,
            TasksUrls.SUPPORT_EMAIL,
        )
        Notice.Expired -> stringResource(Res.string.checkout_expired)
        Notice.SubscriptionInvalid -> stringResource(Res.string.your_subscription_expired)
        Notice.StorageFailed -> stringResource(Res.string.error_encryption_failed)
        Notice.RateLimited -> stringResource(Res.string.error_too_many_requests)
        Notice.Unavailable -> stringResource(Res.string.error_billing_unavailable)
        Notice.Failed -> stringResource(Res.string.error_contact_support, TasksUrls.SUPPORT_EMAIL)
    }
    MessageDialog(message = message, onDismiss = onDismiss)
}
