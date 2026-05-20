package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.components.TasksIcon
import org.tasks.compose.pickers.IconPicker
import org.tasks.compose.pickers.IconPickerViewModel
import org.tasks.data.PrincipalWithAccess
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_ACCEPTED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_DECLINED
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_INVALID
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_NO_RESPONSE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_UNKNOWN
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.Task
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.color
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.delete_tag_confirmation
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.icon
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.email
import tasks.kmp.generated.resources.invalid_email_address
import tasks.kmp.generated.resources.invite_awaiting_response
import tasks.kmp.generated.resources.invite_declined
import tasks.kmp.generated.resources.invite_invalid
import tasks.kmp.generated.resources.list_members
import tasks.kmp.generated.resources.logout_warning
import tasks.kmp.generated.resources.new_list
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.remove_user
import tasks.kmp.generated.resources.remove_user_confirmation
import tasks.kmp.generated.resources.save
import tasks.kmp.generated.resources.send
import tasks.kmp.generated.resources.share
import tasks.kmp.generated.resources.share_list
import tasks.kmp.generated.resources.user

data class ListSettingsState(
    val name: String = "",
    val color: Int = 0,
    val icon: String = TasksIcons.LIST,
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val snackbar: String? = null,
    val calendar: CaldavCalendar? = null,
    val account: CaldavAccount? = null,
    val principals: List<PrincipalWithAccess> = emptyList(),
    val confirmRemovePrincipal: PrincipalWithAccess? = null,
    val shareDialogOpen: Boolean = false,
    val shareLoading: Boolean = false,
    val showIconPicker: Boolean = false,
    val showColorPicker: Boolean = false,
    val pickerColors: List<PickerColor> = emptyList(),
    val hasPro: Boolean = false,
    val hasColorWheel: Boolean = false,
    val showDiscardDialog: Boolean = false,
) {
    val isNew: Boolean get() = calendar?.id == null || calendar.id == Task.NO_ID

    val canShare: Boolean
        get() = account?.let { a ->
            a.canShare && (isNew || calendar?.access == ACCESS_OWNER)
        } ?: false

    val canRemovePrincipals: Boolean
        get() = calendar?.access == ACCESS_OWNER && account?.canRemovePrincipal == true

    val hasChanges: Boolean
        get() {
            val cal = calendar ?: return name.isNotBlank() || color != 0 || icon != TasksIcons.LIST
            return name.trim() != (cal.name ?: "") ||
                    color != cal.color ||
                    icon != (cal.icon ?: TasksIcons.LIST)
        }

    val useEmailForSharing: Boolean
        get() = account?.serverType !in listOf(SERVER_OWNCLOUD, SERVER_NEXTCLOUD)
}

private val CaldavAccount.canShare: Boolean
    get() = serverType in listOf(SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD)

private val CaldavAccount.canRemovePrincipal: Boolean
    get() = serverType in listOf(SERVER_TASKS, SERVER_OWNCLOUD, SERVER_SABREDAV, SERVER_NEXTCLOUD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSettingsScreen(
    state: ListSettingsState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardDialogChange: (Boolean) -> Unit,
    onDismissSnackbar: () -> Unit,
    onOpenShareDialog: () -> Unit,
    onCloseShareDialog: () -> Unit,
    onShare: (String) -> Unit,
    onConfirmRemovePrincipal: (PrincipalWithAccess?) -> Unit,
    onRemovePrincipal: (PrincipalWithAccess) -> Unit,
    onOpenColorPicker: () -> Unit,
    onCloseColorPicker: () -> Unit,
    onSelectColor: (PickerColor) -> Unit,
    onColorWheelSelected: () -> Unit,
    onOpenIconPicker: () -> Unit,
    onCloseIconPicker: () -> Unit,
    onSelectIcon: (String) -> Unit,
    onSubscribe: () -> Unit,
    headerContent: @Composable () -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    PlatformBackHandler(enabled = state.hasChanges) {
        onDiscardDialogChange(true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(WindowInsets.systemBars)
            .union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.hasChanges) {
                            onDiscardDialogChange(true)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = {
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
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            headerContent()

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            // Name input
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            ) {
                TextInputCard(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(Res.string.display_name),
                    placeholder = if (state.isNew) stringResource(Res.string.new_list) else null,
                    error = state.nameError,
                )
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            // Color and icon pickers
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                SettingsItemCard(position = CardPosition.First) {
                    PreferenceRow(
                        title = stringResource(Res.string.color),
                        showChevron = true,
                        onClick = onOpenColorPicker,
                        leading = {
                            val bgColor = if (state.color != 0)
                                Color(
                                    state.pickerColors
                                        .firstOrNull { it.originalColor == state.color }
                                        ?.primaryColor
                                        ?: state.color
                                )
                            else
                                MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .padding(start = SettingsContentPadding)
                                    .size(SettingsIconSize)
                                    .clip(CircleShape)
                                    .background(bgColor),
                            )
                        },
                    )
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.icon),
                        showChevron = true,
                        onClick = onOpenIconPicker,
                        leading = {
                            TasksIcon(
                                label = state.icon,
                                modifier = Modifier
                                    .padding(start = SettingsContentPadding)
                                    .size(SettingsIconSize),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            // Share button
            if (state.canShare) {
                val hasName = state.name.isNotBlank()
                Column(
                    modifier = Modifier.padding(horizontal = SettingsContentPadding),
                ) {
                    SettingsItemCard {
                        PreferenceRow(
                            title = if (hasName) {
                                stringResource(Res.string.share_list, state.name)
                            } else {
                                stringResource(Res.string.share)
                            },
                            icon = Icons.Outlined.PersonAdd,
                            enabled = hasName && !state.isLoading,
                            onClick = onOpenShareDialog,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SettingsContentPadding))
            }

            // Principals list
            if (state.principals.isNotEmpty()) {
                PrincipalsList(
                    principals = state.principals,
                    canRemove = state.canRemovePrincipals,
                    onRemove = onConfirmRemovePrincipal,
                )

                Spacer(modifier = Modifier.height(SettingsContentPadding))
            }

            // Save button
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            ) {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(Res.string.save),
                        icon = Icons.Outlined.Save,
                        enabled = state.hasChanges && !state.isLoading,
                        onClick = onSave,
                    )
                }
            }

            // Delete button
            if (!state.isNew) {
                Spacer(modifier = Modifier.height(SettingsContentPadding))

                Column(
                    modifier = Modifier.padding(horizontal = SettingsContentPadding),
                ) {
                    DangerCard(
                        icon = Icons.Outlined.DeleteOutline,
                        title = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDialog = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }
    }

    // Share dialog
    if (state.shareDialogOpen) {
        ShareDialog(
            email = state.useEmailForSharing,
            listName = state.name,
            isLoading = state.shareLoading,
            onDismiss = onCloseShareDialog,
            onShare = onShare,
        )
    }

    // Color picker dialog
    if (state.showColorPicker) {
        ColorPickerDialog(
            hasPro = state.hasPro,
            colors = state.pickerColors,
            onDismiss = onCloseColorPicker,
            onColorSelected = onSelectColor,
            onColorWheelSelected = onColorWheelSelected,
            showColorWheel = state.hasColorWheel,
        )
    }

    // Icon picker dialog
    if (state.showIconPicker) {
        val iconPickerViewModel = remember { IconPickerViewModel() }
        val iconState = iconPickerViewModel.viewState.collectAsState().value
        val searchResults = iconPickerViewModel.searchResults.collectAsState().value
        BasicAlertDialog(
            onDismissRequest = onCloseIconPicker,
            modifier = Modifier.padding(vertical = 32.dp),
        ) {
            IconPicker(
                icons = iconState.icons,
                query = iconState.query,
                searchResults = searchResults,
                collapsed = iconState.collapsed,
                onQueryChange = iconPickerViewModel::onQueryChange,
                onSelected = { onSelectIcon(it.name) },
                toggleCollapsed = iconPickerViewModel::setCollapsed,
                hasPro = state.hasPro,
                subscribe = onSubscribe,
            )
        }
    }

    // Remove principal dialog
    state.confirmRemovePrincipal?.let { principal ->
        AlertDialog(
            onDismissRequest = { onConfirmRemovePrincipal(null) },
            confirmButton = {
                TextButton(onClick = { onRemovePrincipal(principal) }) {
                    Text(text = stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmRemovePrincipal(null) }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
            title = {
                Text(
                    stringResource(Res.string.remove_user),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        Res.string.remove_user_confirmation,
                        principal.name,
                        state.calendar?.name ?: "",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }

    // Delete dialog
    if (showDeleteDialog) {
        val calendarName = state.calendar?.name ?: state.name
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.delete_tag_confirmation, calendarName),
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
                    Text(text = stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }

    // Discard dialog
    if (state.showDiscardDialog) {
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

@Composable
private fun PrincipalsList(
    principals: List<PrincipalWithAccess>,
    canRemove: Boolean,
    onRemove: (PrincipalWithAccess) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
    ) {
        SectionHeader(title = stringResource(Res.string.list_members))

        Column(
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            principals.forEachIndexed { index, principal ->
                SettingsItemCard(
                    position = CardPosition.forIndex(index, principals.size),
                ) {
                    PrincipalRow(
                        principal = principal,
                        canRemove = canRemove,
                        onRemove = { onRemove(principal) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrincipalRow(
    principal: PrincipalWithAccess,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsContentPadding,
                vertical = SettingsRowPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val email = principal.email
            Text(
                text = principal.displayName ?: email ?: principal.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (principal.displayName != null && email != null) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (principal.inviteStatus != INVITE_ACCEPTED) {
                Text(
                    text = stringResource(
                        when (principal.inviteStatus) {
                            INVITE_UNKNOWN, INVITE_NO_RESPONSE ->
                                Res.string.invite_awaiting_response
                            INVITE_DECLINED ->
                                Res.string.invite_declined
                            INVITE_INVALID ->
                                Res.string.invite_invalid
                            else -> throw IllegalStateException()
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (principal.inviteStatus) {
                        INVITE_DECLINED, INVITE_INVALID -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (canRemove) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = onRemove,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShareDialog(
    email: Boolean,
    listName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val trimmed = text.trim()
    val isValid = trimmed.isNotEmpty() && (!email || isValidEmail(trimmed))

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = stringResource(Res.string.share_list, listName),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            TextInputCard(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = stringResource(if (email) Res.string.email else Res.string.user),
                error = if (showError && !isValid) stringResource(Res.string.invalid_email_address) else null,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onShare(trimmed)
                    } else {
                        showError = true
                    }
                },
                enabled = !isLoading,
            ) {
                Text(text = stringResource(Res.string.send))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}

internal expect fun isValidEmail(email: String): Boolean
