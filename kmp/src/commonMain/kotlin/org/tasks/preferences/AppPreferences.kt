package org.tasks.preferences

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.tasks.compose.pickers.DEFAULT_AFTERNOON
import org.tasks.compose.pickers.DEFAULT_EVENING
import org.tasks.compose.pickers.DEFAULT_MORNING
import org.tasks.compose.pickers.DEFAULT_NIGHT
import org.tasks.compose.pickers.QuickPickTimes
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.plusDays
import org.tasks.time.withMillisOfDay

const val DEFAULT_REMINDER_TIME = 18 * 60 * 60 * 1000
const val DEFAULT_QUIET_HOURS_START = 22 * 60 * 60 * 1000
const val DEFAULT_QUIET_HOURS_END = 10 * 60 * 60 * 1000
const val DEFAULT_SNOOZE_MINUTES = 15
const val DEFAULT_LOCATION_UPDATE_INTERVAL = 15

val DEFAULT_ALARMS: List<Alarm> = listOf(
    Alarm.whenStarted(0),
    Alarm.whenDue(0),
    Alarm.whenOverdue(0),
)

val DEFAULT_ALARMS_JSON: Set<String> = DEFAULT_ALARMS.toAlarmJson()

val alarmOrder: Comparator<Alarm> = compareBy({ it.type }, { it.time })

fun List<Alarm>.toAlarmJson(): Set<String> = map { Json.encodeToString(it) }.toSet()

fun Set<String>.toAlarms(): List<Alarm> =
    mapNotNull {
        try {
            Json.decodeFromString<Alarm>(it)
        } catch (e: Exception) {
            Logger.e(e, tag = "AppPreferences") { "Failed to decode alarm: $it" }
            null
        }
    }
        .sortedWith(alarmOrder)

data class DatePickerPreferences(
    val shortcutMorning: Int = DEFAULT_MORNING,
    val shortcutAfternoon: Int = DEFAULT_AFTERNOON,
    val shortcutEvening: Int = DEFAULT_EVENING,
    val shortcutNight: Int = DEFAULT_NIGHT,
    val alwaysDisplayFullDate: Boolean = false,
    val datePickerInputMode: Boolean = false,
    val timePickerInputMode: Boolean = false,
) {
    val quickPickTimes: QuickPickTimes
        get() = QuickPickTimes(shortcutMorning, shortcutAfternoon, shortcutEvening, shortcutNight)
}

data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val persistentNotifications: Boolean = true,
    val wearableNotifications: Boolean = true,
    val bundleNotifications: Boolean = true,
    val voiceReminders: Boolean = false,
    val swipeToSnoozeEnabled: Boolean = false,
    val swipeToSnoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val defaultRemindersEnabled: Boolean = true,
    val defaultReminderTime: Int = DEFAULT_REMINDER_TIME,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = DEFAULT_QUIET_HOURS_START,
    val quietHoursEnd: Int = DEFAULT_QUIET_HOURS_END,
)

data class TaskDefaultSettings(
    val addTasksToTop: Boolean = true,
    val defaultList: String? = null,
    val defaultTags: List<String> = emptyList(),
    val defaultPriority: Int = Task.Priority.LOW,
    val defaultHideUntil: Int = Task.HIDE_UNTIL_NONE,
    val defaultDueDate: Int = Task.URGENCY_NONE,
    val defaultCalendar: String? = null,
    val defaultRecurrence: String? = null,
    val defaultRecurrenceFrom: Int = 0,
    val defaultAlarms: List<Alarm> = DEFAULT_ALARMS,
    val defaultRingMode: Int = 0,
    val defaultLocation: String? = null,
    val defaultLocationReminder: Int = 0,
    val locationUpdateIntervalMinutes: Int = DEFAULT_LOCATION_UPDATE_INTERVAL,
)

fun NotificationSettings.isCurrentlyQuietHours(
    now: Long = currentTimeMillis(),
): Boolean {
    if (!quietHoursEnabled) {
        return false
    }
    val start = now.withMillisOfDay(quietHoursStart)
    val end = now.withMillisOfDay(quietHoursEnd)
    return if (start > end) {
        now < end || now > start
    } else {
        now > start && now < end
    }
}

fun NotificationSettings.adjustForQuietHours(time: Long): Long {
    if (!quietHoursEnabled) {
        return time
    }
    val start = time.withMillisOfDay(quietHoursStart)
    val end = time.withMillisOfDay(quietHoursEnd)
    if (start > end) {
        if (time < end) {
            return end
        } else if (time > start) {
            return end.plusDays(1)
        }
    } else if (time > start && time < end) {
        return end
    }
    return time
}

interface AppPreferences {
    suspend fun getInstallVersion(): Int
    suspend fun setInstallVersion(value: Int)
    suspend fun getInstallDate(): Long
    suspend fun setInstallDate(value: Long)
    suspend fun getDeviceInstallVersion(): Int
    suspend fun setDeviceInstallVersion(value: Int)
    suspend fun isDefaultDueTimeEnabled(): Boolean
    suspend fun defaultLocationReminder(): Int
    suspend fun defaultAlarms(): List<Alarm>
    suspend fun defaultRingMode(): Int
    suspend fun defaultDueTime(): Int
    suspend fun defaultPriority(): Int
    suspend fun isCurrentlyQuietHours(): Boolean
    suspend fun adjustForQuietHours(time: Long): Long
    suspend fun locationUpdateIntervalMinutes(): Int = DEFAULT_LOCATION_UPDATE_INTERVAL
    suspend fun addTasksToTop(): Boolean = true
    suspend fun taskDefaults(): TaskDefaultSettings
    suspend fun setAddTasksToTop(value: Boolean)
    suspend fun setDefaultList(value: String?)
    suspend fun setDefaultTags(value: List<String>)
    suspend fun setDefaultPriority(value: Int)
    suspend fun setDefaultHideUntil(value: Int)
    suspend fun setDefaultDueDate(value: Int)
    suspend fun setDefaultCalendar(value: String?)
    suspend fun setDefaultRecurrence(value: String?)
    suspend fun setDefaultRecurrenceFrom(value: Int)

    suspend fun setDefaultAlarms(value: List<Alarm>)
    suspend fun setDefaultRingMode(value: Int)
    suspend fun setDefaultLocation(value: String?)
    suspend fun setDefaultLocationReminder(value: Int)
    suspend fun setLocationUpdateIntervalMinutes(value: Int)
    suspend fun datePickerPreferences(): DatePickerPreferences = DatePickerPreferences()
    suspend fun setDatePickerInputMode(value: Boolean) {}
    suspend fun setTimePickerInputMode(value: Boolean) {}
    suspend fun notificationSettings(): NotificationSettings
    suspend fun setNotificationsEnabled(value: Boolean)
    suspend fun setPersistentNotifications(value: Boolean)
    suspend fun setWearableNotifications(value: Boolean)
    suspend fun setBundleNotifications(value: Boolean)
    suspend fun setVoiceReminders(value: Boolean)
    suspend fun setSwipeToSnoozeEnabled(value: Boolean)
    suspend fun setSwipeToSnoozeMinutes(value: Int)
    suspend fun setDefaultRemindersEnabled(value: Boolean)
    suspend fun setDefaultReminderTime(value: Int)
    suspend fun setQuietHoursEnabled(value: Boolean)
    suspend fun setQuietHoursStart(value: Int)
    suspend fun setQuietHoursEnd(value: Int)
}
