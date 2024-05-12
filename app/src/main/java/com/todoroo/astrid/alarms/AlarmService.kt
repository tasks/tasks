/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import org.tasks.LocalBroadcastManager
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class AlarmService @Inject constructor(
    private val alarmDao: AlarmDao,
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val notificationManager: NotificationManager,
    private val workManager: WorkManager,
    private val alarmCalculator: AlarmCalculator,
) {
    suspend fun getAlarms(taskId: Long): List<Alarm> = alarmDao.getAlarms(taskId)

    /**
     * Save the given array of alarms into the database
     *
     * @return true if data was changed
     */
    suspend fun synchronizeAlarms(taskId: Long, alarms: MutableSet<Alarm>): Boolean {
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
            workManager.triggerNotifications()
            localBroadcastManager.broadcastRefreshList()
        }
        return changed
    }

    suspend fun snooze(time: Long, taskIds: List<Long>) {
        notificationManager.cancel(taskIds)
        alarmDao.getSnoozed(taskIds).let { alarmDao.delete(it) }
        taskIds.map { Alarm(it, time, TYPE_SNOOZE) }.let { alarmDao.insert(it) }
        taskDao.touch(taskIds)
        workManager.triggerNotifications()
    }

    suspend fun getAlarms(): Pair<List<AlarmEntry>, List<AlarmEntry>> {
        val start = currentTimeMillis()
        val overdue = ArrayList<AlarmEntry>()
        val future = ArrayList<AlarmEntry>()
        alarmDao.getActiveAlarms()
            .groupBy { it.task }
            .forEach { (taskId, alarms) ->
                val task = taskDao.fetch(taskId) ?: return@forEach
                val alarmEntries = alarms.mapNotNull {
                    alarmCalculator.toAlarmEntry(task, it)
                }
                val (now, later) = alarmEntries.partition { it.time <= currentTimeMillis() }
                later
                    .find { it.type == TYPE_SNOOZE }
                    ?.let { future.add(it) }
                    ?: run {
                        now.firstOrNull()?.let { overdue.add(it) }
                        later.minByOrNull { it.time }?.let { future.add(it) }
                    }
            }
        Timber.d("took ${currentTimeMillis() - start}ms overdue=${overdue.size} future=${future.size}")
        return overdue to future
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
