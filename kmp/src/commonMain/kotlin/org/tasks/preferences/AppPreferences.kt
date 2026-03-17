package org.tasks.preferences

import org.tasks.data.entity.Alarm

interface AppPreferences {
    suspend fun isDefaultDueTimeEnabled(): Boolean
    suspend fun defaultLocationReminder(): Int
    suspend fun defaultAlarms(): List<Alarm>
    suspend fun defaultRandomHours(): Int
    suspend fun defaultRingMode(): Int
}
