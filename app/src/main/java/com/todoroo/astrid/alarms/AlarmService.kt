/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.DbUtils
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Notification
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
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
    private val preferences: Preferences,
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
            if (!alarms.removeIf { it.same(existing)}) {
                alarmDao.delete(existing)
                changed = true
            }
        }
        alarmDao.insert(alarms.map { it.copy(task = taskId) })
        if (alarms.isNotEmpty()) {
            changed = true
        }
        if (changed) {
            localBroadcastManager.broadcastRefreshList()
        }
        return changed
    }

    suspend fun snooze(time: Long, taskIds: List<Long>) {
        notificationManager.cancel(taskIds)
        alarmDao.deleteSnoozed(taskIds)
        alarmDao.insert(taskIds.map { Alarm(task = it, time = time, type = TYPE_SNOOZE) })
        taskDao.touch(taskIds)
        workManager.triggerNotifications()
    }

    suspend fun triggerAlarms(
        trigger: suspend (List<Notification>) -> Unit
    ): Long {
        if (preferences.isCurrentlyQuietHours) {
            return preferences.adjustForQuietHours(currentTimeMillis())
        }
        val (overdue, _) = getAlarms()
        overdue
            .sortedBy { it.timestamp }
            .also { alarms ->
                alarms
                    .map { it.taskId }
                    .chunked(DbUtils.MAX_SQLITE_ARGS)
                    .onEach { alarmDao.deleteSnoozed(it) }
            }
            .map { it.copy(timestamp = currentTimeMillis()) }
            .let { trigger(it) }
        val alreadyTriggered = overdue.map { it.taskId }.toSet()
        val (moreOverdue, future) = getAlarms()
        return moreOverdue
            .filterNot { it.type == Alarm.TYPE_RANDOM || alreadyTriggered.contains(it.taskId) }
            .plus(future)
            .minOfOrNull { it.timestamp }
            ?: 0
    }

    internal suspend fun getAlarms(): Pair<List<Notification>, List<Notification>> {
        val start = currentTimeMillis()
        val overdue = ArrayList<Notification>()
        val future = ArrayList<Notification>()
        alarmDao.getActiveAlarms()
            .groupBy { it.task }
            .forEach { (taskId, alarms) ->
                val task = taskDao.fetch(taskId) ?: return@forEach
                val alarmEntries = alarms.mapNotNull {
                    alarmCalculator.toAlarmEntry(task, it)
                }
                val (now, later) = alarmEntries.partition {
                    it.timestamp < DateTime().startOfMinute().plusMinutes(1).millis
                }
                later
                    .filter { it.type == TYPE_SNOOZE }
                    .maxByOrNull { it.timestamp }
                    ?.let { future.add(it) }
                    ?: run {
                        now.firstOrNull()?.let { overdue.add(it) }
                        later.minByOrNull { it.timestamp }?.let { future.add(it) }
                    }
            }
        Timber.d("took ${currentTimeMillis() - start}ms overdue=${overdue.size} future=${future.size}")
        return overdue to future
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
