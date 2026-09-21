package org.tasks.preferences.fragments

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.compose.resources.stringResource
import org.tasks.R
import org.tasks.compose.settings.NotificationsContent
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.Context.openChannelNotificationSettings
import org.tasks.extensions.Context.openUri
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import timber.log.Timber
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.more_notification_settings_summary
import javax.inject.Inject

@AndroidEntryPoint
class Notifications : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: NotificationsHiltViewModel by viewModels()

    private val completionSoundLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        viewModel.handleCompletionSoundResult(result.resultCode, result.data)
    }

    private val ttsCheckLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        viewModel.handleTtsCheckResult(result.resultCode)
        if (result.resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            try {
                startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
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
            LaunchedEffect(Unit) {
                viewModel.ttsCheckRequests.collect {
                    try {
                        ttsCheckLauncher.launch(Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA))
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }

            NotificationsContent(
                viewModel = viewModel,
                is24HourFormat = requireContext().is24HourFormat,
                openUri = { context?.openUri(it) },
                onBatteryOptimization = {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
                onCompletionSound = { launchRingtonePicker() },
                onMoreSettings = { requireContext().openChannelNotificationSettings() },
                moreSettingsSummary = stringResource(Res.string.more_notification_settings_summary),
                bottomInsets = {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                },
            )
        }
    }

    private fun launchRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI
            )
            when (val existingValue = viewModel.getCompletionRingtoneValue()) {
                null -> putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )
                "" -> putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                else -> putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(existingValue)
                )
            }
        }
        completionSoundLauncher.launch(intent)
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
}
