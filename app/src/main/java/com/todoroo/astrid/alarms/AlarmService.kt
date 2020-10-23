/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import kotlinx.coroutines.runBlocking
import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.NotificationQueue
import java.util.*
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
        if (oldDueDate <= 0 || newDueDate <= 0 || newDueDate <= oldDueDate) {
            return
        }
        val alarms: MutableSet<Long> = LinkedHashSet()
        for (alarm in getAlarms(taskId)) {
            alarms.add(alarm.time + (newDueDate - oldDueDate))
        }
        if (alarms.isNotEmpty()) {
            synchronizeAlarms(taskId, alarms)
        }
    }

    suspend fun getAlarms(taskId: Long): List<Alarm> {
        return alarmDao.getAlarms(taskId)
    }

    /**
     * Save the given array of alarms into the database
     *
     * @return true if data was changed
     */
    suspend fun synchronizeAlarms(taskId: Long, timestamps: MutableSet<Long>): Boolean {
        var changed = false
        for (item in alarmDao.getAlarms(taskId)) {
            if (!timestamps.remove(item.time)) {
                jobs.cancelAlarm(item.id)
                alarmDao.delete(item)
                changed = true
            }
        }
        for (timestamp in timestamps) {
            alarmDao.insert(Alarm(taskId, timestamp))
            changed = true
        }
        if (changed) {
            scheduleAlarms(taskId)
        }
        return changed
    }

    private suspend fun getActiveAlarmsForTask(taskId: Long): List<Alarm> {
        return alarmDao.getActiveAlarms(taskId)
    }

    // TODO: remove runBlocking
    fun scheduleAllAlarms() = runBlocking {
        for (alarm in alarmDao.getActiveAlarms()) {
            scheduleAlarm(alarm)
        }
    }

    suspend fun cancelAlarms(taskId: Long) {
        for (alarm in getActiveAlarmsForTask(taskId)) {
            jobs.cancelAlarm(alarm.id)
        }
    }

    /** Schedules alarms for a single task  */
    private suspend fun scheduleAlarms(taskId: Long) {
        for (alarm in getActiveAlarmsForTask(taskId)) {
            scheduleAlarm(alarm)
        }
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