package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun TaskDefaultsScreen(
    addToTopEnabled: Boolean,
    defaultListName: String,
    defaultTagsSummary: String,
    importanceSummary: String,
    startDateSummary: String,
    dueDateSummary: String,
    calendarName: String,
    recurrenceSummary: String,
    recurrenceFromSummary: String,
    remindersSummary: String,
    randomReminderSummary: String,
    remindersModeSummary: String,
    locationName: String,
    hasDefaultLocation: Boolean,
    locationReminderSummary: String,
    onAddToTop: (Boolean) -> Unit,
    onDefaultList: () -> Unit,
    onDefaultTags: () -> Unit,
    onImportance: () -> Unit,
    onStartDate: () -> Unit,
    onDueDate: () -> Unit,
    onCalendar: () -> Unit,
    onRecurrence: () -> Unit,
    onRecurrenceFrom: () -> Unit,
    onReminders: () -> Unit,
    onRandomReminder: () -> Unit,
    onRemindersMode: () -> Unit,
    onLocation: () -> Unit,
    onDeleteLocation: () -> Unit,
    onLocationReminder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // New tasks on top island
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(R.string.google_tasks_add_to_top),
                icon = Icons.Outlined.VerticalAlignTop,
                checked = addToTopEnabled,
                onCheckedChange = onAddToTop,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Defaults island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.default_list),
                    icon = Icons.AutoMirrored.Outlined.List,
                    summary = defaultListName,
                    onClick = onDefaultList,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.default_tags),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    summary = defaultTagsSummary,
                    onClick = onDefaultTags,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_default_importance_title),
                    icon = Icons.Outlined.Flag,
                    summary = importanceSummary,
                    onClick = onImportance,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.default_start_date),
                    icon = Icons.Outlined.PendingActions,
                    summary = startDateSummary,
                    onClick = onStartDate,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.default_due_date),
                    icon = Icons.Outlined.Schedule,
                    summary = dueDateSummary,
                    onClick = onDueDate,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.default_calendar),
                    icon = Icons.Outlined.Event,
                    summary = calendarName,
                    onClick = onCalendar,
                )
            }
        }

        // Reminders section
        SectionHeader(
            R.string.TEA_control_reminders,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_default_reminders_title),
                    icon = Icons.Outlined.Notifications,
                    summary = remindersSummary,
                    summaryMaxLines = Int.MAX_VALUE,
                    onClick = onReminders,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.rmd_EPr_defaultRemind_title),
                    summary = randomReminderSummary,
                    onClick = onRandomReminder,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_default_reminders_mode_title),
                    summary = remindersModeSummary,
                    onClick = onRemindersMode,
                )
            }
        }

        // Recurrence section
        SectionHeader(
            R.string.TEA_control_repeat,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.default_recurrence),
                    icon = Icons.Outlined.Repeat,
                    summary = recurrenceSummary,
                    onClick = onRecurrence,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.repeats_from),
                    summary = recurrenceFromSummary,
                    onClick = onRecurrenceFrom,
                )
            }
        }

        // Location section
        SectionHeader(
            R.string.TEA_control_location,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.default_location),
                    icon = Icons.Outlined.Place,
                    summary = locationName,
                    onClick = onLocation,
                    trailing = if (hasDefaultLocation) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = SettingsContentPadding)
                                    .size(SettingsIconSize)
                                    .clickable(onClick = onDeleteLocation),
                                tint = colorResource(R.color.icon_tint_with_alpha),
                            )
                        }
                    } else null,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.EPr_default_location_reminder_title),
                    summary = locationReminderSummary,
                    onClick = onLocationReminder,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
