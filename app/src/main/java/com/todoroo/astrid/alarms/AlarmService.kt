/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import com.todoroo.astrid.data.Task
import org.tasks.LocalBroadcastManager
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.Alarm.Companion.TYPE_REL_END
import org.tasks.data.Alarm.Companion.TYPE_REL_START
import org.tasks.data.AlarmDao
import org.tasks.data.TaskDao
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.NotificationQueue
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.withMillisOfDay
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
    private val jobs: NotificationQueue,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val localBroadcastManager: LocalBroadcastManager,
) {

    suspend fun getAlarms(taskId: Long): List<Alarm> = alarmDao.getAlarms(taskId)

    /**
     * Save the given array of alarms into the database
     *
     * @return true if data was changed
     */
    suspend fun synchronizeAlarms(taskId: Long, alarms: MutableSet<Alarm>): Boolean {
        val task = taskDao.fetch(taskId) ?: return false
        var changed = false
        for (existing in alarmDao.getAlarms(taskId)) {
            if (!alarms.removeIf {
                    it.type == existing.type &&
                            it.time == existing.time &&
                            it.repeat == existing.repeat &&
                            it.interval == existing.interval
            }) {
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
            scheduleAlarms(task)
            localBroadcastManager.broadcastRefresh()
        }
        return changed
    }

    private suspend fun getActiveAlarmsForTask(taskId: Long): List<Alarm> =
            alarmDao.getActiveAlarms(taskId)

    suspend fun scheduleAllAlarms() {
        alarmDao
            .getActiveAlarms()
            .groupBy { it.task }
            .forEach { (taskId, alarms) ->
                val task = taskDao.fetch(taskId) ?: return@forEach
                alarms.forEach { scheduleAlarm(task, it) }
            }
    }

    suspend fun cancelAlarms(taskId: Long) {
        for (alarm in getActiveAlarmsForTask(taskId)) {
            jobs.cancelAlarm(alarm.id)
        }
    }

    /** Schedules alarms for a single task  */
    suspend fun scheduleAlarms(task: Task) {
        getActiveAlarmsForTask(task.id).forEach { scheduleAlarm(task, it) }
    }

    /** Schedules alarms for a single task  */
    private fun scheduleAlarm(task: Task, alarm: Alarm?) {
        if (alarm == null) {
            return
        }
        val trigger = when (alarm.type) {
            TYPE_DATE_TIME ->
                alarm.time
            TYPE_REL_START ->
                when {
                    task.hasStartTime() ->
                        task.hideUntil + alarm.time
                    task.hasStartDate() ->
                        task.hideUntil.withMillisOfDay(preferences.defaultDueTime) + alarm.time
                    else ->
                        NO_ALARM
                }
            TYPE_REL_END ->
                when {
                    task.hasDueTime() ->
                        task.dueDate + alarm.time
                    task.hasDueDate() ->
                        task.dueDate.withMillisOfDay(preferences.defaultDueTime) + alarm.time
                    else ->
                        NO_ALARM
                }
            else -> NO_ALARM
        }
        jobs.cancelAlarm(alarm.id)
        when {
            trigger <= NO_ALARM ->
                {}
            trigger > task.reminderLast ->
                jobs.add(AlarmEntry(alarm.id, alarm.task, trigger))
            alarm.repeat > 0 -> {
                val past = (task.reminderLast - trigger) / alarm.interval
                val next = trigger + (past + 1) * alarm.interval
                if (past < alarm.repeat && next > task.reminderLast) {
                    jobs.add(AlarmEntry(alarm.id, alarm.task, next))
                }
            }
        }
    }

    companion object {
        private const val NO_ALARM = 0L
    }
}
