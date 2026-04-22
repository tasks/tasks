package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.accept
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.app_password
import tasks.kmp.generated.resources.app_password_created_at
import tasks.kmp.generated.resources.app_password_delete_confirmation
import tasks.kmp.generated.resources.app_password_enter_description
import tasks.kmp.generated.resources.app_password_last_access
import tasks.kmp.generated.resources.app_password_save
import tasks.kmp.generated.resources.app_passwords
import tasks.kmp.generated.resources.authentication_required
import tasks.kmp.generated.resources.button_subscribe
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.delete_tag_confirmation
import tasks.kmp.generated.resources.email_to_task
import tasks.kmp.generated.resources.email_to_task_address
import tasks.kmp.generated.resources.email_to_task_calendar
import tasks.kmp.generated.resources.generate_new_password
import tasks.kmp.generated.resources.guests
import tasks.kmp.generated.resources.help
import tasks.kmp.generated.resources.insufficient_sponsorship
import tasks.kmp.generated.resources.insufficient_subscription
import tasks.kmp.generated.resources.last_backup_never
import tasks.kmp.generated.resources.local_lists
import tasks.kmp.generated.resources.logout
import tasks.kmp.generated.resources.logout_warning
import tasks.kmp.generated.resources.manage_subscription
import tasks.kmp.generated.resources.list_count
import tasks.kmp.generated.resources.migrate
import tasks.kmp.generated.resources.migrate_count
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.password
import tasks.kmp.generated.resources.regenerate_email_address
import tasks.kmp.generated.resources.regenerate_email_address_confirmation
import tasks.kmp.generated.resources.remove
import tasks.kmp.generated.resources.shared_by
import tasks.kmp.generated.resources.shared_with_me
import tasks.kmp.generated.resources.sign_in_with_github
import tasks.kmp.generated.resources.sign_in_with_google
import tasks.kmp.generated.resources.tos_updated_title
import tasks.kmp.generated.resources.url
import tasks.kmp.generated.resources.user
import tasks.kmp.generated.resources.view_tos
import tasks.kmp.generated.resources.your_subscription_expired
import org.tasks.caldav.TasksAccountResponse
import org.tasks.caldav.TasksAccountResponse.Guest
import org.tasks.compose.components.TasksIcon
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.viewmodel.TasksAccountState
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.themes.TasksIcons

data class CalendarItem(
    val name: String,
    val calendarUri: String?,
)

data class NewPassword(
    val username: String,
    val password: String,
)

data class SharedCalendarDisplay(
    val name: String,
    val icon: String?,
    val color: Int,
    val ownerName: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAccountScreen(
    state: TasksAccountState,
    onSignIn: () -> Unit,
    onSubscribe: () -> Unit,
    onOpenSponsor: () -> Unit,
    onMigrate: () -> Unit,
    onCopyEmail: () -> Unit,
    onRegenerateEmail: () -> Unit,
    onSelectCalendar: (String?) -> Unit,
    onDeletePassword: (Int, String) -> Unit,
    onGeneratePassword: (String) -> Unit,
    onOpenAppPasswordsInfo: () -> Unit,
    onCopyField: (String, String) -> Unit,
    onClearNewPassword: () -> Unit,
    onRefreshPasswords: () -> Unit,
    onOpenHelp: () -> Unit,
    onAddAccount: () -> Unit,
    onModifySubscription: () -> Unit,
    onCancelSubscription: () -> Unit,
    onLogout: () -> Unit,
    onAcceptTos: () -> Unit,
    onViewTos: () -> Unit,
    onDismissTos: () -> Unit,
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showRegenerateDialog by rememberSaveable { mutableStateOf(false) }
    var deletePasswordId by rememberSaveable { mutableStateOf<Int?>(null) }
    var deletePasswordDescription by rememberSaveable { mutableStateOf<String?>(null) }
    var showDescriptionDialog by rememberSaveable { mutableStateOf(false) }
    var showCalendarDialog by rememberSaveable { mutableStateOf(false) }
    var showManageSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Upgrade card for guests
        if (state.isGuest && !state.isTasksSubscription) {
            AccountSettingsCard(
                state = ProCardState.Upgrade,
                onClick = onSubscribe,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Error banner card
        state.account?.takeIf { !it.error.isNullOrBlank() }?.let { account ->
            ErrorBannerCard(
                account = account,
                isGithub = state.isGithub,
                hasSubscription = state.hasSubscription,
                isTasksSubscription = state.isTasksSubscription,
                onSignIn = onSignIn,
                onSubscribe = onSubscribe,
                onOpenSponsor = onOpenSponsor,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Migrate card
        if (state.localListCount > 0) {
            SectionHeader(stringResource(Res.string.migrate), modifier = Modifier.padding(horizontal = SettingsContentPadding))
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                PreferenceRow(
                    title = stringResource(Res.string.local_lists),
                    summary = stringResource(
                        Res.string.migrate_count,
                        pluralStringResource(
                            Res.plurals.list_count,
                            state.localListCount,
                            state.localListCount,
                        ),
                    ),
                    onClick = onMigrate,
                )
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Email-to-task card
        if (state.inboundEmail != null) {
            SectionHeader(stringResource(Res.string.email_to_task), modifier = Modifier.padding(horizontal = SettingsContentPadding))
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                SettingsItemCard(position = CardPosition.First) {
                    PreferenceRow(
                        title = stringResource(Res.string.email_to_task_address),
                        summary = state.inboundEmail,
                        icon = Icons.Outlined.Email,
                        onClick = onCopyEmail,
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = SettingsContentPadding)
                                    .size(SettingsIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (state.inboundCalendarName != null) {
                    SettingsItemCard(position = CardPosition.Middle) {
                        PreferenceRow(
                            title = stringResource(Res.string.email_to_task_calendar),
                            summary = state.inboundCalendarName,
                            icon = Icons.AutoMirrored.Outlined.List,
                            onClick = { showCalendarDialog = true },
                        )
                    }
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.regenerate_email_address),
                        icon = Icons.Outlined.Autorenew,
                        onClick = { showRegenerateDialog = true },
                    )
                }
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Shared with me section (calendars shared by others)
        if (state.sharedWithMe.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.shared_with_me), modifier = Modifier.padding(horizontal = SettingsContentPadding))
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                state.sharedWithMe.forEachIndexed { index, shared ->
                    SettingsItemCard(
                        position = CardPosition.forIndex(index, state.sharedWithMe.size),
                    ) {
                        PreferenceRow(
                            title = shared.name,
                            summary = shared.ownerName?.let { stringResource(Res.string.shared_by, it) },
                            leading = {
                                TasksIcon(
                                    modifier = Modifier
                                        .padding(start = SettingsContentPadding)
                                        .size(SettingsIconSize),
                                    label = shared.icon ?: TasksIcons.LIST,
                                    tint = when (shared.color) {
                                        0 -> MaterialTheme.colorScheme.onSurface
                                        else -> Color(shared.color)
                                    },
                                )
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Guests section (people using this user's guest slots)
        if (state.guests.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = SettingsContentPadding)
                    .height(48.dp),
            ) {
                Text(
                    text = stringResource(Res.string.guests, state.guests.size, state.maxGuests),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                state.guests.forEachIndexed { index, guest ->
                    SettingsItemCard(
                        position = CardPosition.forIndex(index, state.guests.size),
                    ) {
                        PreferenceRow(
                            title = guest.displayName ?: guest.email ?: "",
                            summary = if (guest.displayName != null && guest.email != null)
                                guest.email else null,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // App passwords card
        SectionHeader(stringResource(Res.string.app_passwords), modifier = Modifier.padding(horizontal = SettingsContentPadding), onClick = onOpenAppPasswordsInfo)
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val passwordCount = (state.appPasswords?.size ?: 0)
            val totalPasswordItems = passwordCount + 1 // +1 for generate button
            state.appPasswords?.forEachIndexed { index, pw ->
                val description = pw.description ?: stringResource(Res.string.app_password)
                SettingsItemCard(
                    position = CardPosition.forIndex(index, totalPasswordItems),
                ) {
                    val createdAtText = formatRelativeDay(pw.createdAt)
                    val lastAccessText = formatRelativeDay(pw.lastAccess)
                        ?: stringResource(Res.string.last_backup_never)
                    val summary = stringResource(Res.string.app_password_created_at, createdAtText ?: "") + "\n" +
                            stringResource(Res.string.app_password_last_access, lastAccessText)
                    PreferenceRow(
                        title = description,
                        summary = summary,
                        trailing = {
                            IconButton(onClick = {
                                deletePasswordId = pw.sessionId
                                deletePasswordDescription = description
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(SettingsIconSize),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
            SettingsItemCard(
                position = if (passwordCount == 0) CardPosition.Only else CardPosition.Last,
            ) {
                PreferenceRow(
                    title = stringResource(Res.string.generate_new_password),
                    icon = Icons.Outlined.Add,
                    onClick = { showDescriptionDialog = true },
                )
            }
        }

        // Bottom actions card
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        val showManageRow = state.hasSubscription && !state.isGithub && !state.isGuest
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(
                position = if (showManageRow) CardPosition.First else CardPosition.Only,
            ) {
                PreferenceRow(
                    title = stringResource(Res.string.add_account),
                    icon = Icons.Outlined.Add,
                    onClick = onAddAccount,
                )
            }
            if (showManageRow) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.manage_subscription),
                        onClick = { showManageSheet = true },
                    )
                }
            }
        }

        // Logout card
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        DangerCard(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = stringResource(Res.string.logout),
            tint = MaterialTheme.colorScheme.error,
            onClick = { showLogoutDialog = true },
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    // Dialogs

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            text = { Text(stringResource(Res.string.logout_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(stringResource(Res.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text(stringResource(Res.string.regenerate_email_address)) },
            text = { Text(stringResource(Res.string.regenerate_email_address_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showRegenerateDialog = false
                    onRegenerateEmail()
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (deletePasswordId != null && deletePasswordDescription != null) {
        AlertDialog(
            onDismissRequest = {
                deletePasswordId = null
                deletePasswordDescription = null
            },
            title = { Text(stringResource(Res.string.delete_tag_confirmation, deletePasswordDescription!!)) },
            text = { Text(stringResource(Res.string.app_password_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = deletePasswordId!!
                    val desc = deletePasswordDescription!!
                    deletePasswordId = null
                    deletePasswordDescription = null
                    onDeletePassword(id, desc)
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deletePasswordId = null
                    deletePasswordDescription = null
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (showDescriptionDialog) {
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            title = { Text(stringResource(Res.string.app_password_enter_description)) },
            text = {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDescriptionDialog = false
                    onGeneratePassword(description)
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDescriptionDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.newPassword != null) {
        NewPasswordDialog(
            caldavUrl = state.caldavUrl,
            username = state.newPassword!!.username,
            password = state.newPassword!!.password,
            onCopyField = onCopyField,
            onDismiss = {
                onClearNewPassword()
                onRefreshPasswords()
            },
            onHelp = onOpenHelp,
        )
    }

    if (showCalendarDialog && state.calendars.isNotEmpty()) {
        CalendarSelectionDialog(
            calendars = state.calendars,
            currentUri = state.inboundCalendarUri,
            onSelect = { uri ->
                showCalendarDialog = false
                onSelectCalendar(uri)
            },
            onDismiss = { showCalendarDialog = false },
        )
    }

    if (state.showTosDialog) {
        AlertDialog(
            onDismissRequest = onDismissTos,
            title = { Text(stringResource(Res.string.tos_updated_title)) },
            confirmButton = {
                TextButton(onClick = onAcceptTos) {
                    Text(stringResource(Res.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissTos) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                TextButton(onClick = onViewTos) {
                    Text(stringResource(Res.string.view_tos))
                }
            },
        )
    }

    // Bottom sheet
    if (showManageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManageSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            ManageSubscriptionSheetContent(
                onUpgrade = {},
                onModify = {
                    showManageSheet = false
                    onModifySubscription()
                },
                onCancel = {
                    showManageSheet = false
                    onCancelSubscription()
                },
                showUpgrade = false,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun ErrorBannerCard(
    account: CaldavAccount,
    isGithub: Boolean,
    hasSubscription: Boolean,
    isTasksSubscription: Boolean,
    onSignIn: () -> Unit,
    onSubscribe: () -> Unit,
    onOpenSponsor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val defaultTint = MaterialTheme.colorScheme.onSurfaceVariant

    val title: String?
    val summary: String
    val onClick: () -> Unit

    when {
        account.isPaymentRequired() -> {
            if (isGithub) {
                title = null
                summary = stringResource(Res.string.insufficient_sponsorship)
                onClick = onOpenSponsor
            } else if (!hasSubscription || isTasksSubscription) {
                title = stringResource(Res.string.button_subscribe)
                summary = stringResource(Res.string.your_subscription_expired)
                onClick = onSubscribe
            } else {
                title = stringResource(Res.string.manage_subscription)
                summary = stringResource(Res.string.insufficient_subscription)
                onClick = onSubscribe
            }
        }
        account.isLoggedOut() -> {
            title = stringResource(
                if (isGithub) Res.string.sign_in_with_github else Res.string.sign_in_with_google
            )
            summary = stringResource(Res.string.authentication_required)
            onClick = onSignIn
        }
        else -> {
            title = null
            summary = account.error ?: ""
            onClick = {}
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsCardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = SettingsRowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = SettingsContentPadding)
                    .size(SettingsIconSize),
                tint = errorColor,
            )
            Spacer(modifier = Modifier.width(SettingsContentPadding))
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(SettingsContentPadding))
        }
    }
}


@Composable
private fun formatRelativeDay(timestamp: Long?): String? {
    if (timestamp == null) return null
    val result by produceState<String?>(
        initialValue = null,
        key1 = timestamp,
    ) {
        value = getRelativeDay(
            timestamp,
            DateStyle.FULL,
            lowercase = true,
        )
    }
    return result
}

@Composable
private fun NewPasswordDialog(
    caldavUrl: String,
    username: String?,
    password: String?,
    onCopyField: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onHelp: () -> Unit,
) {
    val urlLabel = stringResource(Res.string.url)
    val userLabel = stringResource(Res.string.user)
    val passwordLabel = stringResource(Res.string.password)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(Res.string.app_password_save)) },
        text = {
            Column {
                CopyableField(
                    label = urlLabel,
                    value = caldavUrl,
                    onCopy = { onCopyField(urlLabel, caldavUrl) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                CopyableField(
                    label = userLabel,
                    value = username ?: "",
                    onCopy = {
                        username?.let { onCopyField(userLabel, it) }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                CopyableField(
                    label = passwordLabel,
                    value = password ?: "",
                    onCopy = {
                        password?.let { onCopyField(passwordLabel, it) }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onHelp) {
                Text(stringResource(Res.string.help))
            }
        },
    )
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}


@Composable
private fun CalendarSelectionDialog(
    calendars: List<CalendarItem>,
    currentUri: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.email_to_task_calendar)) },
        text = {
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                calendars.forEach { calendar ->
                    val selected = calendar.calendarUri == currentUri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(calendar.calendarUri) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = calendar.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
