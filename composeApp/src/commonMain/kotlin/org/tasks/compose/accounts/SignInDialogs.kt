package org.tasks.compose.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Reporting
import org.tasks.compose.settings.conflictingAccountMessage
import org.tasks.compose.settings.signedInAsOtherMessage
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.sign_in
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.subscription_not_found
import tasks.kmp.generated.resources.upgrade
import tasks.kmp.generated.resources.upgrade_subscription_banner
import tasks.kmp.generated.resources.wrong_account

private val blocking = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInErrorDialog(
    signInState: SignInState?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    reporting: Reporting,
    source: String,
    onPaymentRequired: (() -> Unit)?,
) {
    val errorState = signInState as? SignInState.Error ?: return
    LaunchedEffect(errorState) {
        reporting.logEvent(
            AnalyticsEvents.SIGN_IN_ERROR,
            AnalyticsEvents.PARAM_MESSAGE to errorState.report,
            AnalyticsEvents.PARAM_SOURCE to source,
        )
    }
    val offersUpgrade = onPaymentRequired != null && when (errorState) {
        SignInState.Error.PaymentRequired,
        SignInState.Error.UpgradeRequired -> true
        is SignInState.Error.AccountInUse,
        is SignInState.Error.DifferentAccount,
        is SignInState.Error.Failed -> false
    }
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = if (offersUpgrade) blocking else DialogProperties(),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val title = stringResource(Res.string.wrong_account)
                val body = when (errorState) {
                    is SignInState.Error.AccountInUse ->
                        conflictingAccountMessage(errorState.existingAccount)
                    is SignInState.Error.DifferentAccount ->
                        signedInAsOtherMessage(errorState.existingAccount)
                    SignInState.Error.UpgradeRequired ->
                        stringResource(Res.string.upgrade_subscription_banner)
                    SignInState.Error.PaymentRequired -> stringResource(
                        Res.string.subscription_not_found,
                        TasksUrls.SUPPORT_EMAIL,
                    )
                    is SignInState.Error.Failed -> errorState.message
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (body != title) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (offersUpgrade) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                onDismiss()
                                onPaymentRequired?.invoke()
                            },
                        ) {
                            Text(stringResource(Res.string.upgrade))
                        }
                    } else if (errorState is SignInState.Error.DifferentAccount) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.cancel))
                        }
                        TextButton(onClick = onRetry) {
                            Text(stringResource(Res.string.sign_in))
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInProgressDialog(
    signInState: SignInState,
    onCancel: () -> Unit,
) {
    if (signInState != SignInState.Loading && signInState != SignInState.Finishing) return
    BasicAlertDialog(
        onDismissRequest = {},
        properties = blocking,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                if (signInState == SignInState.Loading) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}
