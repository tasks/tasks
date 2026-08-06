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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.edit.ringModeString
import org.tasks.compose.pickers.RING_FIVE_TIMES
import org.tasks.compose.pickers.RING_NONSTOP
import org.tasks.compose.pickers.RING_ONCE
import org.tasks.data.entity.Task
import org.tasks.preferences.TaskDefaultSettings
import tasks.kmp.generated.resources.EPr_default_importance_title
import tasks.kmp.generated.resources.EPr_default_location_reminder_title
import tasks.kmp.generated.resources.EPr_default_reminders_mode_title
import tasks.kmp.generated.resources.EPr_default_reminders_title
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.TEA_control_location
import tasks.kmp.generated.resources.TEA_control_reminders
import tasks.kmp.generated.resources.TEA_control_repeat
import tasks.kmp.generated.resources.day_after_tomorrow
import tasks.kmp.generated.resources.day_before_due
import tasks.kmp.generated.resources.default_calendar
import tasks.kmp.generated.resources.default_due_date
import tasks.kmp.generated.resources.default_list
import tasks.kmp.generated.resources.default_location
import tasks.kmp.generated.resources.default_location_reminder_on_arrival
import tasks.kmp.generated.resources.default_location_reminder_on_arrival_or_departure
import tasks.kmp.generated.resources.default_location_reminder_on_departure
import tasks.kmp.generated.resources.default_recurrence
import tasks.kmp.generated.resources.default_start_date
import tasks.kmp.generated.resources.default_tags
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.due_time
import tasks.kmp.generated.resources.google_tasks_add_to_top
import tasks.kmp.generated.resources.location_update_interval_15_minutes
import tasks.kmp.generated.resources.location_update_interval_1_hour
import tasks.kmp.generated.resources.location_update_interval_2_hours
import tasks.kmp.generated.resources.location_update_interval_30_minutes
import tasks.kmp.generated.resources.location_update_interval_disabled
import tasks.kmp.generated.resources.location_update_interval_title
import tasks.kmp.generated.resources.next_week
import tasks.kmp.generated.resources.no_due_date
import tasks.kmp.generated.resources.no_reminders
import tasks.kmp.generated.resources.no_start_date
import tasks.kmp.generated.resources.none
import tasks.kmp.generated.resources.priority_high
import tasks.kmp.generated.resources.priority_low
import tasks.kmp.generated.resources.priority_medium
import tasks.kmp.generated.resources.repeat_type_completion_capitalized
import tasks.kmp.generated.resources.repeats_from
import tasks.kmp.generated.resources.today
import tasks.kmp.generated.resources.tomorrow
import tasks.kmp.generated.resources.week_before_due

val PriorityOptions: List<Pair<Int, StringResource>> = listOf(
    Task.Priority.HIGH to Res.string.priority_high,
    Task.Priority.MEDIUM to Res.string.priority_medium,
    Task.Priority.LOW to Res.string.priority_low,
    Task.Priority.NONE to Res.string.none,
)

val StartDateOptions: List<Pair<Int, StringResource>> = listOf(
    Task.HIDE_UNTIL_NONE to Res.string.no_start_date,
    Task.HIDE_UNTIL_DUE to Res.string.due_date,
    Task.HIDE_UNTIL_DUE_TIME to Res.string.due_time,
    Task.HIDE_UNTIL_DAY_BEFORE to Res.string.day_before_due,
    Task.HIDE_UNTIL_WEEK_BEFORE to Res.string.week_before_due,
)

val DueDateOptions: List<Pair<Int, StringResource>> = listOf(
    Task.URGENCY_NONE to Res.string.no_due_date,
    Task.URGENCY_TODAY to Res.string.today,
    Task.URGENCY_TOMORROW to Res.string.tomorrow,
    Task.URGENCY_DAY_AFTER to Res.string.day_after_tomorrow,
    Task.URGENCY_NEXT_WEEK to Res.string.next_week,
)

val RepeatFromOptions: List<Pair<Int, StringResource>> = listOf(
    Task.RepeatFrom.DUE_DATE to Res.string.due_date,
    Task.RepeatFrom.COMPLETION_DATE to Res.string.repeat_type_completion_capitalized,
)

val RingModeOptions: List<Pair<Int, StringResource>> = listOf(
    0 to ringModeString(RING_ONCE),
    Task.NOTIFY_MODE_FIVE to ringModeString(RING_FIVE_TIMES),
    Task.NOTIFY_MODE_NONSTOP to ringModeString(RING_NONSTOP),
)

val LocationReminderOptions: List<Pair<Int, StringResource>> = listOf(
    0 to Res.string.no_reminders,
    1 to Res.string.default_location_reminder_on_arrival,
    2 to Res.string.default_location_reminder_on_departure,
    3 to Res.string.default_location_reminder_on_arrival_or_departure,
)

val LocationUpdateIntervalOptions: List<Pair<Int, StringResource>> = listOf(
    0 to Res.string.location_update_interval_disabled,
    15 to Res.string.location_update_interval_15_minutes,
    30 to Res.string.location_update_interval_30_minutes,
    60 to Res.string.location_update_interval_1_hour,
    120 to Res.string.location_update_interval_2_hours,
)

@Composable
private fun List<Pair<Int, StringResource>>.label(value: Int): String =
    stringResource(firstOrNull { it.first == value }?.second ?: first().second)

@Composable
fun TaskDefaultsScreen(
    settings: TaskDefaultSettings,
    defaultListName: String,
    defaultTagsSummary: String,
    calendarName: String,
    recurrenceSummary: String,
    remindersSummary: String,
    locationName: String,
    hasDefaultLocation: Boolean,
    showCalendar: Boolean,
    showRingMode: Boolean,
    showLocation: Boolean,
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
    onRemindersMode: () -> Unit,
    onLocation: () -> Unit,
    onDeleteLocation: () -> Unit,
    onLocationReminder: () -> Unit,
    onLocationUpdateInterval: () -> Unit,
    bottomInsets: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SwitchPreferenceRow(
                title = stringResource(Res.string.google_tasks_add_to_top),
                icon = Icons.Outlined.VerticalAlignTop,
                checked = settings.addTasksToTop,
                onCheckedChange = onAddToTop,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        CardGroup(
            listOfNotNull(
                DefaultRow.LIST,
                DefaultRow.TAGS,
                DefaultRow.IMPORTANCE,
                DefaultRow.START_DATE,
                DefaultRow.DUE_DATE,
                DefaultRow.CALENDAR.takeIf { showCalendar },
            )
        ) { row ->
            when (row) {
                DefaultRow.LIST ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_list),
                        icon = Icons.AutoMirrored.Outlined.List,
                        summary = defaultListName,
                        onClick = onDefaultList,
                    )
                DefaultRow.TAGS ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_tags),
                        icon = Icons.AutoMirrored.Outlined.Label,
                        summary = defaultTagsSummary,
                        onClick = onDefaultTags,
                    )
                DefaultRow.IMPORTANCE ->
                    PreferenceRow(
                        title = stringResource(Res.string.EPr_default_importance_title),
                        icon = Icons.Outlined.Flag,
                        summary = PriorityOptions.label(settings.defaultPriority),
                        onClick = onImportance,
                    )
                DefaultRow.START_DATE ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_start_date),
                        icon = Icons.Outlined.PendingActions,
                        summary = StartDateOptions.label(settings.defaultHideUntil),
                        onClick = onStartDate,
                    )
                DefaultRow.DUE_DATE ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_due_date),
                        icon = Icons.Outlined.Schedule,
                        summary = DueDateOptions.label(settings.defaultDueDate),
                        onClick = onDueDate,
                    )
                DefaultRow.CALENDAR ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_calendar),
                        icon = Icons.Outlined.Event,
                        summary = calendarName,
                        onClick = onCalendar,
                    )
            }
        }

        SectionHeader(
            stringResource(Res.string.TEA_control_reminders),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        CardGroup(
            listOfNotNull(
                ReminderRow.REMINDERS,
                ReminderRow.RING_MODE.takeIf { showRingMode },
            )
        ) { row ->
            when (row) {
                ReminderRow.REMINDERS ->
                    PreferenceRow(
                        title = stringResource(Res.string.EPr_default_reminders_title),
                        icon = Icons.Outlined.Notifications,
                        summary = remindersSummary,
                        summaryMaxLines = Int.MAX_VALUE,
                        onClick = onReminders,
                    )
                ReminderRow.RING_MODE ->
                    PreferenceRow(
                        title = stringResource(Res.string.EPr_default_reminders_mode_title),
                        summary = RingModeOptions.label(settings.defaultRingMode),
                        onClick = onRemindersMode,
                    )
            }
        }

        SectionHeader(
            stringResource(Res.string.TEA_control_repeat),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        CardGroup(listOf(RecurrenceRow.RECURRENCE, RecurrenceRow.REPEAT_FROM)) { row ->
            when (row) {
                RecurrenceRow.RECURRENCE ->
                    PreferenceRow(
                        title = stringResource(Res.string.default_recurrence),
                        icon = Icons.Outlined.Repeat,
                        summary = recurrenceSummary,
                        onClick = onRecurrence,
                    )
                RecurrenceRow.REPEAT_FROM ->
                    PreferenceRow(
                        title = stringResource(Res.string.repeats_from),
                        summary = RepeatFromOptions.label(settings.defaultRecurrenceFrom),
                        onClick = onRecurrenceFrom,
                    )
            }
        }

        if (showLocation) {
            SectionHeader(
                stringResource(Res.string.TEA_control_location),
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            CardGroup(
                listOf(
                    LocationRow.LOCATION,
                    LocationRow.LOCATION_REMINDER,
                    LocationRow.UPDATE_INTERVAL,
                )
            ) { row ->
                when (row) {
                    LocationRow.LOCATION ->
                        PreferenceRow(
                            title = stringResource(Res.string.default_location),
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else null,
                        )
                    LocationRow.LOCATION_REMINDER ->
                        PreferenceRow(
                            title = stringResource(Res.string.EPr_default_location_reminder_title),
                            summary = LocationReminderOptions
                                .label(settings.defaultLocationReminder),
                            onClick = onLocationReminder,
                        )
                    LocationRow.UPDATE_INTERVAL ->
                        PreferenceRow(
                            title = stringResource(Res.string.location_update_interval_title),
                            summary = LocationUpdateIntervalOptions
                                .label(settings.locationUpdateIntervalMinutes),
                            onClick = onLocationUpdateInterval,
                        )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        bottomInsets()
    }
}

private enum class DefaultRow {
    LIST,
    TAGS,
    IMPORTANCE,
    START_DATE,
    DUE_DATE,
    CALENDAR,
}

private enum class ReminderRow {
    REMINDERS,
    RING_MODE,
}

private enum class RecurrenceRow {
    RECURRENCE,
    REPEAT_FROM,
}

private enum class LocationRow {
    LOCATION,
    LOCATION_REMINDER,
    UPDATE_INTERVAL,
}

@Composable
private fun <T : Any> CardGroup(rows: List<T>, row: @Composable (T) -> Unit) {
    if (rows.isEmpty()) return
    Column(
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        rows.forEachIndexed { index, item ->
            key(item) {
                SettingsItemCard(position = CardPosition.forIndex(index, rows.size)) {
                    row(item)
                }
            }
        }
    }
}

@Composable
fun ListPreferenceDialog(
    title: String,
    options: List<Pair<Int, StringResource>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                        )
                        Text(
                            text = stringResource(label),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
