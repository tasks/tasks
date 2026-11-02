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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.compose.settings.MessageDialog
import org.tasks.compose.settings.conflictingAccountMessage
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
import tasks.kmp.generated.resources.retry
import tasks.kmp.generated.resources.upgrade_subscription_banner
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
    onRetry: () -> Unit = {},
) {
    when (notice) {
        null -> Unit
        Notice.ChangeUnavailable -> ActionDialog(
            message = stringResource(Res.string.error_billing_unavailable),
            actionLabel = stringResource(Res.string.retry),
            onAction = onRetry,
            onDismiss = onDismiss,
        )
        Notice.NeedsUpgrade -> Message(Res.string.upgrade_subscription_banner, onDismiss)
        Notice.PurchaseGone -> Message(Res.string.error_purchase_not_verified, onDismiss)
        Notice.SubscriptionCancelled -> Message(Res.string.your_subscription_expired, onDismiss)
        Notice.AccountLinkConflict -> MessageDialog(
            message = conflictingAccountMessage(null),
            onDismiss = onDismiss,
        )
        is Notice.AccountInUse -> MessageDialog(
            message = conflictingAccountMessage(notice.existingAccount),
            onDismiss = onDismiss,
        )
        Notice.Expired -> Message(Res.string.checkout_expired, onDismiss)
        Notice.SubscriptionInvalid -> Message(Res.string.your_subscription_expired, onDismiss)
        Notice.StorageFailed -> Message(Res.string.error_encryption_failed, onDismiss)
        Notice.RateLimited -> Message(Res.string.error_too_many_requests, onDismiss)
        Notice.Unavailable -> Message(Res.string.error_billing_unavailable, onDismiss)
        Notice.Failed -> MessageDialog(
            message = stringResource(Res.string.error_contact_support, TasksUrls.SUPPORT_EMAIL),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun Message(message: StringResource, onDismiss: () -> Unit) =
    MessageDialog(message = stringResource(message), onDismiss = onDismiss)

@Composable
private fun ActionDialog(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
