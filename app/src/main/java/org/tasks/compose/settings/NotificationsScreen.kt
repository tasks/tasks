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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun NotificationsScreen(
    showBatteryOptimization: Boolean,
    completionSoundName: String,
    showPreUpsideDownCake: Boolean,
    persistentEnabled: Boolean,
    wearableEnabled: Boolean,
    bundleEnabled: Boolean,
    voiceEnabled: Boolean,
    swipeToSnoozeEnabled: Boolean,
    snoozeSummary: String,
    defaultRemindersEnabled: Boolean,
    reminderTimeSummary: String,
    quietHoursEnabled: Boolean,
    isCurrentlyQuietHours: Boolean,
    quietStartSummary: String,
    quietEndSummary: String,
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
                title = stringResource(R.string.quiet_hours_in_effect),
                tint = colorResource(R.color.overdue),
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        // Troubleshooting island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val topTotal = if (showBatteryOptimization) 2 else 1
            var topI = 0

            SettingsItemCard(position = cardPosition(topI++, topTotal)) {
                PreferenceRow(
                    title = stringResource(R.string.troubleshooting),
                    summary = stringResource(R.string.notification_troubleshooting_summary),
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = onTroubleshooting,
                )
            }
            if (showBatteryOptimization) {
                SettingsItemCard(position = cardPosition(topI, topTotal)) {
                    PreferenceRow(
                        title = stringResource(R.string.disable_battery_optimizations),
                        summary = stringResource(R.string.notification_disable_battery_optimizations_description),
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = onBatteryOptimization,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // General notifications island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            val total = 3 + (if (showPreUpsideDownCake) 2 else 0)
            var i = 0

            SettingsItemCard(position = cardPosition(i++, total)) {
                PreferenceRow(
                    title = stringResource(R.string.completion_sound),
                    summary = completionSoundName,
                    onClick = onCompletionSound,
                )
            }
            if (showPreUpsideDownCake) {
                SettingsItemCard(position = cardPosition(i++, total)) {
                    SwitchPreferenceRow(
                        title = stringResource(R.string.persistent_notifications),
                        summary = stringResource(R.string.persistent_notifications_description),
                        checked = persistentEnabled,
                        onCheckedChange = onPersistent,
                    )
                }
                SettingsItemCard(position = cardPosition(i++, total)) {
                    SwitchPreferenceRow(
                        title = stringResource(R.string.wearable_notifications),
                        summary = stringResource(R.string.wearable_notifications_summary),
                        checked = wearableEnabled,
                        onCheckedChange = onWearable,
                    )
                }
            }
            SettingsItemCard(position = cardPosition(i++, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.bundle_notifications),
                    summary = stringResource(R.string.bundle_notifications_summary),
                    checked = bundleEnabled,
                    onCheckedChange = onBundle,
                )
            }
            SettingsItemCard(position = cardPosition(i, total)) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.EPr_voiceRemindersEnabled_title),
                    summary = stringResource(R.string.EPr_voiceRemindersEnabled_desc_enabled),
                    checked = voiceEnabled,
                    onCheckedChange = onVoice,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // More settings island
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            PreferenceRow(
                title = stringResource(R.string.more_settings),
                summary = stringResource(R.string.more_notification_settings_summary),
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = onMoreSettings,
            )
        }

        // All day tasks section
        SectionHeader(
            R.string.default_reminder,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.add_default_reminders),
                    summary = if (defaultRemindersEnabled)
                        stringResource(R.string.rmd_time_enabled_on)
                    else
                        stringResource(R.string.rmd_time_enabled_off),
                    checked = defaultRemindersEnabled,
                    onCheckedChange = onDefaultReminders,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.rmd_EPr_rmd_time_title),
                    summary = reminderTimeSummary,
                    onClick = onReminderTime,
                )
            }
        }

        // Swipe to snooze section
        SectionHeader(
            R.string.swipe_to_snooze_title,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    checked = swipeToSnoozeEnabled,
                    onCheckedChange = onSwipeToSnooze,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.swipe_to_snooze_description),
                    summary = snoozeSummary,
                    enabled = swipeToSnoozeEnabled,
                    onClick = onSnoozeTime,
                )
            }
        }

        // Quiet hours section
        SectionHeader(
            R.string.quiet_hours,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    summary = stringResource(R.string.quiet_hours_summary),
                    checked = quietHoursEnabled,
                    onCheckedChange = onQuietHours,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(R.string.rmd_EPr_quiet_hours_start_title),
                    summary = quietStartSummary,
                    enabled = quietHoursEnabled,
                    onClick = onQuietStart,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(R.string.rmd_EPr_quiet_hours_end_title),
                    summary = quietEndSummary,
                    enabled = quietHoursEnabled,
                    onClick = onQuietEnd,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
