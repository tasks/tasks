package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
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
import org.tasks.kmp.formatTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_voiceRemindersEnabled_desc_enabled
import tasks.kmp.generated.resources.EPr_voiceRemindersEnabled_title
import tasks.kmp.generated.resources.add_default_reminders
import tasks.kmp.generated.resources.bundle_notifications
import tasks.kmp.generated.resources.bundle_notifications_summary
import tasks.kmp.generated.resources.completion_sound
import tasks.kmp.generated.resources.default_reminder
import tasks.kmp.generated.resources.disable_battery_optimizations
import tasks.kmp.generated.resources.enabled
import tasks.kmp.generated.resources.more_settings
import tasks.kmp.generated.resources.notification_disable_battery_optimizations_description
import tasks.kmp.generated.resources.notification_troubleshooting_summary
import tasks.kmp.generated.resources.persistent_notifications
import tasks.kmp.generated.resources.persistent_notifications_description
import tasks.kmp.generated.resources.quiet_hours
import tasks.kmp.generated.resources.quiet_hours_in_effect
import tasks.kmp.generated.resources.quiet_hours_summary
import tasks.kmp.generated.resources.rmd_EPr_quiet_hours_end_title
import tasks.kmp.generated.resources.rmd_EPr_quiet_hours_start_title
import tasks.kmp.generated.resources.rmd_EPr_rmd_time_title
import tasks.kmp.generated.resources.rmd_time_enabled_off
import tasks.kmp.generated.resources.rmd_time_enabled_on
import tasks.kmp.generated.resources.swipe_to_snooze_description
import tasks.kmp.generated.resources.swipe_to_snooze_time_1_hour
import tasks.kmp.generated.resources.swipe_to_snooze_time_15_minutes
import tasks.kmp.generated.resources.swipe_to_snooze_time_24_hours
import tasks.kmp.generated.resources.swipe_to_snooze_time_30_minutes
import tasks.kmp.generated.resources.swipe_to_snooze_time_description
import tasks.kmp.generated.resources.swipe_to_snooze_time_immediately
import tasks.kmp.generated.resources.swipe_to_snooze_title
import tasks.kmp.generated.resources.troubleshooting
import tasks.kmp.generated.resources.wearable_notifications
import tasks.kmp.generated.resources.wearable_notifications_summary

val SnoozeTimes: List<Pair<Int, StringResource>> = listOf(
    0 to Res.string.swipe_to_snooze_time_immediately,
    15 to Res.string.swipe_to_snooze_time_15_minutes,
    30 to Res.string.swipe_to_snooze_time_30_minutes,
    60 to Res.string.swipe_to_snooze_time_1_hour,
    1440 to Res.string.swipe_to_snooze_time_24_hours,
)

private fun snoozeLabel(minutes: Int): StringResource =
    SnoozeTimes.firstOrNull { it.first == minutes }?.second ?: SnoozeTimes.first().second

private fun timeSummary(millisOfDay: Int, is24HourFormat: Boolean): String =
    formatTime(currentTimeMillis().withMillisOfDay(millisOfDay), is24HourFormat)

@Composable
fun NotificationsScreen(
    is24HourFormat: Boolean,
    isCurrentlyQuietHours: Boolean,
    showTroubleshooting: Boolean,
    showBatteryOptimization: Boolean,
    showCompletionSound: Boolean,
    completionSoundName: String,
    showOngoingNotifications: Boolean,
    showBundleNotifications: Boolean,
    showVoiceReminders: Boolean,
    showSystemNotificationSettings: Boolean,
    moreSettingsSummary: String?,
    showSwipeToSnooze: Boolean,
    persistentEnabled: Boolean,
    wearableEnabled: Boolean,
    bundleEnabled: Boolean,
    voiceEnabled: Boolean,
    swipeToSnoozeEnabled: Boolean,
    swipeToSnoozeMinutes: Int,
    defaultRemindersEnabled: Boolean,
    defaultReminderTime: Int,
    quietHoursEnabled: Boolean,
    quietHoursStart: Int,
    quietHoursEnd: Int,
    onTroubleshooting: () -> Unit,
    onBatteryOptimization: () -> Unit,
    onCompletionSound: () -> Unit,
    onPersistent: (Boolean) -> Unit,
    onWearable: (Boolean) -> Unit,
    onBundle: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onMoreSettings: () -> Unit,
    onSwipeToSnooze: (Boolean) -> Unit,
    onSnoozeTime: () -> Unit,
    onDefaultReminders: (Boolean) -> Unit,
    onReminderTime: () -> Unit,
    onQuietHours: (Boolean) -> Unit,
    onQuietStart: () -> Unit,
    onQuietEnd: () -> Unit,
    bottomInsets: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        if (isCurrentlyQuietHours) {
            DangerCard(
                icon = Icons.Outlined.NotificationsOff,
                title = stringResource(Res.string.quiet_hours_in_effect),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        CardGroup(
            listOfNotNull(
                TroubleshootingRow.TROUBLESHOOTING.takeIf { showTroubleshooting },
                TroubleshootingRow.BATTERY_OPTIMIZATION.takeIf { showBatteryOptimization },
            )
        ) { row ->
            when (row) {
                TroubleshootingRow.TROUBLESHOOTING ->
                    PreferenceRow(
                        title = stringResource(Res.string.troubleshooting),
                        summary = stringResource(Res.string.notification_troubleshooting_summary),
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = onTroubleshooting,
                    )
                TroubleshootingRow.BATTERY_OPTIMIZATION ->
                    PreferenceRow(
                        title = stringResource(Res.string.disable_battery_optimizations),
                        summary = stringResource(Res.string.notification_disable_battery_optimizations_description),
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = onBatteryOptimization,
                    )
            }
        }

        CardGroup(
            listOfNotNull(
                NotificationRow.COMPLETION_SOUND.takeIf { showCompletionSound },
                NotificationRow.PERSISTENT.takeIf { showOngoingNotifications },
                NotificationRow.WEARABLE.takeIf { showOngoingNotifications },
                NotificationRow.BUNDLE.takeIf { showBundleNotifications },
                NotificationRow.VOICE.takeIf { showVoiceReminders },
            )
        ) { row ->
            when (row) {
                NotificationRow.COMPLETION_SOUND ->
                    PreferenceRow(
                        title = stringResource(Res.string.completion_sound),
                        summary = completionSoundName,
                        onClick = onCompletionSound,
                    )
                NotificationRow.PERSISTENT ->
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.persistent_notifications),
                        summary = stringResource(Res.string.persistent_notifications_description),
                        checked = persistentEnabled,
                        onCheckedChange = onPersistent,
                    )
                NotificationRow.WEARABLE ->
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.wearable_notifications),
                        summary = stringResource(Res.string.wearable_notifications_summary),
                        checked = wearableEnabled,
                        onCheckedChange = onWearable,
                    )
                NotificationRow.BUNDLE ->
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.bundle_notifications),
                        summary = stringResource(Res.string.bundle_notifications_summary),
                        checked = bundleEnabled,
                        onCheckedChange = onBundle,
                    )
                NotificationRow.VOICE ->
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.EPr_voiceRemindersEnabled_title),
                        summary = stringResource(Res.string.EPr_voiceRemindersEnabled_desc_enabled),
                        checked = voiceEnabled,
                        onCheckedChange = onVoice,
                    )
            }
        }

        if (showSystemNotificationSettings) {
            SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                PreferenceRow(
                    title = stringResource(Res.string.more_settings),
                    summary = moreSettingsSummary,
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = onMoreSettings,
                )
            }
        }

        SectionHeader(
            stringResource(Res.string.default_reminder),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.add_default_reminders),
                    summary = if (defaultRemindersEnabled)
                        stringResource(Res.string.rmd_time_enabled_on)
                    else
                        stringResource(Res.string.rmd_time_enabled_off),
                    checked = defaultRemindersEnabled,
                    onCheckedChange = onDefaultReminders,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.rmd_EPr_rmd_time_title),
                    summary = timeSummary(defaultReminderTime, is24HourFormat),
                    onClick = onReminderTime,
                )
            }
        }

        if (showSwipeToSnooze) {
            SectionHeader(
                stringResource(Res.string.swipe_to_snooze_title),
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Column(
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
            ) {
                SettingsItemCard(position = CardPosition.First) {
                    SwitchPreferenceRow(
                        title = stringResource(Res.string.enabled),
                        checked = swipeToSnoozeEnabled,
                        onCheckedChange = onSwipeToSnooze,
                    )
                }
                SettingsItemCard(position = CardPosition.Last) {
                    PreferenceRow(
                        title = stringResource(Res.string.swipe_to_snooze_description),
                        summary = stringResource(
                            Res.string.swipe_to_snooze_time_description,
                            stringResource(snoozeLabel(swipeToSnoozeMinutes)),
                        ),
                        enabled = swipeToSnoozeEnabled,
                        onClick = onSnoozeTime,
                    )
                }
            }
        }

        SectionHeader(
            stringResource(Res.string.quiet_hours),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.enabled),
                    summary = stringResource(Res.string.quiet_hours_summary),
                    checked = quietHoursEnabled,
                    onCheckedChange = onQuietHours,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(Res.string.rmd_EPr_quiet_hours_start_title),
                    summary = timeSummary(quietHoursStart, is24HourFormat),
                    enabled = quietHoursEnabled,
                    onClick = onQuietStart,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.rmd_EPr_quiet_hours_end_title),
                    summary = timeSummary(quietHoursEnd, is24HourFormat),
                    enabled = quietHoursEnabled,
                    onClick = onQuietEnd,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        bottomInsets()
    }
}

private enum class TroubleshootingRow {
    TROUBLESHOOTING,
    BATTERY_OPTIMIZATION,
}

private enum class NotificationRow {
    COMPLETION_SOUND,
    PERSISTENT,
    WEARABLE,
    BUNDLE,
    VOICE,
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
    Spacer(modifier = Modifier.height(SettingsContentPadding))
}

@Composable
fun SnoozeTimeDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.swipe_to_snooze_description)) },
        text = {
            Column {
                SnoozeTimes.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = minutes == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(minutes) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = minutes == selected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(label),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
