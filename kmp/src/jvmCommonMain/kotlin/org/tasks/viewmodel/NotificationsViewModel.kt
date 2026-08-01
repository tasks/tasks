package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.PlatformConfiguration
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.NotificationSettings
import org.tasks.preferences.isCurrentlyQuietHours
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.plusDays
import org.tasks.time.withMillisOfDay

open class NotificationsViewModel(
    private val appPreferences: AppPreferences,
    platformConfiguration: PlatformConfiguration,
    private val persistenceScope: CoroutineScope,
) : ViewModel() {

    enum class TimePickerTarget {
        DEFAULT_REMINDER,
        QUIET_HOURS_START,
        QUIET_HOURS_END,
    }

    val showTroubleshooting: Boolean = platformConfiguration.supportsNotificationTroubleshooting
    val showSystemNotificationSettings: Boolean =
        platformConfiguration.supportsSystemNotificationSettings
    val showOngoingNotifications: Boolean = platformConfiguration.supportsOngoingNotifications
    val showBundleNotifications: Boolean = platformConfiguration.supportsBundledNotifications
    val showVoiceReminders: Boolean = platformConfiguration.supportsVoiceReminders
    val showCompletionSound: Boolean = platformConfiguration.supportsCompletionSound
    val showSwipeToSnooze: Boolean = platformConfiguration.supportsSwipeToSnooze

    var settings by mutableStateOf(NotificationSettings())
        private set

    var loaded by mutableStateOf(false)
        private set

    var timePickerInputMode by mutableStateOf(false)
        private set

    var isCurrentlyQuietHours by mutableStateOf(false)
        private set

    var showBatteryOptimization by mutableStateOf(false)
        protected set

    var completionSoundName by mutableStateOf("")
        protected set

    var showSnoozeDialog by mutableStateOf(false)
        private set

    var timePickerTarget by mutableStateOf<TimePickerTarget?>(null)
        private set

    private var quietHoursRefreshJob: Job? = null

    private var pendingWrite: Job? = null

    init {
        viewModelScope.launch {
            reload()
            scheduleQuietHoursRefresh()
        }
    }

    protected suspend fun reload() {
        settings = appPreferences.notificationSettings()
        timePickerInputMode = appPreferences.datePickerPreferences().timePickerInputMode
        isCurrentlyQuietHours = settings.isCurrentlyQuietHours()
        loaded = true
    }

    open fun refreshState() {
        viewModelScope.launch {
            while (true) {
                val write = pendingWrite ?: break
                write.join()
                if (pendingWrite === write) {
                    break
                }
            }
            reload()
            scheduleQuietHoursRefresh()
        }
    }

    private fun persist(block: suspend () -> Unit) {
        val previous = pendingWrite
        pendingWrite = persistenceScope.launch {
            previous?.join()
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to save notification settings" }
                try {
                    reload()
                } catch (reloadError: Exception) {
                    Logger.e(reloadError, tag = TAG) { "Failed to reload notification settings" }
                }
            }
        }
    }

    protected open fun rescheduleNotifications(cancelExisting: Boolean) {}

    fun updatePersistent(enabled: Boolean) {
        settings = settings.copy(
            persistentNotifications = enabled,
            wearableNotifications = if (enabled) false else settings.wearableNotifications,
        )
        persist {
            appPreferences.setPersistentNotifications(enabled)
            if (enabled) {
                appPreferences.setWearableNotifications(false)
            }
            rescheduleNotifications(false)
        }
    }

    fun updateWearable(enabled: Boolean) {
        settings = settings.copy(
            wearableNotifications = enabled,
            persistentNotifications = if (enabled) false else settings.persistentNotifications,
        )
        persist {
            appPreferences.setWearableNotifications(enabled)
            if (enabled) {
                appPreferences.setPersistentNotifications(false)
            }
            rescheduleNotifications(false)
        }
    }

    fun updateBundle(enabled: Boolean) {
        settings = settings.copy(bundleNotifications = enabled)
        persist {
            appPreferences.setBundleNotifications(enabled)
            rescheduleNotifications(true)
        }
    }

    open fun updateVoice(enabled: Boolean) {
        settings = settings.copy(voiceReminders = enabled)
        persist { appPreferences.setVoiceReminders(enabled) }
    }

    fun updateSwipeToSnooze(enabled: Boolean) {
        settings = settings.copy(swipeToSnoozeEnabled = enabled)
        persist { appPreferences.setSwipeToSnoozeEnabled(enabled) }
    }

    fun setSnoozeTime(minutes: Int) {
        settings = settings.copy(swipeToSnoozeMinutes = minutes)
        showSnoozeDialog = false
        persist { appPreferences.setSwipeToSnoozeMinutes(minutes) }
    }

    fun openSnoozeDialog() {
        showSnoozeDialog = true
    }

    fun dismissSnoozeDialog() {
        showSnoozeDialog = false
    }

    fun updateDefaultReminders(enabled: Boolean) {
        settings = settings.copy(defaultRemindersEnabled = enabled)
        persist { appPreferences.setDefaultRemindersEnabled(enabled) }
    }

    fun updateQuietHours(enabled: Boolean) {
        settings = settings.copy(quietHoursEnabled = enabled)
        isCurrentlyQuietHours = settings.isCurrentlyQuietHours()
        persist {
            appPreferences.setQuietHoursEnabled(enabled)
            rescheduleNotifications(false)
        }
        scheduleQuietHoursRefresh()
    }

    fun openTimePicker(target: TimePickerTarget) {
        timePickerTarget = target
    }

    fun dismissTimePicker() {
        timePickerTarget = null
    }

    fun timePickerInitialValue(target: TimePickerTarget): Int = when (target) {
        TimePickerTarget.DEFAULT_REMINDER -> settings.defaultReminderTime
        TimePickerTarget.QUIET_HOURS_START -> settings.quietHoursStart
        TimePickerTarget.QUIET_HOURS_END -> settings.quietHoursEnd
    }

    fun setTime(target: TimePickerTarget, millisOfDay: Int) {
        when (target) {
            TimePickerTarget.DEFAULT_REMINDER -> {
                settings = settings.copy(defaultReminderTime = millisOfDay)
                persist {
                    appPreferences.setDefaultReminderTime(millisOfDay)
                    rescheduleNotifications(false)
                }
            }
            TimePickerTarget.QUIET_HOURS_START -> {
                settings = settings.copy(quietHoursStart = millisOfDay)
                persist {
                    appPreferences.setQuietHoursStart(millisOfDay)
                    rescheduleNotifications(false)
                }
            }
            TimePickerTarget.QUIET_HOURS_END -> {
                settings = settings.copy(quietHoursEnd = millisOfDay)
                persist {
                    appPreferences.setQuietHoursEnd(millisOfDay)
                    rescheduleNotifications(false)
                }
            }
        }
        isCurrentlyQuietHours = settings.isCurrentlyQuietHours()
        scheduleQuietHoursRefresh()
    }

    fun updateTimePickerInputMode(inputMode: Boolean) {
        timePickerInputMode = inputMode
        persist { appPreferences.setTimePickerInputMode(inputMode) }
    }

    private fun scheduleQuietHoursRefresh() {
        quietHoursRefreshJob?.cancel()
        if (!settings.quietHoursEnabled) {
            return
        }
        quietHoursRefreshJob = viewModelScope.launch {
            while (true) {
                val now = currentTimeMillis()
                delay(settings.nextQuietHoursBoundary(now) - now + 1000)
                isCurrentlyQuietHours = settings.isCurrentlyQuietHours()
            }
        }
    }
}

private const val TAG = "NotificationsViewModel"

private fun NotificationSettings.nextQuietHoursBoundary(now: Long): Long =
    listOf(quietHoursStart, quietHoursEnd)
        .map { now.withMillisOfDay(it) }
        .map { if (it <= now) it.plusDays(1) else it }
        .min()
