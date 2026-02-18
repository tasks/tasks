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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.caldav.TasksAccountResponse
import org.tasks.preferences.fragments.TasksAccountViewModel.NewPassword

data class CalendarItem(
    val name: String,
    val calendarUri: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksAccountScreen(
    account: CaldavAccount?,
    isGithub: Boolean,
    hasSubscription: Boolean,
    isTasksSubscription: Boolean,
    localListCount: Int,
    localListSummary: String,
    inboundEmail: String?,
    inboundCalendarName: String?,
    appPasswords: List<TasksAccountResponse.AppPassword>?,
    newPassword: NewPassword?,
    calendars: List<CalendarItem>,
    inboundCalendarUri: String?,
    showTosDialog: Boolean,
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
    onCopyField: (Int, String) -> Unit,
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

        // Error banner card
        if (account != null && !account.error.isNullOrBlank()) {
            ErrorBannerCard(
                account = account,
                isGithub = isGithub,
                hasSubscription = hasSubscription,
                isTasksSubscription = isTasksSubscription,
                onSignIn = onSignIn,
                onSubscribe = onSubscribe,
                onOpenSponsor = onOpenSponsor,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Migrate card
        if (localListCount > 0) {
            SectionHeader(R.string.migrate, modifier = Modifier.padding(horizontal = SettingsContentPadding))
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                PreferenceRow(
                    title = stringResource(R.string.local_lists),
                    summary = localListSummary,
                    onClick = onMigrate,
                )
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // Email-to-task card
        if (inboundEmail != null) {
            SectionHeader(R.string.email_to_task, modifier = Modifier.padding(horizontal = SettingsContentPadding))
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                SettingsItemCard(position = CardPosition.First) {
                    PreferenceRow(
                        title = stringResource(R.string.email_to_task_address),
                        summary = inboundEmail,
                        icon = Icons.Outlined.Email,
                        onClick = onCopyEmail,
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = SettingsContentPadding)
                                    .size(SettingsIconSize),
                                tint = colorResource(R.color.icon_tint_with_alpha),
                            )
                        },
                    )
                }
                if (inboundCalendarName != null) {
                    SettingsItemCard(position = CardPosition.Middle) {
                        PreferenceRow(
                            title = stringResource(R.string.email_to_task_calendar),
                            summary = inboundCalendarName,
                            icon = Icons.AutoMirrored.Outlined.List,
                            onClick = { showCalendarDialog = true },
                        )
                    }
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.regenerate_email_address),
                        icon = Icons.Outlined.Autorenew,
                        onClick = { showRegenerateDialog = true },
                    )
                }
            }
            Spacer(modifier = Modifier.height(SettingsSectionGap))
        }

        // App passwords card
        SectionHeader(R.string.app_passwords, modifier = Modifier.padding(horizontal = SettingsContentPadding), onClick = onOpenAppPasswordsInfo)
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val passwordCount = (appPasswords?.size ?: 0)
            val totalPasswordItems = passwordCount + 1 // +1 for generate button
            appPasswords?.forEachIndexed { index, pw ->
                val description = pw.description ?: stringResource(R.string.app_password)
                SettingsItemCard(
                    position = cardPosition(index, totalPasswordItems),
                ) {
                    val createdAtText = formatRelativeDay(pw.createdAt)
                    val lastAccessText = formatRelativeDay(pw.lastAccess)
                        ?: stringResource(R.string.last_backup_never)
                    val summary = stringResource(R.string.app_password_created_at, createdAtText ?: "") + "\n" +
                            stringResource(R.string.app_password_last_access, lastAccessText)
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
                                    tint = colorResource(R.color.icon_tint_with_alpha),
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
                    title = stringResource(R.string.generate_new_password),
                    icon = Icons.Outlined.Add,
                    onClick = { showDescriptionDialog = true },
                )
            }
        }

        // Bottom actions card
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        val showManageRow = hasSubscription && !isGithub
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(
                position = if (showManageRow) CardPosition.First else CardPosition.Only,
            ) {
                PreferenceRow(
                    title = stringResource(R.string.add_account),
                    icon = Icons.Outlined.Add,
                    onClick = onAddAccount,
                )
            }
            if (showManageRow) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.manage_subscription),
                        onClick = { showManageSheet = true },
                    )
                }
            }
        }

        // Logout card
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        DangerCard(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = stringResource(R.string.logout),
            tint = colorResource(R.color.overdue),
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
            text = { Text(stringResource(R.string.logout_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text(stringResource(R.string.regenerate_email_address)) },
            text = { Text(stringResource(R.string.regenerate_email_address_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showRegenerateDialog = false
                    onRegenerateEmail()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.delete_tag_confirmation, deletePasswordDescription!!)) },
            text = { Text(stringResource(R.string.app_password_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = deletePasswordId!!
                    val desc = deletePasswordDescription!!
                    deletePasswordId = null
                    deletePasswordDescription = null
                    onDeletePassword(id, desc)
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deletePasswordId = null
                    deletePasswordDescription = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDescriptionDialog) {
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            title = { Text(stringResource(R.string.app_password_enter_description)) },
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
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDescriptionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (newPassword != null) {
        NewPasswordDialog(
            caldavUrl = stringResource(R.string.tasks_caldav_url),
            username = newPassword.username,
            password = newPassword.password,
            onCopyField = onCopyField,
            onDismiss = {
                onClearNewPassword()
                onRefreshPasswords()
            },
            onHelp = onOpenHelp,
        )
    }

    if (showCalendarDialog && calendars.isNotEmpty()) {
        CalendarSelectionDialog(
            calendars = calendars,
            currentUri = inboundCalendarUri,
            onSelect = { uri ->
                showCalendarDialog = false
                onSelectCalendar(uri)
            },
            onDismiss = { showCalendarDialog = false },
        )
    }

    if (showTosDialog) {
        AlertDialog(
            onDismissRequest = onDismissTos,
            title = { Text(stringResource(R.string.tos_updated_title)) },
            confirmButton = {
                TextButton(onClick = onAcceptTos) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissTos) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TextButton(onClick = onViewTos) {
                    Text(stringResource(R.string.view_tos))
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
    val errorColor = colorResource(R.color.overdue)
    val defaultTint = colorResource(R.color.icon_tint_with_alpha)

    val title: String?
    val summary: String
    val onClick: () -> Unit

    when {
        account.isPaymentRequired() -> {
            if (isGithub) {
                title = null
                summary = stringResource(R.string.insufficient_sponsorship)
                @Suppress("KotlinConstantConditions")
                onClick = if (BuildConfig.FLAVOR == "googleplay") {
                    {}
                } else {
                    onOpenSponsor
                }
            } else if (!hasSubscription || isTasksSubscription) {
                title = stringResource(R.string.button_subscribe)
                summary = stringResource(R.string.your_subscription_expired)
                onClick = onSubscribe
            } else {
                title = stringResource(R.string.manage_subscription)
                summary = stringResource(R.string.insufficient_subscription)
                onClick = onSubscribe
            }
        }
        account.isLoggedOut() -> {
            title = stringResource(
                if (isGithub) R.string.sign_in_with_github else R.string.sign_in_with_google
            )
            summary = stringResource(R.string.authentication_required)
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
        value = org.tasks.kmp.org.tasks.time.getRelativeDay(
            timestamp,
            org.tasks.kmp.org.tasks.time.DateStyle.FULL,
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
    onCopyField: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    onHelp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.app_password_save)) },
        text = {
            Column {
                CopyableField(
                    label = stringResource(R.string.url),
                    value = caldavUrl,
                    onCopy = { onCopyField(R.string.url, caldavUrl) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                CopyableField(
                    label = stringResource(R.string.user),
                    value = username ?: "",
                    onCopy = {
                        username?.let { onCopyField(R.string.user, it) }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                CopyableField(
                    label = stringResource(R.string.password),
                    value = password ?: "",
                    onCopy = {
                        password?.let { onCopyField(R.string.password, it) }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onHelp) {
                Text(stringResource(R.string.help))
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
        title = { Text(stringResource(R.string.email_to_task_calendar)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
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
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
