package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.tasks.TasksUrls
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.viewmodel.NotificationsViewModel
import org.tasks.viewmodel.NotificationsViewModel.TimePickerTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    viewModel: NotificationsViewModel,
    is24HourFormat: Boolean,
    openUri: (String) -> Unit,
    onBatteryOptimization: (() -> Unit)? = null,
    onCompletionSound: (() -> Unit)? = null,
    onMoreSettings: (() -> Unit)? = null,
    moreSettingsSummary: String? = null,
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
    NotificationsScreen(
        is24HourFormat = is24HourFormat,
        isCurrentlyQuietHours = viewModel.isCurrentlyQuietHours,
        showTroubleshooting = viewModel.showTroubleshooting,
        showBatteryOptimization = viewModel.showBatteryOptimization,
        showCompletionSound = viewModel.showCompletionSound,
        completionSoundName = viewModel.completionSoundName,
        showOngoingNotifications = viewModel.showOngoingNotifications,
        showBundleNotifications = viewModel.showBundleNotifications,
        showVoiceReminders = viewModel.showVoiceReminders,
        showSystemNotificationSettings = viewModel.showSystemNotificationSettings,
        moreSettingsSummary = moreSettingsSummary,
        showSwipeToSnooze = viewModel.showSwipeToSnooze,
        persistentEnabled = settings.persistentNotifications,
        wearableEnabled = settings.wearableNotifications,
        bundleEnabled = settings.bundleNotifications,
        voiceEnabled = settings.voiceReminders,
        swipeToSnoozeEnabled = settings.swipeToSnoozeEnabled,
        swipeToSnoozeMinutes = settings.swipeToSnoozeMinutes,
        defaultRemindersEnabled = settings.defaultRemindersEnabled,
        defaultReminderTime = settings.defaultReminderTime,
        quietHoursEnabled = settings.quietHoursEnabled,
        quietHoursStart = settings.quietHoursStart,
        quietHoursEnd = settings.quietHoursEnd,
        onTroubleshooting = { openUri(TasksUrls.NOTIFICATIONS) },
        onBatteryOptimization = { onBatteryOptimization?.invoke() },
        onCompletionSound = { onCompletionSound?.invoke() },
        onPersistent = { viewModel.updatePersistent(it) },
        onWearable = { viewModel.updateWearable(it) },
        onBundle = { viewModel.updateBundle(it) },
        onVoice = { viewModel.updateVoice(it) },
        onMoreSettings = { onMoreSettings?.invoke() },
        onSwipeToSnooze = { viewModel.updateSwipeToSnooze(it) },
        onSnoozeTime = { viewModel.openSnoozeDialog() },
        onDefaultReminders = { viewModel.updateDefaultReminders(it) },
        onReminderTime = { viewModel.openTimePicker(TimePickerTarget.DEFAULT_REMINDER) },
        onQuietHours = { viewModel.updateQuietHours(it) },
        onQuietStart = { viewModel.openTimePicker(TimePickerTarget.QUIET_HOURS_START) },
        onQuietEnd = { viewModel.openTimePicker(TimePickerTarget.QUIET_HOURS_END) },
        bottomInsets = bottomInsets,
    )

    if (viewModel.showSnoozeDialog) {
        SnoozeTimeDialog(
            selected = settings.swipeToSnoozeMinutes,
            onSelect = { viewModel.setSnoozeTime(it) },
            onDismiss = { viewModel.dismissSnoozeDialog() },
        )
    }

    viewModel.timePickerTarget?.let { target ->
        key(target) {
            val initial = viewModel.timePickerInitialValue(target)
            TimePickerDialog(
                state = rememberTimePickerState(
                    initialHour = initial / (60 * 60_000),
                    initialMinute = (initial / 60_000) % 60,
                    is24Hour = is24HourFormat,
                ),
                initialDisplayMode = if (viewModel.timePickerInputMode) {
                    DisplayMode.Input
                } else {
                    DisplayMode.Picker
                },
                setDisplayMode = { viewModel.updateTimePickerInputMode(it == DisplayMode.Input) },
                selected = { viewModel.setTime(target, it) },
                dismiss = { viewModel.dismissTimePicker() },
            )
        }
    }
}
