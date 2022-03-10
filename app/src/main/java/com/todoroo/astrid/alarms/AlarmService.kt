/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import com.todoroo.astrid.data.Task
import org.tasks.LocalBroadcastManager
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.AlarmDao
import org.tasks.data.TaskDao
import org.tasks.jobs.NotificationQueue
import org.tasks.notifications.NotificationManager
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
    private val localBroadcastManager: LocalBroadcastManager,
    private val notificationManager: NotificationManager,
    private val alarmCalculator: AlarmCalculator,
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
            localBroadcastManager.broadcastRefreshList()
        }
        return changed
    }

    suspend fun scheduleAllAlarms() {
        alarmDao
            .getActiveAlarms()
            .groupBy { it.task }
            .forEach { (taskId, alarms) ->
                val task = taskDao.fetch(taskId) ?: return@forEach
                scheduleAlarms(task, alarms)
            }
    }

    fun cancelAlarms(taskId: Long) {
        jobs.cancelForTask(taskId)
    }

    suspend fun snooze(time: Long, taskIds: List<Long>) {
        notificationManager.cancel(taskIds)
        alarmDao.getSnoozed(taskIds).let { alarmDao.delete(it) }
        taskIds.map { Alarm(it, time, TYPE_SNOOZE) }.let { alarmDao.insert(it) }
        taskDao.touch(taskIds)
        scheduleAlarms(taskIds)
    }

    suspend fun scheduleAlarms(taskIds: List<Long>) {
        taskDao.fetch(taskIds).forEach { scheduleAlarms(it) }
    }

    /** Schedules alarms for a single task  */
    suspend fun scheduleAlarms(task: Task) {
        scheduleAlarms(task, alarmDao.getActiveAlarms(task.id))
    }

    private fun scheduleAlarms(task: Task, alarms: List<Alarm>) {
        jobs.cancelForTask(task.id)
        val alarmEntries = alarms.mapNotNull {
            alarmCalculator.toAlarmEntry(task, it)
        }
        val next =
            alarmEntries.find { it.type == TYPE_SNOOZE } ?: alarmEntries.minByOrNull { it.time }
        next?.let { jobs.add(it) }
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
