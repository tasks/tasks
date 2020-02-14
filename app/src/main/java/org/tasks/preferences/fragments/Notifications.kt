package org.tasks.preferences.fragments

import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.preference.Preference
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.voice.VoiceOutputAssistant
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.activities.TimePickerActivity
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.receivers.ShortcutBadger
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.time.DateTime
import org.tasks.ui.TimePreference
import timber.log.Timber
import javax.inject.Inject

private const val REQUEST_QUIET_START = 10001
private const val REQUEST_QUIET_END = 10002
private const val REQUEST_DEFAULT_REMIND = 10003
private const val REQUEST_BADGE_LIST = 10004
private const val REQUEST_CODE_ALERT_RINGTONE = 10005
private const val REQUEST_CODE_TTS_CHECK = 10006

class Notifications : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var voiceOutputAssistant: VoiceOutputAssistant

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_notifications, rootKey)

        rescheduleNotificationsOnChange(
                R.string.p_rmd_time,
                R.string.p_rmd_enable_quiet,
                R.string.p_rmd_quietStart,
                R.string.p_rmd_quietEnd,
                R.string.p_rmd_persistent)

        initializeRingtonePreference()
        initializeTimePreference(getDefaultRemindTimePreference()!!, REQUEST_DEFAULT_REMIND)
        initializeTimePreference(getQuietStartPreference()!!, REQUEST_QUIET_START)
        initializeTimePreference(getQuietEndPreference()!!, REQUEST_QUIET_END)

        findPreference(R.string.notification_channel_settings)
                .setOnPreferenceClickListener(::openNotificationChannelSettings)
        findPreference(R.string.battery_optimization_settings)
                .setOnPreferenceClickListener(::openBatteryOptimizationSettings)

        findPreference(R.string.p_bundle_notifications)
                .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                    NotificationSchedulerIntentService.enqueueWork(context, true)
                    true
                }

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
        val filter = defaultFilterProvider.badgeFilter
        badgePreference.summary = filter.listingTitle
        badgePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(context, FilterSelectionActivity::class.java)
            intent.putExtra(
                    FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.badgeFilter)
            intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true)
            startActivityForResult(intent, REQUEST_BADGE_LIST)
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

        requires(AndroidUtilities.atLeastOreo(), R.string.notification_channel_settings)
        requires(AndroidUtilities.atLeastMarshmallow(), R.string.battery_optimization_settings)
        requires(
                AndroidUtilities.preOreo(), R.string.p_rmd_ringtone, R.string.p_rmd_vibrate, R.string.p_led_notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        voiceOutputAssistant.shutdown()
    }

    override fun inject(component: FragmentComponent) {
        component.inject(this)
    }

    private fun rescheduleNotificationsOnChange(vararg resIds: Int) {
        for (resId in resIds) {
            findPreference(resId)
                    .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                        NotificationSchedulerIntentService.enqueueWork(context, false)
                        true
                    }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return if (preference!!.key == getString(R.string.p_rmd_ringtone)) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            val existingValue: String? = preferences.getStringValue(R.string.p_rmd_ringtone)
            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
                }
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }
            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
            true
        } else {
            super.onPreferenceTreeClick(preference)
        }
    }

    private fun getQuietStartPreference(): TimePreference? {
        return getTimePreference(R.string.p_rmd_quietStart)
    }

    private fun getQuietEndPreference(): TimePreference? {
        return getTimePreference(R.string.p_rmd_quietEnd)
    }

    private fun getDefaultRemindTimePreference(): TimePreference? {
        return getTimePreference(R.string.p_rmd_time)
    }

    private fun getTimePreference(resId: Int): TimePreference? {
        return findPreference(getString(resId)) as TimePreference?
    }

    private fun initializeTimePreference(preference: TimePreference, requestCode: Int) {
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val current = DateTime().withMillisOfDay(preference.millisOfDay)
            val intent = Intent(context, TimePickerActivity::class.java)
            intent.putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.millis)
            startActivityForResult(intent, requestCode)
            true
        }
    }

    private fun initializeRingtonePreference() {
        val ringtoneChangedListener = Preference.OnPreferenceChangeListener { preference: Preference, value: Any? ->
            if ("" == value) {
                preference.setSummary(R.string.silent)
            } else {
                val ringtone = RingtoneManager.getRingtone(
                        context,
                        if (value == null) Settings.System.DEFAULT_NOTIFICATION_URI else Uri.parse(value as String?))
                preference.summary = if (ringtone == null) "" else ringtone.getTitle(context)
            }
            true
        }
        val ringtoneKey = R.string.p_rmd_ringtone
        val ringtonePreference: Preference = findPreference(ringtoneKey)
        ringtonePreference.onPreferenceChangeListener = ringtoneChangedListener
        ringtoneChangedListener.onPreferenceChange(
                ringtonePreference,
                preferences.getStringValue(ringtoneKey))
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun openNotificationChannelSettings(@Suppress("UNUSED_PARAMETER") ignored: Preference): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity!!.packageName)
        startActivity(intent)
        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun openBatteryOptimizationSettings(@Suppress("UNUSED_PARAMETER") ignored: Preference): Boolean {
        val intent = Intent()
        intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        startActivity(intent)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE) {
            if (resultCode == RESULT_OK && data != null) {
                val ringtone: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (ringtone != null) {
                    preferences.setString(R.string.p_rmd_ringtone, ringtone.toString())
                } else {
                    preferences.setString(R.string.p_rmd_ringtone, "")
                }
                initializeRingtonePreference()
            }
        } else if (requestCode == REQUEST_QUIET_START) {
            if (resultCode == RESULT_OK) {
                getQuietStartPreference()!!.handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_QUIET_END) {
            if (resultCode == RESULT_OK) {
                getQuietEndPreference()!!.handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_DEFAULT_REMIND) {
            if (resultCode == RESULT_OK) {
                getDefaultRemindTimePreference()!!.handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_BADGE_LIST) {
            if (resultCode == RESULT_OK) {
                val filter: Filter = data!!.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER)!!
                defaultFilterProvider.badgeFilter = filter
                findPreference(R.string.p_badge_list).summary = filter.listingTitle
                localBroadcastManager.broadcastRefresh()
            }
        } else if (requestCode == REQUEST_CODE_TTS_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) { // success, create the TTS instance
                voiceOutputAssistant.initTTS()
            } else { // missing data, install it
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installIntent)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}