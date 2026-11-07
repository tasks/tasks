package org.tasks.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import org.tasks.billing.PurchaseTerms
import org.tasks.billing.StripePurchase
import org.tasks.billing.StripeRestore
import org.tasks.billing.StripeRestoreOutcome
import org.tasks.billing.StripeRestoreRequest
import org.tasks.billing.formatCredit
import org.tasks.kmp.org.tasks.compose.ShowKeyboard
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.email
import tasks.kmp.generated.resources.error_billing_unavailable
import tasks.kmp.generated.resources.error_contact_support
import tasks.kmp.generated.resources.error_encryption_failed
import tasks.kmp.generated.resources.error_invalid_code
import tasks.kmp.generated.resources.error_no_purchase_found
import tasks.kmp.generated.resources.error_too_many_requests
import tasks.kmp.generated.resources.invalid_email_address
import tasks.kmp.generated.resources.pro_card_credit
import tasks.kmp.generated.resources.purchases_restored
import tasks.kmp.generated.resources.restore_code
import tasks.kmp.generated.resources.restore_code_sent
import tasks.kmp.generated.resources.restore_email_heading
import tasks.kmp.generated.resources.restore_purchases
import tasks.kmp.generated.resources.restore_renewal_settling
import tasks.kmp.generated.resources.restore_send_code
import tasks.kmp.generated.resources.restore_use_another_email
import tasks.kmp.generated.resources.upgrade
import tasks.kmp.generated.resources.upgrade_subscription_banner
import tasks.kmp.generated.resources.your_subscription_expired

data class StripeRestoreCallbacks(
    val onRequestCode: suspend (email: String) -> StripeRestoreRequest,
    val onVerifyCode: suspend (email: String, code: String) -> StripeRestore,
    val onRestored: suspend (
        purchase: StripePurchase,
        covered: Boolean,
        terms: PurchaseTerms?,
    ) -> StripeRestoreOutcome,
    val onUpgrade: () -> Unit,
)

@Serializable
internal enum class Refusal {
    INVALID_EMAIL,
    INVALID_CODE,
    NO_PURCHASE_FOUND,
    TOO_MANY_REQUESTS,
    SUBSCRIPTION_EXPIRED,
    UNAVAILABLE,
    SERVER_FAILED,
    ENCRYPTION_FAILED,
}

@Serializable
internal sealed interface StripeTabState {
    @Serializable
    data class Email(
        val error: Refusal? = null,
        val busy: Boolean = false,
    ) : StripeTabState

    @Serializable
    data class Code(
        val email: String,
        val error: Refusal? = null,
        val busy: Boolean = false,
    ) : StripeTabState

    @Serializable
    data class Restored(val settling: Boolean = false) : StripeTabState

    @Serializable
    data object Upgrade : StripeTabState

    @Serializable
    data class AccountInUse(val existingAccount: String) : StripeTabState

    @Serializable
    data class Ended(val terms: PurchaseTerms? = null) : StripeTabState

    @Serializable
    data class Failed(val refusal: Refusal, val startOver: Boolean) : StripeTabState
}

@Composable
private fun Refusal.message(): String = when (this) {
    Refusal.INVALID_EMAIL -> stringResource(Res.string.invalid_email_address)
    Refusal.INVALID_CODE -> stringResource(Res.string.error_invalid_code)
    Refusal.NO_PURCHASE_FOUND -> stringResource(Res.string.error_no_purchase_found)
    Refusal.TOO_MANY_REQUESTS -> stringResource(Res.string.error_too_many_requests)
    Refusal.SUBSCRIPTION_EXPIRED -> stringResource(Res.string.your_subscription_expired)
    Refusal.UNAVAILABLE -> stringResource(Res.string.error_billing_unavailable)
    Refusal.SERVER_FAILED ->
        stringResource(Res.string.error_contact_support, TasksUrls.SUPPORT_EMAIL)
    Refusal.ENCRYPTION_FAILED -> stringResource(Res.string.error_encryption_failed)
}

@Composable
private fun StripeTabState.Ended.creditNote(): String? =
    formatCredit(terms?.creditCents, terms?.creditCurrency)
        ?.let { stringResource(Res.string.pro_card_credit, it) }

internal val StripeTabStateSaver = jsonSaver<StripeTabState>(
    tag = "StripeTab",
    what = "restore step",
) {
    when (it) {
        null -> StripeTabState.Email()
        is StripeTabState.Email -> it.copy(busy = false)
        is StripeTabState.Code -> it.copy(busy = false)
        else -> it
    }
}

private object DigitsOnly : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        for (i in length - 1 downTo 0) {
            if (!charAt(i).isDigit()) {
                delete(i, i + 1)
            }
        }
    }
}

@Composable
internal fun StripeTab(
    callbacks: StripeRestoreCallbacks,
    onBack: () -> Unit,
    successButtonText: String? = null,
) {
    val email = rememberTextFieldState()
    val code = rememberTextFieldState()
    var state: StripeTabState by rememberSaveable(
        stateSaver = StripeTabStateSaver,
    ) { mutableStateOf(StripeTabState.Email()) }
    val scope = rememberCoroutineScope()

    fun sendCode(address: String, resend: Boolean = false) {
        val trimmed = address.trim()
        if (!isValidEmail(trimmed)) {
            state = StripeTabState.Email(Refusal.INVALID_EMAIL)
            return
        }
        fun refused(error: Refusal) = if (resend) {
            StripeTabState.Code(trimmed, error)
        } else {
            StripeTabState.Email(error)
        }
        state = if (resend) {
            StripeTabState.Code(trimmed, busy = true)
        } else {
            StripeTabState.Email(busy = true)
        }
        scope.launch {
            state = when (callbacks.onRequestCode(trimmed)) {
                StripeRestoreRequest.Sent -> {
                    code.clearText()
                    StripeTabState.Code(trimmed)
                }
                StripeRestoreRequest.InvalidEmail -> StripeTabState.Email(Refusal.INVALID_EMAIL)
                StripeRestoreRequest.RateLimited -> refused(Refusal.TOO_MANY_REQUESTS)
                StripeRestoreRequest.Unavailable -> refused(Refusal.UNAVAILABLE)
                StripeRestoreRequest.Failed -> refused(Refusal.SERVER_FAILED)
            }
        }
    }

    fun verify(address: String, entered: String) {
        state = StripeTabState.Code(address, busy = true)
        scope.launch {
            state = withContext(NonCancellable) { callbacks.verifyAndKeep(address, entered) }
        }
    }

    when (val current = state) {
        is StripeTabState.Email -> EmailStep(
            email = email,
            error = current.error?.message(),
            busy = current.busy,
            onSend = { sendCode(email.text.toString()) },
        )
        is StripeTabState.Code -> CodeStep(
            email = current.email,
            code = code,
            error = current.error?.message(),
            busy = current.busy,
            onVerify = { verify(current.email, code.text.toString()) },
            onResend = { sendCode(current.email, resend = true) },
            onChangeEmail = { state = StripeTabState.Email() },
        )
        is StripeTabState.Restored -> SuccessContent(
            onBack = onBack,
            message = if (current.settling) {
                stringResource(Res.string.restore_renewal_settling)
            } else {
                stringResource(Res.string.purchases_restored)
            },
            buttonText = successButtonText,
        )
        StripeTabState.Upgrade -> MessageStep(
            message = stringResource(Res.string.upgrade_subscription_banner),
            actionLabel = stringResource(Res.string.upgrade),
            onAction = callbacks.onUpgrade,
        )
        is StripeTabState.AccountInUse -> MessageStep(
            message = conflictingAccountMessage(current.existingAccount),
        )
        is StripeTabState.Ended -> MessageStep(
            message = stringResource(Res.string.your_subscription_expired),
            note = current.creditNote(),
            actionLabel = stringResource(Res.string.restore_use_another_email),
            onAction = { state = StripeTabState.Email() },
        )
        is StripeTabState.Failed -> MessageStep(
            message = current.refusal.message(),
            actionLabel = if (current.startOver) {
                stringResource(Res.string.restore_use_another_email)
            } else {
                null
            },
            onAction = { state = StripeTabState.Email() },
        )
    }
}

private suspend fun StripeRestoreCallbacks.verifyAndKeep(
    address: String,
    entered: String,
): StripeTabState = when (val result = onVerifyCode(address, entered)) {
    is StripeRestore.Restored -> when (
        val stored = onRestored(result.purchase, result.covered, result.terms)
    ) {
        StripeRestoreOutcome.Restored -> StripeTabState.Restored(settling = !result.isActive)
        StripeRestoreOutcome.Ended -> StripeTabState.Ended(result.terms)
        StripeRestoreOutcome.NeedsUpgrade -> StripeTabState.Upgrade
        is StripeRestoreOutcome.AccountInUse -> StripeTabState.AccountInUse(stored.existingAccount)
        is StripeRestoreOutcome.StorageFailed -> StripeTabState.Failed(
            refusal = if (stored.encryptionFailed) {
                Refusal.ENCRYPTION_FAILED
            } else {
                Refusal.SERVER_FAILED
            },
            startOver = false,
        )
    }
    StripeRestore.InvalidCode -> StripeTabState.Code(address, Refusal.INVALID_CODE)
    StripeRestore.NothingToRestore -> StripeTabState.Email(Refusal.NO_PURCHASE_FOUND)
    StripeRestore.InvalidEmail -> StripeTabState.Email(Refusal.INVALID_EMAIL)
    StripeRestore.TooManyAttempts ->
        StripeTabState.Failed(Refusal.TOO_MANY_REQUESTS, startOver = true)
    StripeRestore.SubscriptionInvalid ->
        StripeTabState.Failed(Refusal.SUBSCRIPTION_EXPIRED, startOver = true)
    StripeRestore.Unavailable -> StripeTabState.Failed(Refusal.UNAVAILABLE, startOver = true)
    StripeRestore.Failed -> StripeTabState.Failed(Refusal.SERVER_FAILED, startOver = true)
}

@Composable
private fun EmailStep(
    email: TextFieldState,
    error: String?,
    busy: Boolean,
    onSend: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    ShowKeyboard(visible = true, focusRequester = focusRequester)
    Column(
        modifier = Modifier.fillMaxWidth().padding(SettingsContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.restore_email_heading),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            state = email,
            label = { Text(stringResource(Res.string.email)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Go,
            ),
            onKeyboardAction = { if (email.text.isNotBlank()) onSend() },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSend,
            enabled = !busy && email.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                ButtonProgress()
            } else {
                Text(stringResource(Res.string.restore_send_code))
            }
        }
    }
}

@Composable
private fun CodeStep(
    email: String,
    code: TextFieldState,
    error: String?,
    busy: Boolean,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    ShowKeyboard(visible = true, focusRequester = focusRequester)
    Column(
        modifier = Modifier.fillMaxWidth().padding(SettingsContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.restore_code_sent, email),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            state = code,
            label = { Text(stringResource(Res.string.restore_code)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = DigitsOnly,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Go,
            ),
            onKeyboardAction = { if (code.text.isNotBlank()) onVerify() },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            enabled = !busy && code.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                ButtonProgress()
            } else {
                Text(stringResource(Res.string.restore_purchases))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onChangeEmail, enabled = !busy) {
                Text(stringResource(Res.string.restore_use_another_email))
            }
            TextButton(onClick = onResend, enabled = !busy) {
                Text(stringResource(Res.string.restore_send_code))
            }
        }
    }
}

@Composable
private fun ButtonProgress() = CircularProgressIndicator(
    modifier = Modifier.size(16.dp),
    strokeWidth = 2.dp,
    color = LocalContentColor.current,
)

@Composable
private fun MessageStep(
    message: String,
    note: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(SettingsContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (note != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
