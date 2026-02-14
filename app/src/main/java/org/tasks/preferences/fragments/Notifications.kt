package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.compose.settings.NotificationsScreen
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.extensions.Context.openChannelNotificationSettings
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import org.tasks.time.withMillisOfDay
import javax.inject.Inject

@AndroidEntryPoint
class Notifications : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: NotificationsViewModel by viewModels()

    private val listPickerLauncher = registerForFilterPickerResult {
        viewModel.setBadgeFilter(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            NotificationsScreen(
                showBatteryOptimization = viewModel.showBatteryOptimization,
                completionSoundName = viewModel.completionSoundName,
                showPreUpsideDownCake = AndroidUtilities.preUpsideDownCake(),
                persistentEnabled = viewModel.persistentEnabled,
                wearableEnabled = viewModel.wearableEnabled,
                bundleEnabled = viewModel.bundleEnabled,
                voiceEnabled = viewModel.voiceEnabled,
                swipeToSnoozeEnabled = viewModel.swipeToSnoozeEnabled,
                snoozeSummary = viewModel.snoozeSummary,
                defaultRemindersEnabled = viewModel.defaultRemindersEnabled,
                reminderTimeSummary = viewModel.reminderTimeSummary,
                badgesEnabled = viewModel.badgesEnabled,
                badgeFilterName = viewModel.badgeFilterName,
                quietHoursEnabled = viewModel.quietHoursEnabled,
                isCurrentlyQuietHours = viewModel.isCurrentlyQuietHours,
                quietStartSummary = viewModel.quietStartSummary,
                quietEndSummary = viewModel.quietEndSummary,
                onTroubleshooting = {
                    context?.openUri(R.string.url_notifications)
                },
                onBatteryOptimization = {
                    startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                },
                onCompletionSound = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_TYPE,
                            RingtoneManager.TYPE_NOTIFICATION
                        )
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            Settings.System.DEFAULT_NOTIFICATION_URI
                        )
                        val existingValue = viewModel.getCompletionRingtoneValue()
                        if (existingValue != null) {
                            if (existingValue.isEmpty()) {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    null as Uri?
                                )
                            } else {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    Uri.parse(existingValue)
                                )
                            }
                        } else {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                Settings.System.DEFAULT_NOTIFICATION_URI
                            )
                        }
                    }
                    startActivityForResult(intent, REQUEST_CODE_COMPLETION_SOUND)
                },
                onPersistent = { viewModel.updatePersistent(it) },
                onWearable = { viewModel.updateWearable(it) },
                onBundle = { viewModel.updateBundle(it) },
                onVoice = { enabled ->
                    viewModel.updateVoice(enabled)
                    if (enabled && viewModel.needsTtsCheck) {
                        try {
                            val checkIntent = Intent().apply {
                                action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
                            }
                            startActivityForResult(checkIntent, REQUEST_CODE_TTS_CHECK)
                        } catch (_: Exception) { }
                    }
                },
                onMoreSettings = {
                    requireContext().openChannelNotificationSettings()
                },
                onSwipeToSnooze = { viewModel.updateSwipeToSnooze(it) },
                onSnoozeTime = { viewModel.openSnoozeDialog() },
                onDefaultReminders = { viewModel.updateDefaultReminders(it) },
                onReminderTime = {
                    val millisOfDay = viewModel.getReminderTimeMillisOfDay(
                        R.integer.default_remind_time
                    )
                    val current = DateTime().withMillisOfDay(millisOfDay)
                    newTimePicker(this@Notifications, REQUEST_DEFAULT_REMIND, current.millis)
                        .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
                },
                onBadges = { viewModel.updateBadges(it) },
                onBadgeList = {
                    lifecycleScope.launch {
                        listPickerLauncher.launch(
                            context = requireContext(),
                            selectedFilter = viewModel.getBadgeFilter(),
                        )
                    }
                },
                onQuietHours = { viewModel.updateQuietHours(it) },
                onQuietStart = {
                    val millisOfDay = viewModel.getQuietStartMillisOfDay(
                        R.integer.default_quiet_hours_start
                    )
                    val current = DateTime().withMillisOfDay(millisOfDay)
                    newTimePicker(this@Notifications, REQUEST_QUIET_START, current.millis)
                        .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
                },
                onQuietEnd = {
                    val millisOfDay = viewModel.getQuietEndMillisOfDay(
                        R.integer.default_quiet_hours_end
                    )
                    val current = DateTime().withMillisOfDay(millisOfDay)
                    newTimePicker(this@Notifications, REQUEST_QUIET_END, current.millis)
                        .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
                },
            )

            if (viewModel.showSnoozeDialog) {
                val currentValue = viewModel.getSnoozeCurrentValue()
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSnoozeDialog() },
                    title = { Text(stringResource(R.string.swipe_to_snooze_description)) },
                    text = {
                        Column {
                            viewModel.snoozeEntries.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSnoozeTime(viewModel.snoozeValues[index])
                                            viewModel.refreshSnoozeSummary()
                                            viewModel.dismissSnoozeDialog()
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = viewModel.snoozeValues[index] == currentValue,
                                        onClick = null,
                                    )
                                    Text(
                                        text = entry,
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

            if (viewModel.showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRestartDialog() },
                    text = { Text(stringResource(R.string.restart_required)) },
                    confirmButton = {
                        TextButton(onClick = { kotlin.system.exitProcess(0) }) {
                            Text(stringResource(R.string.restart_now))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissRestartDialog() }) {
                            Text(stringResource(R.string.restart_later))
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_COMPLETION_SOUND -> {
                viewModel.handleCompletionSoundResult(resultCode, data)
            }
            REQUEST_QUIET_START -> if (resultCode == RESULT_OK && data != null) {
                viewModel.handleQuietStartResult(data)
            }
            REQUEST_QUIET_END -> if (resultCode == RESULT_OK && data != null) {
                viewModel.handleQuietEndResult(data)
            }
            REQUEST_DEFAULT_REMIND -> if (resultCode == RESULT_OK && data != null) {
                viewModel.handleDefaultRemindResult(data)
            }
            REQUEST_CODE_TTS_CHECK -> {
                viewModel.handleTtsCheckResult(resultCode)
                if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    val installIntent = Intent().apply {
                        action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    }
                    startActivity(installIntent)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"
        private const val REQUEST_QUIET_START = 10001
        private const val REQUEST_QUIET_END = 10002
        private const val REQUEST_DEFAULT_REMIND = 10003
        private const val REQUEST_CODE_TTS_CHECK = 10006
        private const val REQUEST_CODE_COMPLETION_SOUND = 10007
    }
}
