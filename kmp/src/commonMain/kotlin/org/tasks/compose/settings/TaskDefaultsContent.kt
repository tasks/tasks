package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.entity.Alarm
import org.tasks.reminders.alarmText
import org.tasks.viewmodel.TaskDefaultsViewModel
import org.tasks.viewmodel.TaskDefaultsViewModel.ListPickerTarget
import tasks.kmp.generated.resources.EPr_default_importance_title
import tasks.kmp.generated.resources.EPr_default_location_reminder_title
import tasks.kmp.generated.resources.EPr_default_reminders_mode_title
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.default_due_date
import tasks.kmp.generated.resources.default_start_date
import tasks.kmp.generated.resources.location_update_interval_title
import tasks.kmp.generated.resources.no_reminders
import tasks.kmp.generated.resources.repeats_from

@Composable
fun TaskDefaultsContent(
    viewModel: TaskDefaultsViewModel,
    is24HourFormat: Boolean,
    onDefaultList: () -> Unit,
    onDefaultTags: () -> Unit,
    onCalendar: () -> Unit,
    onRecurrence: () -> Unit,
    onReminders: () -> Unit,
    onLocation: () -> Unit,
    bottomInsets: @Composable () -> Unit = {},
) {
    if (!viewModel.loaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val settings = viewModel.settings
    TaskDefaultsScreen(
        settings = settings,
        defaultListName = viewModel.defaultListName,
        defaultTagsSummary = viewModel.defaultTagsSummary,
        calendarName = viewModel.calendarName,
        recurrenceSummary = viewModel.recurrenceSummary,
        remindersSummary = remindersSummary(settings.defaultAlarms, is24HourFormat),
        locationName = viewModel.locationName,
        hasDefaultLocation = viewModel.hasDefaultLocation,
        showCalendar = viewModel.showCalendar,
        showRingMode = viewModel.showRingMode,
        showLocation = viewModel.showLocation,
        onAddToTop = { viewModel.updateAddToTop(it) },
        onDefaultList = onDefaultList,
        onDefaultTags = onDefaultTags,
        onImportance = { viewModel.openListPicker(ListPickerTarget.PRIORITY) },
        onStartDate = { viewModel.openListPicker(ListPickerTarget.START_DATE) },
        onDueDate = { viewModel.openListPicker(ListPickerTarget.DUE_DATE) },
        onCalendar = onCalendar,
        onRecurrence = onRecurrence,
        onRecurrenceFrom = { viewModel.openListPicker(ListPickerTarget.REPEAT_FROM) },
        onReminders = onReminders,
        onRemindersMode = { viewModel.openListPicker(ListPickerTarget.RING_MODE) },
        onLocation = onLocation,
        onDeleteLocation = { viewModel.setDefaultLocation(null) },
        onLocationReminder = { viewModel.openListPicker(ListPickerTarget.LOCATION_REMINDER) },
        onLocationUpdateInterval = {
            viewModel.openListPicker(ListPickerTarget.LOCATION_UPDATE_INTERVAL)
        },
        bottomInsets = bottomInsets,
    )

    viewModel.listPickerTarget?.let { target ->
        key(target) {
            ListPreferenceDialog(
                title = stringResource(target.title),
                options = target.options,
                selected = viewModel.listPickerValue(target),
                onSelect = { viewModel.setListPickerValue(target, it) },
                onDismiss = { viewModel.dismissListPicker() },
            )
        }
    }
}

@Composable
private fun remindersSummary(alarms: List<Alarm>, is24HourFormat: Boolean): String {
    if (alarms.isEmpty()) {
        return stringResource(Res.string.no_reminders)
    }
    return alarms
        .map { alarmText(it, is24HourFormat).replace("\n", ", ") }
        .joinToString("\n")
}

private val ListPickerTarget.title: StringResource
    get() = when (this) {
        ListPickerTarget.PRIORITY -> Res.string.EPr_default_importance_title
        ListPickerTarget.START_DATE -> Res.string.default_start_date
        ListPickerTarget.DUE_DATE -> Res.string.default_due_date
        ListPickerTarget.REPEAT_FROM -> Res.string.repeats_from
        ListPickerTarget.RING_MODE -> Res.string.EPr_default_reminders_mode_title
        ListPickerTarget.LOCATION_REMINDER -> Res.string.EPr_default_location_reminder_title
        ListPickerTarget.LOCATION_UPDATE_INTERVAL -> Res.string.location_update_interval_title
    }

private val ListPickerTarget.options: List<Pair<Int, StringResource>>
    get() = when (this) {
        ListPickerTarget.PRIORITY -> PriorityOptions
        ListPickerTarget.START_DATE -> StartDateOptions
        ListPickerTarget.DUE_DATE -> DueDateOptions
        ListPickerTarget.REPEAT_FROM -> RepeatFromOptions
        ListPickerTarget.RING_MODE -> RingModeOptions
        ListPickerTarget.LOCATION_REMINDER -> LocationReminderOptions
        ListPickerTarget.LOCATION_UPDATE_INTERVAL -> LocationUpdateIntervalOptions
    }
