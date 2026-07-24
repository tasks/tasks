package org.tasks.preferences

import org.tasks.compose.pickers.DEFAULT_AFTERNOON
import org.tasks.compose.pickers.DEFAULT_EVENING
import org.tasks.compose.pickers.DEFAULT_MORNING
import org.tasks.compose.pickers.DEFAULT_NIGHT
import org.tasks.compose.pickers.QuickPickTimes
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task

data class DatePickerPreferences(
    val shortcutMorning: Int = DEFAULT_MORNING,
    val shortcutAfternoon: Int = DEFAULT_AFTERNOON,
    val shortcutEvening: Int = DEFAULT_EVENING,
    val shortcutNight: Int = DEFAULT_NIGHT,
    val defaultHideUntil: Int = Task.HIDE_UNTIL_NONE,
    val alwaysDisplayFullDate: Boolean = false,
    val datePickerInputMode: Boolean = false,
    val timePickerInputMode: Boolean = false,
) {
    val quickPickTimes: QuickPickTimes
        get() = QuickPickTimes(shortcutMorning, shortcutAfternoon, shortcutEvening, shortcutNight)
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
    suspend fun defaultRandomHours(): Int
    suspend fun defaultRingMode(): Int
    suspend fun defaultDueTime(): Int
    suspend fun defaultPriority(): Int
    suspend fun isCurrentlyQuietHours(): Boolean
    suspend fun adjustForQuietHours(time: Long): Long
    suspend fun locationUpdateIntervalMinutes(): Int = 15
    suspend fun addTasksToTop(): Boolean = true
    suspend fun datePickerPreferences(): DatePickerPreferences = DatePickerPreferences()
    suspend fun setDatePickerInputMode(value: Boolean) {}
    suspend fun setTimePickerInputMode(value: Boolean) {}
}
