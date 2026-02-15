package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.voice.VoiceOutputAssistant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.extensions.Context.getResourceUri
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.filters.Filter
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.receivers.ShortcutBadger
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val voiceOutputAssistant: VoiceOutputAssistant,
) : ViewModel() {

    var showBatteryOptimization by mutableStateOf(false)
        private set
    var completionSoundName by mutableStateOf("")
        private set
    var persistentEnabled by mutableStateOf(false)
        private set
    var wearableEnabled by mutableStateOf(false)
        private set
    var bundleEnabled by mutableStateOf(false)
        private set
    var voiceEnabled by mutableStateOf(false)
        private set
    var swipeToSnoozeEnabled by mutableStateOf(false)
        private set
    var snoozeSummary by mutableStateOf("")
        private set
    var defaultRemindersEnabled by mutableStateOf(false)
        private set
    var reminderTimeSummary by mutableStateOf("")
        private set
    var badgesEnabled by mutableStateOf(false)
        private set
    var badgeFilterName by mutableStateOf("")
        private set
    var quietHoursEnabled by mutableStateOf(false)
        private set
    var isCurrentlyQuietHours by mutableStateOf(false)
        private set
    var quietStartSummary by mutableStateOf("")
        private set
    var quietEndSummary by mutableStateOf("")
        private set
    var showSnoozeDialog by mutableStateOf(false)
        private set
    var showRestartDialog by mutableStateOf(false)
        private set

    private var quietHoursRefreshJob: Job? = null

    val snoozeEntries: Array<String> = context.resources.getStringArray(R.array.swipe_to_snooze_times)
    val snoozeValues: Array<String> = context.resources.getStringArray(R.array.swipe_to_snooze_time_values)
    private val defaultBundleNotifications: Boolean = context.resources.getBoolean(R.bool.default_bundle_notifications)

    init {
        refreshState()
    }

    fun refreshState() {
        checkBatteryOptimizations()
        refreshCompletionSoundName()
        persistentEnabled = preferences.getBoolean(R.string.p_rmd_persistent, true)
        wearableEnabled = preferences.getBoolean(R.string.p_wearable_notifications, true)
        bundleEnabled = preferences.getBoolean(R.string.p_bundle_notifications, defaultBundleNotifications)
        voiceEnabled = preferences.getBoolean(R.string.p_voiceRemindersEnabled, false)
        swipeToSnoozeEnabled = preferences.getBoolean(R.string.p_rmd_swipe_to_snooze_enabled, false)
        refreshSnoozeSummary()
        defaultRemindersEnabled = preferences.getBoolean(R.string.p_rmd_time_enabled, true)
        refreshTimeSummary(R.string.p_rmd_time, R.integer.default_remind_time) { reminderTimeSummary = it }
        badgesEnabled = preferences.getBoolean(R.string.p_badges_enabled, false)
        viewModelScope.launch {
            val filter = defaultFilterProvider.getBadgeFilter()
            badgeFilterName = filter.title ?: ""
        }
        quietHoursEnabled = preferences.getBoolean(R.string.p_rmd_enable_quiet, false)
        isCurrentlyQuietHours = preferences.isCurrentlyQuietHours
        refreshTimeSummary(R.string.p_rmd_quietStart, R.integer.default_quiet_hours_start) { quietStartSummary = it }
        refreshTimeSummary(R.string.p_rmd_quietEnd, R.integer.default_quiet_hours_end) { quietEndSummary = it }
        scheduleQuietHoursRefresh()
    }

    private fun checkBatteryOptimizations() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        showBatteryOptimization =
            !powerManager.isIgnoringBatteryOptimizations(context.getString(R.string.app_package))
    }

    private fun refreshCompletionSoundName() {
        val value = preferences.getStringValue(R.string.p_completion_ringtone)
        val defaultUri = context.getResourceUri(R.raw.long_rising_tone)
        completionSoundName = when {
            value == "" -> context.getString(R.string.none)
            value != null -> {
                val uri = value.toUri()
                if (uri == defaultUri) {
                    context.getString(R.string.settings_default)
                } else {
                    try {
                        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: value
                    } catch (e: SecurityException) {
                        value
                    }
                }
            }
            else -> context.getString(R.string.settings_default)
        }
    }

    fun refreshSnoozeSummary() {
        val currentValue = preferences.getIntegerFromString(
            R.string.p_rmd_swipe_to_snooze_time_minutes, 15
        ).toString()
        val index = snoozeValues.indexOf(currentValue).coerceAtLeast(0)
        snoozeSummary = context.getString(R.string.swipe_to_snooze_time_description, snoozeEntries[index])
    }

    private fun refreshTimeSummary(prefKey: Int, defaultRes: Int, setter: (String) -> Unit) {
        val millisOfDay = preferences.getInt(prefKey, context.resources.getInteger(defaultRes))
        val timeString = getTimeString(
            currentTimeMillis().withMillisOfDay(millisOfDay),
            context.is24HourFormat
        )
        setter(timeString)
    }

    private fun rescheduleNotifications(cancelExisting: Boolean) {
        NotificationSchedulerIntentService.enqueueWork(context, cancelExisting)
    }

    fun updatePersistent(enabled: Boolean) {
        preferences.setBoolean(R.string.p_rmd_persistent, enabled)
        persistentEnabled = enabled
        if (enabled) {
            preferences.setBoolean(R.string.p_wearable_notifications, false)
            wearableEnabled = false
        }
        rescheduleNotifications(false)
    }

    fun updateWearable(enabled: Boolean) {
        preferences.setBoolean(R.string.p_wearable_notifications, enabled)
        wearableEnabled = enabled
        if (enabled) {
            preferences.setBoolean(R.string.p_rmd_persistent, false)
            persistentEnabled = false
        }
        rescheduleNotifications(false)
    }

    fun updateBundle(enabled: Boolean) {
        preferences.setBoolean(R.string.p_bundle_notifications, enabled)
        bundleEnabled = enabled
        rescheduleNotifications(true)
    }

    fun updateVoice(enabled: Boolean) {
        try {
            if (enabled && !voiceOutputAssistant.isTTSInitialized) {
                // Fragment will handle launching TTS check intent
            } else if (!enabled && voiceOutputAssistant.isTTSInitialized) {
                voiceOutputAssistant.shutdown()
            }
        } catch (e: VerifyError) {
            Timber.e(e)
            preferences.setBoolean(R.string.p_voiceRemindersEnabled, false)
            voiceEnabled = false
            return
        }
        preferences.setBoolean(R.string.p_voiceRemindersEnabled, enabled)
        voiceEnabled = enabled
    }

    val needsTtsCheck: Boolean
        get() = !voiceOutputAssistant.isTTSInitialized

    fun initTts() {
        voiceOutputAssistant.initTTS()
    }

    fun updateSwipeToSnooze(enabled: Boolean) {
        preferences.setBoolean(R.string.p_rmd_swipe_to_snooze_enabled, enabled)
        swipeToSnoozeEnabled = enabled
    }

    fun setSnoozeTime(value: String) {
        preferences.setString(R.string.p_rmd_swipe_to_snooze_time_minutes, value)
    }

    fun getSnoozeCurrentValue(): String {
        return preferences.getIntegerFromString(
            R.string.p_rmd_swipe_to_snooze_time_minutes, 15
        ).toString()
    }

    fun openSnoozeDialog() {
        showSnoozeDialog = true
    }

    fun dismissSnoozeDialog() {
        showSnoozeDialog = false
    }

    fun dismissRestartDialog() {
        showRestartDialog = false
    }

    fun updateDefaultReminders(enabled: Boolean) {
        preferences.setBoolean(R.string.p_rmd_time_enabled, enabled)
        defaultRemindersEnabled = enabled
    }

    fun getReminderTimeMillisOfDay(defaultRes: Int): Int {
        return preferences.getInt(R.string.p_rmd_time, context.resources.getInteger(defaultRes))
    }

    fun updateBadges(enabled: Boolean) {
        preferences.setBoolean(R.string.p_badges_enabled, enabled)
        badgesEnabled = enabled
        if (enabled) {
            showRestartDialog = true
        } else {
            ShortcutBadger.removeCount(context)
        }
    }

    fun setBadgeFilter(filter: Filter) {
        defaultFilterProvider.setBadgeFilter(filter)
        badgeFilterName = filter.title ?: ""
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun getBadgeFilter() = defaultFilterProvider.getBadgeFilter()

    fun updateQuietHours(enabled: Boolean) {
        preferences.setBoolean(R.string.p_rmd_enable_quiet, enabled)
        quietHoursEnabled = enabled
        isCurrentlyQuietHours = preferences.isCurrentlyQuietHours
        rescheduleNotifications(false)
        scheduleQuietHoursRefresh()
    }

    fun getQuietStartMillisOfDay(defaultRes: Int): Int {
        return preferences.getInt(R.string.p_rmd_quietStart, context.resources.getInteger(defaultRes))
    }

    fun getQuietEndMillisOfDay(defaultRes: Int): Int {
        return preferences.getInt(R.string.p_rmd_quietEnd, context.resources.getInteger(defaultRes))
    }

    fun handleCompletionSoundResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val ringtone: android.net.Uri? =
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (ringtone != null) {
                preferences.setString(R.string.p_completion_ringtone, ringtone.toString())
            } else {
                preferences.setString(R.string.p_completion_ringtone, "")
            }
            refreshCompletionSoundName()
        }
    }

    fun handleTimePickerResult(
        timestamp: Long,
        prefKey: Int,
        defaultRes: Int,
        setter: (String) -> Unit,
    ) {
        val millisOfDay = DateTime(timestamp).millisOfDay
        preferences.setInt(prefKey, millisOfDay)
        refreshTimeSummary(prefKey, defaultRes, setter)
    }

    fun handleQuietStartResult(timestamp: Long) {
        handleTimePickerResult(
            timestamp, R.string.p_rmd_quietStart, R.integer.default_quiet_hours_start
        ) { quietStartSummary = it }
        isCurrentlyQuietHours = preferences.isCurrentlyQuietHours
        rescheduleNotifications(false)
        scheduleQuietHoursRefresh()
    }

    fun handleQuietEndResult(timestamp: Long) {
        handleTimePickerResult(
            timestamp, R.string.p_rmd_quietEnd, R.integer.default_quiet_hours_end
        ) { quietEndSummary = it }
        isCurrentlyQuietHours = preferences.isCurrentlyQuietHours
        rescheduleNotifications(false)
        scheduleQuietHoursRefresh()
    }

    fun handleDefaultRemindResult(timestamp: Long) {
        handleTimePickerResult(
            timestamp, R.string.p_rmd_time, R.integer.default_remind_time
        ) { reminderTimeSummary = it }
        rescheduleNotifications(false)
    }

    fun handleTtsCheckResult(resultCode: Int) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            voiceOutputAssistant.initTTS()
        }
    }

    fun getCompletionRingtoneValue(): String? {
        return preferences.getStringValue(R.string.p_completion_ringtone)
    }

    private fun scheduleQuietHoursRefresh() {
        quietHoursRefreshJob?.cancel()
        if (!isCurrentlyQuietHours) return
        val endMillis = preferences.getInt(
            R.string.p_rmd_quietEnd,
            context.resources.getInteger(R.integer.default_quiet_hours_end)
        )
        quietHoursRefreshJob = viewModelScope.launch {
            var endTime = DateTime().withMillisOfDay(endMillis)
            if (endTime.isBeforeNow) {
                endTime = endTime.plusDays(1)
            }
            val delayMs = endTime.millis - currentTimeMillis() + 1000
            if (delayMs > 0) {
                delay(delayMs)
            }
            isCurrentlyQuietHours = preferences.isCurrentlyQuietHours
            scheduleQuietHoursRefresh()
        }
    }

    override fun onCleared() {
        super.onCleared()
        quietHoursRefreshJob?.cancel()
        voiceOutputAssistant.shutdown()
    }
}
