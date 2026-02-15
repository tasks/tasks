package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.entity.CaldavAccount

sealed class SettingsDestination(val titleRes: Int) {
    data object LookAndFeel : SettingsDestination(R.string.preferences_look_and_feel)
    data object Notifications : SettingsDestination(R.string.notifications)
    data object TaskDefaults : SettingsDestination(R.string.task_defaults)
    data object TaskList : SettingsDestination(R.string.task_list_options)
    data object TaskEdit : SettingsDestination(R.string.EPr_edit_screen_options)
    data object DateAndTime : SettingsDestination(R.string.date_and_time)
    data object NavigationDrawer : SettingsDestination(R.string.navigation_drawer)
    data object Backups : SettingsDestination(R.string.backup_BPr_header)
    data object Widgets : SettingsDestination(R.string.widget_settings)
    data object Advanced : SettingsDestination(R.string.preferences_advanced)
    data object HelpAndFeedback : SettingsDestination(R.string.about)
    data object Debug : SettingsDestination(R.string.debug)
}

@Composable
fun MainSettingsScreen(
    accounts: List<CaldavAccount>,
    proCardState: ProCardState?,
    showBackupWarning: Boolean,
    showWidgets: Boolean,
    isDebug: Boolean = BuildConfig.DEBUG,
    onAccountClick: (CaldavAccount) -> Unit,
    onAddAccountClick: () -> Unit,
    onSettingsClick: (SettingsDestination) -> Unit,
    onProCardClick: () -> Unit,
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
                        position = cardPosition(index, totalAccountItems),
                    ) {
                        AccountRow(
                            account = account,
                            onClick = { onAccountClick(account) }
                        )
                    }
                }
                if (!hasTasksOrg) {
                    SettingsItemCard(
                        position = if (accounts.isEmpty()) CardPosition.Only else CardPosition.Last,
                    ) {
                        PreferenceRow(
                            title = stringResource(R.string.add_account),
                            icon = Icons.Outlined.Add,
                            onClick = onAddAccountClick
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        // Appearance island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.preferences_look_and_feel),
                    icon = Icons.Outlined.Palette,
                    onClick = { onSettingsClick(SettingsDestination.LookAndFeel) }
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.notifications),
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
                    title = stringResource(R.string.task_defaults),
                    icon = Icons.Outlined.Add,
                    onClick = { onSettingsClick(SettingsDestination.TaskDefaults) }
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.task_list_options),
                    icon = Icons.AutoMirrored.Outlined.List,
                    onClick = { onSettingsClick(SettingsDestination.TaskList) }
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_edit_screen_options),
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
                    title = stringResource(R.string.date_and_time),
                    icon = Icons.Outlined.Schedule,
                    onClick = { onSettingsClick(SettingsDestination.DateAndTime) }
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.navigation_drawer),
                    icon = Icons.Outlined.Menu,
                    onClick = { onSettingsClick(SettingsDestination.NavigationDrawer) }
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.backup_BPr_header),
                    icon = Icons.Outlined.SdStorage,
                    showWarning = showBackupWarning,
                    onClick = { onSettingsClick(SettingsDestination.Backups) }
                )
            }
            if (showWidgets) {
                SettingsItemCard(position = CardPosition.Middle) {
                    PreferenceRow(
                        title = stringResource(R.string.widget_settings),
                        icon = Icons.Outlined.Widgets,
                        onClick = { onSettingsClick(SettingsDestination.Widgets) }
                    )
                }
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.preferences_advanced),
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
                    title = stringResource(R.string.about),
                    icon = Icons.Outlined.Info,
                    onClick = { onSettingsClick(SettingsDestination.HelpAndFeedback) }
                )
            }
            if (isDebug) {
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(R.string.debug),
                        icon = Icons.Outlined.BugReport,
                        onClick = { onSettingsClick(SettingsDestination.Debug) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
