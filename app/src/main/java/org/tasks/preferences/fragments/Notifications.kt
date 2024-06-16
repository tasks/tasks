package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.voice.VoiceOutputAssistant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.extensions.Context.getResourceUri
import org.tasks.extensions.Context.openChannelNotificationSettings
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.receivers.ShortcutBadger
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.time.DateTime
import org.tasks.ui.TimePreference
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Notifications : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var voiceOutputAssistant: VoiceOutputAssistant

    private val listPickerLauncher = registerForListPickerResult {
        defaultFilterProvider.setBadgeFilter(it)
        findPreference(R.string.p_badge_list).summary = it.title
        localBroadcastManager.broadcastRefresh()
    }

    override fun getPreferenceXml() = R.xml.preferences_notifications

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        rescheduleNotificationsOnChange(
            false,
            R.string.p_rmd_time_enabled,
            R.string.p_rmd_time,
            R.string.p_rmd_enable_quiet,
            R.string.p_rmd_quietStart,
            R.string.p_rmd_quietEnd
        )
        rescheduleNotificationsOnChange(true, R.string.p_bundle_notifications)

        initializeRingtonePreference()
        initializeCompletionSoundPreference()
        initializeTimePreference(getDefaultRemindTimePreference()!!, REQUEST_DEFAULT_REMIND)
        initializeTimePreference(getQuietStartPreference()!!, REQUEST_QUIET_START)
        initializeTimePreference(getQuietEndPreference()!!, REQUEST_QUIET_END)

        findPreference(R.string.p_badges_enabled)
            .setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                if (newValue != null) {
                    if (newValue as Boolean) {
                        showRestartDialog()
                    } else {
                        ShortcutBadger.removeCount(context)
                    }
                    true
                } else {
                    false
                }
            }

        val badgePreference: Preference = findPreference(R.string.p_badge_list)
        val filter = defaultFilterProvider.getBadgeFilter()
        badgePreference.summary = filter.title
        badgePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launch {
                listPickerLauncher.launch(
                    context = requireContext(),
                    selectedFilter = defaultFilterProvider.getBadgeFilter(),
                )
            }
            true
        }

        findPreference(R.string.p_voiceRemindersEnabled)
            .setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
                val enabled = newValue as Boolean
                try {
                    if (enabled && !voiceOutputAssistant.isTTSInitialized) {
                        val checkIntent = Intent()
                        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
                        startActivityForResult(checkIntent, REQUEST_CODE_TTS_CHECK)
                    } else if (!enabled && voiceOutputAssistant.isTTSInitialized) {
                        voiceOutputAssistant.shutdown()
                    }
                } catch (e: VerifyError) {
                    Timber.e(e)
                    preference.isEnabled = false
                    preferences.setBoolean(preference.key, false)
                }
                true
            }

        findPreference(R.string.more_settings).setOnPreferenceClickListener {
            requireContext().openChannelNotificationSettings()
            true
        }

        val persistentReminders =
            findPreference(R.string.p_rmd_persistent) as SwitchPreferenceCompat
        val wearableReminders =
            findPreference(R.string.p_wearable_notifications) as SwitchPreferenceCompat
        if (persistentReminders.isChecked) {
            wearableReminders.isChecked = false
        }
        persistentReminders.setOnPreferenceChangeListener { _, newValue ->
            wearableReminders.isChecked = !(newValue as Boolean)
            rescheduleNotifications(false)
        }
        wearableReminders.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                persistentReminders.isChecked = false
            }
            rescheduleNotifications(false)
        }

        checkBatteryOptimizations()

        openUrl(R.string.troubleshooting, R.string.url_notifications)

        requires(AndroidUtilities.atLeastOreo(), R.string.more_settings)
        requires(
            AndroidUtilities.preOreo(),
            R.string.p_rmd_ringtone,
            R.string.p_rmd_vibrate,
            R.string.p_led_notification
        )
        requires(
            AndroidUtilities.preUpsideDownCake(),
            R.string.p_rmd_persistent,
            R.string.p_wearable_notifications,
        )
    }

    override fun onResume() {
        super.onResume()

        checkBatteryOptimizations()
    }

    private fun checkBatteryOptimizations() {
        val powerManager = requireContext().getSystemService(POWER_SERVICE) as PowerManager
        findPreference(R.string.disable_battery_optimizations).isVisible =
            !powerManager.isIgnoringBatteryOptimizations(getString(R.string.app_package))
    }

    override fun onDestroy() {
        super.onDestroy()

        voiceOutputAssistant.shutdown()
    }

    private fun rescheduleNotificationsOnChange(cancelExisting: Boolean, vararg resIds: Int) {
        for (resId in resIds) {
            findPreference(resId)
                .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                    rescheduleNotifications(cancelExisting)
                }
        }
    }

    private fun rescheduleNotifications(cancelExisting: Boolean): Boolean {
        NotificationSchedulerIntentService.enqueueWork(context, cancelExisting)
        return true
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean =
        when (preference.key) {
            getString(R.string.p_rmd_ringtone),
            getString(R.string.p_completion_ringtone) -> {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                    RingtoneManager.TYPE_NOTIFICATION
                )
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                intent.putExtra(
                    RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    Settings.System.DEFAULT_NOTIFICATION_URI
                )
                val existingValue: String? = preferences.getStringValue(preference.key)
                if (existingValue != null) {
                    if (existingValue.isEmpty()) {
                        intent.putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            null as Uri?
                        )
                    } else {
                        intent.putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(existingValue)
                        )
                    }
                } else {
                    intent.putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        Settings.System.DEFAULT_NOTIFICATION_URI
                    )
                }
                startActivityForResult(
                    intent,
                    if (preference.key == getString(R.string.p_rmd_ringtone)) {
                        REQUEST_CODE_ALERT_RINGTONE
                    } else {
                        REQUEST_CODE_COMPLETION_SOUND
                    }
                )
                true
            }
            else -> {
                super.onPreferenceTreeClick(preference)
            }
        }

    private fun getQuietStartPreference(): TimePreference? =
            getTimePreference(R.string.p_rmd_quietStart)

    private fun getQuietEndPreference(): TimePreference? =
            getTimePreference(R.string.p_rmd_quietEnd)

    private fun getDefaultRemindTimePreference(): TimePreference? =
            getTimePreference(R.string.p_rmd_time)

    private fun getTimePreference(resId: Int): TimePreference? =
            findPreference(getString(resId)) as TimePreference?

    private fun initializeTimePreference(preference: TimePreference, requestCode: Int) {
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val current = DateTime().withMillisOfDay(preference.millisOfDay)
            newTimePicker(this, requestCode, current.millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
            false
        }
    }

    private fun initializeRingtonePreference() =
        initializeRingtonePreference(
            R.string.p_rmd_ringtone,
            R.string.silent,
        )

    private fun initializeCompletionSoundPreference() =
        initializeRingtonePreference(
            R.string.p_completion_ringtone,
            R.string.none,
            requireContext().getResourceUri(R.raw.long_rising_tone)
        )

    private fun initializeRingtonePreference(pref: Int, noneRes: Int, default: Uri? = null) {
        val ringtoneChangedListener =
            Preference.OnPreferenceChangeListener { preference: Preference, value: Any? ->
                if ("" == value) {
                    preference.setSummary(noneRes)
                } else {
                    val uri =
                        (value as? String?)?.toUri()
                            ?: default
                            ?: Settings.System.DEFAULT_RINGTONE_URI
                    preference.summary = if (uri == default) {
                        getString(R.string.settings_default)
                    } else {
                        try {
                            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                        } catch (e: SecurityException) {
                            uri.toString()
                        }
                    }
                }
                true
            }
        val ringtonePreference = findPreference(pref)
        ringtonePreference.onPreferenceChangeListener = ringtoneChangedListener
        ringtoneChangedListener
            .onPreferenceChange(ringtonePreference, preferences.getStringValue(pref))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_ALERT_RINGTONE -> if (resultCode == RESULT_OK && data != null) {
                val ringtone: Uri? =
                        data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (ringtone != null) {
                    preferences.setString(R.string.p_rmd_ringtone, ringtone.toString())
                } else {
                    preferences.setString(R.string.p_rmd_ringtone, "")
                }
                initializeRingtonePreference()
            }
            REQUEST_CODE_COMPLETION_SOUND -> if (resultCode == RESULT_OK && data != null) {
                val ringtone: Uri? =
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (ringtone != null) {
                    preferences.setString(R.string.p_completion_ringtone, ringtone.toString())
                } else {
                    preferences.setString(R.string.p_completion_ringtone, "")
                }
                initializeCompletionSoundPreference()
            }
            REQUEST_QUIET_START -> if (resultCode == RESULT_OK) {
                getQuietStartPreference()!!.handleTimePickerActivityIntent(data)
            }
            REQUEST_QUIET_END -> if (resultCode == RESULT_OK) {
                getQuietEndPreference()!!.handleTimePickerActivityIntent(data)
            }
            REQUEST_DEFAULT_REMIND -> if (resultCode == RESULT_OK) {
                getDefaultRemindTimePreference()!!.handleTimePickerActivityIntent(data)
            }
            REQUEST_CODE_TTS_CHECK -> if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) { // success, create the TTS instance
                voiceOutputAssistant.initTTS()
            } else { // missing data, install it
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installIntent)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val REQUEST_QUIET_START = 10001
        private const val REQUEST_QUIET_END = 10002
        private const val REQUEST_DEFAULT_REMIND = 10003
        private const val REQUEST_CODE_ALERT_RINGTONE = 10005
        private const val REQUEST_CODE_TTS_CHECK = 10006
        private const val REQUEST_CODE_COMPLETION_SOUND = 10007
    }
}