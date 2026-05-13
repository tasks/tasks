package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.composeIcon
import org.tasks.data.composeTitle
import org.tasks.data.entity.CaldavAccount
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_edit_screen_options
import tasks.kmp.generated.resources.add_account
import tasks.kmp.generated.resources.backup_BPr_header
import tasks.kmp.generated.resources.caldav
import tasks.kmp.generated.resources.etesync
import tasks.kmp.generated.resources.date_and_time
import tasks.kmp.generated.resources.debug
import tasks.kmp.generated.resources.about
import tasks.kmp.generated.resources.local_lists
import tasks.kmp.generated.resources.tasks_org
import tasks.kmp.generated.resources.navigation_drawer
import tasks.kmp.generated.resources.notifications
import tasks.kmp.generated.resources.preferences_advanced
import tasks.kmp.generated.resources.preferences_look_and_feel
import tasks.kmp.generated.resources.settings
import tasks.kmp.generated.resources.task_defaults
import tasks.kmp.generated.resources.task_list_options
import tasks.kmp.generated.resources.link_desktop
import tasks.kmp.generated.resources.link_desktop_description
import tasks.kmp.generated.resources.widget_settings

sealed interface SettingsPane {
    val titleRes: StringResource
}

sealed class SettingsDestination(override val titleRes: StringResource) : SettingsPane {
    data object LookAndFeel : SettingsDestination(Res.string.preferences_look_and_feel)
    data object Notifications : SettingsDestination(Res.string.notifications)
    data object TaskDefaults : SettingsDestination(Res.string.task_defaults)
    data object TaskList : SettingsDestination(Res.string.task_list_options)
    data object TaskEdit : SettingsDestination(Res.string.EPr_edit_screen_options)
    data object DateAndTime : SettingsDestination(Res.string.date_and_time)
    data object NavigationDrawer : SettingsDestination(Res.string.navigation_drawer)
    data object Backups : SettingsDestination(Res.string.backup_BPr_header)
    data object Widgets : SettingsDestination(Res.string.widget_settings)
    data object Advanced : SettingsDestination(Res.string.preferences_advanced)
    data object HelpAndFeedback : SettingsDestination(Res.string.about)
    data object Debug : SettingsDestination(Res.string.debug)
}

data class LocalAccountSettingsPane(
    val account: CaldavAccount,
) : SettingsPane {
    override val titleRes: StringResource = Res.string.local_lists
}

data class TasksAccountSettingsPane(
    val account: CaldavAccount,
) : SettingsPane {
    override val titleRes: StringResource = Res.string.tasks_org
}

data class CaldavAccountSettingsPane(
    val account: CaldavAccount,
) : SettingsPane {
    override val titleRes: StringResource = Res.string.caldav
}

data class EtebaseAccountSettingsPane(
    val account: CaldavAccount,
) : SettingsPane {
    override val titleRes: StringResource = Res.string.etesync
}

data class OpenTaskAccountSettingsPane(
    val account: CaldavAccount,
) : SettingsPane {
    override val titleRes: StringResource = Res.string.settings
}

@Composable
fun MainSettingsScreen(
    accounts: List<CaldavAccount>,
    proCardState: ProCardState?,
    environmentLabel: String? = null,
    showBackupWarning: Boolean,
    showWidgets: Boolean,
    isDebug: Boolean = false,
    onAccountClick: (CaldavAccount) -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: (SettingsDestination) -> Unit,
    onProCardClick: () -> Unit,
    showDesktopLinking: Boolean = false,
    onLinkDesktopClick: () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Pro/tasks.org card
        if (proCardState != null) {
            AccountSettingsCard(
                state = proCardState,
                onClick = onProCardClick,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                environmentLabel = environmentLabel,
            )
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        // Accounts card group
        val hasTasksOrg = proCardState is ProCardState.TasksOrgAccount
        if (accounts.isNotEmpty() || !hasTasksOrg) {
            val totalAccountItems = accounts.size + if (!hasTasksOrg) 1 else 0
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                accounts.forEachIndexed { index, account ->
                    SettingsItemCard(
                        position = CardPosition.forIndex(index, totalAccountItems),
                    ) {
                        AccountRow(
                            account = account,
                            onClick = { onAccountClick(account) },
                        )
                    }
                }
                if (!hasTasksOrg) {
                    SettingsItemCard(
                        position = if (accounts.isEmpty()) CardPosition.Only else CardPosition.Last,
                    ) {
                        PreferenceRow(
                            title = stringResource(Res.string.add_account),
                            icon = Icons.Outlined.Add,
                            onClick = onAddAccountClick
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        if (showDesktopLinking) {
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            ) {
                SettingsItemCard(position = CardPosition.Only) {
                    PreferenceRow(
                        title = stringResource(Res.string.link_desktop),
                        summary = stringResource(Res.string.link_desktop_description),
                        icon = Icons.Outlined.Laptop,
                        onClick = onLinkDesktopClick,
                    )
                }
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        SettingsCategories(
            showBackupWarning = showBackupWarning,
            showWidgets = showWidgets,
            isDebug = isDebug,
            onSettingsClick = onSettingsClick,
        )

        bottomContent()
    }
}

@Composable
fun SettingsCategories(
    showBackupWarning: Boolean,
    showWidgets: Boolean,
    isDebug: Boolean,
    onSettingsClick: (SettingsDestination) -> Unit,
) {
    // Appearance island
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            PreferenceRow(
                title = stringResource(Res.string.preferences_look_and_feel),
                icon = Icons.Outlined.Palette,
                onClick = { onSettingsClick(SettingsDestination.LookAndFeel) }
            )
        }
        SettingsItemCard(position = CardPosition.Last) {
            PreferenceRow(
                title = stringResource(Res.string.notifications),
                icon = Icons.Outlined.Notifications,
                onClick = { onSettingsClick(SettingsDestination.Notifications) }
            )
        }
    }

    Spacer(modifier = Modifier.height(SettingsContentPadding))

    // Tasks island
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            PreferenceRow(
                title = stringResource(Res.string.task_defaults),
                icon = Icons.Outlined.Add,
                onClick = { onSettingsClick(SettingsDestination.TaskDefaults) }
            )
        }
        SettingsItemCard(position = CardPosition.Middle) {
            PreferenceRow(
                title = stringResource(Res.string.task_list_options),
                icon = Icons.AutoMirrored.Outlined.List,
                onClick = { onSettingsClick(SettingsDestination.TaskList) }
            )
        }
        SettingsItemCard(position = CardPosition.Last) {
            PreferenceRow(
                title = stringResource(Res.string.EPr_edit_screen_options),
                icon = Icons.Outlined.Edit,
                onClick = { onSettingsClick(SettingsDestination.TaskEdit) }
            )
        }
    }

    Spacer(modifier = Modifier.height(SettingsContentPadding))

    // Configuration island
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            PreferenceRow(
                title = stringResource(Res.string.date_and_time),
                icon = Icons.Outlined.Schedule,
                onClick = { onSettingsClick(SettingsDestination.DateAndTime) }
            )
        }
        SettingsItemCard(position = CardPosition.Middle) {
            PreferenceRow(
                title = stringResource(Res.string.navigation_drawer),
                icon = Icons.Outlined.Menu,
                onClick = { onSettingsClick(SettingsDestination.NavigationDrawer) }
            )
        }
        SettingsItemCard(position = CardPosition.Middle) {
            PreferenceRow(
                title = stringResource(Res.string.backup_BPr_header),
                icon = Icons.Outlined.SdStorage,
                showWarning = showBackupWarning,
                onClick = { onSettingsClick(SettingsDestination.Backups) }
            )
        }
        if (showWidgets) {
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(Res.string.widget_settings),
                    icon = Icons.Outlined.Widgets,
                    onClick = { onSettingsClick(SettingsDestination.Widgets) }
                )
            }
        }
        SettingsItemCard(position = CardPosition.Last) {
            PreferenceRow(
                title = stringResource(Res.string.preferences_advanced),
                icon = Icons.Outlined.Build,
                onClick = { onSettingsClick(SettingsDestination.Advanced) }
            )
        }
    }

    Spacer(modifier = Modifier.height(SettingsContentPadding))

    // About card group
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(
            position = if (isDebug) CardPosition.First else CardPosition.Only,
        ) {
            PreferenceRow(
                title = stringResource(Res.string.about),
                icon = Icons.Outlined.Info,
                onClick = { onSettingsClick(SettingsDestination.HelpAndFeedback) }
            )
        }
        if (isDebug) {
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.debug),
                    icon = Icons.Outlined.BugReport,
                    onClick = { onSettingsClick(SettingsDestination.Debug) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(SettingsContentPadding))
}

@Composable
private fun AccountRow(
    account: CaldavAccount,
    onClick: () -> Unit,
) {
    val icon = account.composeIcon
    val title = account.composeTitle
    PreferenceRow(
        title = if (title != null) stringResource(title) else account.name.orEmpty(),
        summary = account.name,
        iconDrawable = icon?.drawable,
        iconTint = if (icon?.tinted == true) null else Color.Unspecified,
        showError = account.hasError,
        onClick = onClick,
    )
}
