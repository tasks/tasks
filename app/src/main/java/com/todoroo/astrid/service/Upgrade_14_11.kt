@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.data.entity.Task.Companion.NOTIFY_AFTER_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_START
import org.tasks.preferences.Preferences
import javax.inject.Inject

class Upgrade_14_11 @Inject constructor(
    private val preferences: Preferences,
) {
    internal fun migrateDefaultAlarms() {
        val flags = preferences
            .getStringSet(
                R.string.p_default_reminders_key,
                hashSetOf(NOTIFY_AT_DEADLINE.toString(), NOTIFY_AFTER_DEADLINE.toString())
            )
            .mapNotNull { it.toIntOrNull() }
            .fold(0) { acc, flag -> acc or flag }
        preferences.setDefaultAlarms(fromLegacyFlags(flags))
    }

    companion object {
        const val VERSION = 141100

        fun fromLegacyFlags(flags: Int, task: Long = 0): List<Alarm> = buildList {
            if (flags and NOTIFY_AT_START != 0) add(whenStarted(task))
            if (flags and NOTIFY_AT_DEADLINE != 0) add(whenDue(task))
            if (flags and NOTIFY_AFTER_DEADLINE != 0) add(whenOverdue(task))
        }
    }
}
