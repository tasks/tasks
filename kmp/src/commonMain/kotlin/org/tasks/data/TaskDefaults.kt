package org.tasks.data

import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TaskDefaultSettings

suspend fun Task.setDefaultReminders(preferences: AppPreferences) {
    putTransitory(Task.TRANS_DEFAULT_ALARMS, preferences.defaultAlarms())
    ringFlags = preferences.defaultRingMode()
}

fun Task.setDefaultReminders(defaults: TaskDefaultSettings) {
    putTransitory(Task.TRANS_DEFAULT_ALARMS, defaults.defaultAlarms)
    ringFlags = defaults.defaultRingMode
}

fun Task.getDefaultAlarms(defaultRemindersEnabled: Boolean): List<Alarm> = buildList {
    val defaults = getTransitory<List<Alarm>>(Task.TRANS_DEFAULT_ALARMS) ?: emptyList()
    for (alarm in defaults) {
        when (alarm.type) {
            TYPE_REL_START ->
                if (hasStartDate() && (hasStartTime() || defaultRemindersEnabled))
                    add(alarm.copy(task = id))
            TYPE_REL_END ->
                if (hasDueDate() && (hasDueTime() || defaultRemindersEnabled))
                    add(alarm.copy(task = id))
            TYPE_RANDOM -> add(alarm.copy(task = id))
        }
    }
}

internal fun Iterable<Alarm>.applicableTo(task: Task): List<Alarm> =
    filterNot { it.type == TYPE_REL_START && !task.hasStartDate() }
        .filterNot { it.type == TYPE_REL_END && !task.hasDueDate() }
