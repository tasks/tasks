package org.tasks.compose.settings

import org.tasks.themes.TasksIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.data.entity.CaldavAccount
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.logout
import tasks.kmp.generated.resources.logout_confirmation
import tasks.kmp.generated.resources.logout_warning
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.save
import tasks.kmp.generated.resources.sign_in
import tasks.kmp.generated.resources.todoist_api_token
import tasks.kmp.generated.resources.todoist_api_token_description
import tasks.kmp.generated.resources.todoist_api_token_help

/**
 * Todoist authenticates with a bearer token. A personal API token can be pasted
 * straight from the user's integration settings, which works identically on Android,
 * desktop and iOS and needs no registered OAuth client.
 */
data class TodoistAccountState(
    val token: String = "",
    val displayName: String = "",
    val tokenError: String? = null,
    val isLoading: Boolean = false,
    val snackbar: String? = null,
    val account: CaldavAccount? = null,
) {
    val hasChanges: Boolean
        get() = if (account == null) {
            token.isNotBlank()
        } else {
            token.isNotEmpty() || displayName.trim() != (account.name ?: "")
        }
}

@Composable
fun TodoistAccountScreen(
    state: TodoistAccountState,
    isNewAccount: Boolean,
    accountName: String,
    showDiscardDialog: Boolean,
    onTokenChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardDialogChange: (Boolean) -> Unit,
    onDismissSnackbar: () -> Unit,
    onOpenTokenHelp: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    PlatformBackHandler(enabled = state.hasChanges) {
        onDiscardDialogChange(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (!isNewAccount) {
            state.account?.error?.takeIf { it.isNotBlank() }?.let { error ->
                AccountErrorBanner(
                    error = error,
                    modifier = Modifier.padding(horizontal = SettingsContentPadding),
                )
                Spacer(modifier = Modifier.height(SettingsContentPadding))
            }
        }

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            TextInputCard(
                value = state.token,
                onValueChange = onTokenChange,
                label = stringResource(Res.string.todoist_api_token),
                placeholder = if (!isNewAccount) "••••••••" else null,
                error = state.tokenError,
                contentType = ContentType.Password,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                ),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(SettingsRowPadding))
            Text(
                text = stringResource(Res.string.todoist_api_token_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(Res.string.todoist_api_token_help),
                    icon = TasksIcons.OPEN_IN_NEW,
                    onClick = onOpenTokenHelp,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (!isNewAccount) {
            Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                TextInputCard(
                    value = state.displayName,
                    onValueChange = onNameChange,
                    label = stringResource(Res.string.display_name),
                    placeholder = accountName.ifEmpty { null },
                )
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(
                        if (isNewAccount) Res.string.sign_in else Res.string.save
                    ),
                    icon = if (isNewAccount) TasksIcons.LOGIN else TasksIcons.SAVE,
                    enabled = state.hasChanges && !state.isLoading,
                    onClick = onSave,
                )
            }
        }

        if (!isNewAccount) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                DangerCard(
                    icon = TasksIcons.LOGOUT,
                    title = stringResource(Res.string.logout),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteDialog = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (state.snackbar != null) {
            Snackbar(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                action = {
                    TextButton(onClick = onDismissSnackbar) {
                        Text(text = stringResource(Res.string.ok))
                    }
                },
            ) {
                Text(text = state.snackbar)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.logout_confirmation, accountName),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = { Text(text = stringResource(Res.string.logout_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(text = stringResource(Res.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onDiscardDialogChange(false) },
            title = { Text(text = stringResource(Res.string.discard_changes)) },
            confirmButton = {
                TextButton(onClick = {
                    onDiscardDialogChange(false)
                    onNavigateBack()
                }) {
                    Text(text = stringResource(Res.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDiscardDialogChange(false) }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
}
