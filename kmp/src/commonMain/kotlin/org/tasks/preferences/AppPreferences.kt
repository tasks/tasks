package org.tasks.preferences

import org.tasks.data.entity.Alarm

interface AppPreferences {
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
