@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.preferences.Preferences
import org.tasks.time.ONE_HOUR
import javax.inject.Inject

class Upgrade_15_10 @Inject constructor(
    private val preferences: Preferences,
) {
    internal suspend fun migrateRandomReminder() {
        val random = randomAlarm(preferences.getIntegerFromString(LEGACY_RANDOM_HOURS, 0))
            ?: return
        preferences.setDefaultAlarms(preferences.defaultAlarms() + random)
    }

    companion object {
        const val VERSION = 151000

        internal const val LEGACY_RANDOM_HOURS = "notif_default_reminder"

        fun randomAlarm(hours: Int): Alarm? = hours
            .takeIf { it > 0 }
            ?.let { Alarm(time = ONE_HOUR * it, type = TYPE_RANDOM) }
    }
}
