/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.NotificationQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@Singleton
class AlarmService @Inject constructor(
        private val alarmDao: AlarmDao,
        private val jobs: NotificationQueue) {

    suspend fun rescheduleAlarms(taskId: Long, oldDueDate: Long, newDueDate: Long) {
        if (oldDueDate <= 0 || newDueDate <= 0) {
            return
        }
        getAlarms(taskId)
            .takeIf { it.isNotEmpty() }
            ?.onEach { it.time += newDueDate - oldDueDate }
            ?.let { synchronizeAlarms(taskId, it.toMutableSet()) }
    }

    private suspend fun getAlarms(taskId: Long): List<Alarm> = alarmDao.getAlarms(taskId)

    /**
     * Save the given array of alarms into the database
     *
     * @return true if data was changed
     */
    suspend fun synchronizeAlarms(taskId: Long, alarms: MutableSet<Alarm>): Boolean {
        var changed = false
        for (existing in alarmDao.getAlarms(taskId)) {
            if (!alarms.removeIf { it.time == existing.time }) {
                jobs.cancelAlarm(existing.id)
                alarmDao.delete(existing)
                changed = true
            }
        }
        for (alarm in alarms) {
            alarm.task = taskId
            alarmDao.insert(alarm)
            changed = true
        }
        if (changed) {
            scheduleAlarms(taskId)
        }
        return changed
    }

    private suspend fun getActiveAlarmsForTask(taskId: Long): List<Alarm> =
            alarmDao.getActiveAlarms(taskId)

    suspend fun scheduleAllAlarms() {
        alarmDao.getActiveAlarms().forEach(::scheduleAlarm)
    }

    suspend fun cancelAlarms(taskId: Long) {
        for (alarm in getActiveAlarmsForTask(taskId)) {
            jobs.cancelAlarm(alarm.id)
        }
    }

    /** Schedules alarms for a single task  */
    private suspend fun scheduleAlarms(taskId: Long) {
        getActiveAlarmsForTask(taskId).forEach(::scheduleAlarm)
    }

    /** Schedules alarms for a single task  */
    private fun scheduleAlarm(alarm: Alarm?) {
        if (alarm == null) {
            return
        }
        val alarmEntry = AlarmEntry(alarm)
        val time = alarmEntry.time
        if (time == 0L || time == NO_ALARM) {
            jobs.cancelAlarm(alarmEntry.id)
        } else {
            jobs.add(alarmEntry)
        }
    }

    companion object {
        private const val NO_ALARM = Long.MAX_VALUE
    }
}