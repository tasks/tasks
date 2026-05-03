package org.tasks.preferences

import org.tasks.data.entity.Alarm

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
}
