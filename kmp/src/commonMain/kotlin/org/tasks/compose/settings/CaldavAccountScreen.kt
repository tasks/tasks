package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OTHER
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SYNOLOGY_CALENDAR
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.caldav_server_mailbox_org
import tasks.kmp.generated.resources.caldav_server_nextcloud
import tasks.kmp.generated.resources.caldav_server_openxchange
import tasks.kmp.generated.resources.caldav_server_other
import tasks.kmp.generated.resources.caldav_server_owncloud
import tasks.kmp.generated.resources.caldav_server_sabredav
import tasks.kmp.generated.resources.caldav_server_synology_calendar
import tasks.kmp.generated.resources.caldav_server_type
import tasks.kmp.generated.resources.caldav_server_unknown
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.logout
import tasks.kmp.generated.resources.logout_confirmation
import tasks.kmp.generated.resources.logout_warning
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.password
import tasks.kmp.generated.resources.save
import tasks.kmp.generated.resources.sign_in
import tasks.kmp.generated.resources.url
import tasks.kmp.generated.resources.user

data class CaldavAccountState(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val displayName: String = "",
    val serverType: Int = SERVER_UNKNOWN,
    val urlError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val snackbar: String? = null,
    val account: CaldavAccount? = null,
) {
    val hasChanges: Boolean
        get() = if (account == null) {
            url.isNotBlank() || username.isNotBlank() || password.isNotBlank() ||
                    serverType != SERVER_UNKNOWN
        } else {
            url.trim() != (account.url ?: "") ||
                    username.trim() != (account.username ?: "") ||
                    password.isNotEmpty() ||
                    displayName.trim() != (account.name ?: "") ||
                    serverType != account.serverType
        }
}

@Composable
fun CaldavAccountScreen(
    state: CaldavAccountState,
    isNewAccount: Boolean,
    accountName: String,
    showDiscardDialog: Boolean,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onServerTypeChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardDialogChange: (Boolean) -> Unit,
    onDismissSnackbar: () -> Unit,
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

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            TextInputCard(
                value = state.url,
                onValueChange = onUrlChange,
                label = stringResource(Res.string.url),
                placeholder = "https://example.com/dav",
                error = state.urlError,
                position = CardPosition.First,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                ),
            )
            TextInputCard(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(Res.string.user),
                error = state.usernameError,
                position = CardPosition.Middle,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                ),
            )
            TextInputCard(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(Res.string.password),
                placeholder = if (!isNewAccount) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else null,
                error = state.passwordError,
                position = CardPosition.Last,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                ),
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (!isNewAccount) {
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            ) {
                TextInputCard(
                    value = state.displayName,
                    onValueChange = onNameChange,
                    label = stringResource(Res.string.display_name),
                    placeholder = state.username.trim().ifEmpty { null },
                )
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            ServerTypeSelector(
                selected = state.serverType,
                onSelected = onServerTypeChange,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(
                        if (isNewAccount) Res.string.sign_in else Res.string.save
                    ),
                    icon = if (isNewAccount) Icons.Outlined.Login else Icons.Outlined.Save,
                    enabled = state.hasChanges && !state.isLoading,
                    onClick = onSave,
                )
            }
        }

        if (!isNewAccount) {
            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            ) {
                DangerCard(
                    icon = Icons.Outlined.Logout,
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
            text = {
                Text(
                    text = stringResource(Res.string.logout_warning),
                )
            },
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
            title = {
                Text(
                    text = stringResource(Res.string.discard_changes),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
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

val serverTypes = listOf(
    SERVER_MAILBOX_ORG to Res.string.caldav_server_mailbox_org,
    SERVER_NEXTCLOUD to Res.string.caldav_server_nextcloud,
    SERVER_OPEN_XCHANGE to Res.string.caldav_server_openxchange,
    SERVER_OWNCLOUD to Res.string.caldav_server_owncloud,
    SERVER_SABREDAV to Res.string.caldav_server_sabredav,
    SERVER_SYNOLOGY_CALENDAR to Res.string.caldav_server_synology_calendar,
    SERVER_OTHER to Res.string.caldav_server_other,
    SERVER_UNKNOWN to Res.string.caldav_server_unknown,
)

@Composable
internal fun ServerTypeSelector(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = serverTypes
        .firstOrNull { it.first == selected }
        ?.second
        ?: Res.string.caldav_server_unknown

    SettingsItemCard {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(
                    horizontal = SettingsContentPadding,
                    vertical = SettingsRowPadding,
                ),
        ) {
            Text(
                text = stringResource(Res.string.caldav_server_type).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(selectedLabel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                serverTypes.forEach { (value, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            expanded = false
                            onSelected(value)
                        },
                    )
                }
            }
        }
    }
}
