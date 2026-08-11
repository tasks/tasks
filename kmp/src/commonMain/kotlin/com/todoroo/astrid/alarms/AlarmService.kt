/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import co.touchlab.kermit.Logger
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.CaldavAccount.Companion.TYPES_ALARMS
import org.tasks.data.entity.Notification
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_MINUTE
import org.tasks.time.startOfMinute

class AlarmService(
    private val alarmDao: AlarmDao,
    private val taskDao: TaskDao,
    private val dirtyDao: DirtyDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val notifier: Notifier,
    private val alarmCalculator: AlarmCalculator,
    private val preferences: AppPreferences,
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
        val snoozedInFuture =
            alarms.any { it.type == TYPE_SNOOZE && it.time > currentTimeMillis() }
        alarmDao.insert(alarms.map { it.copy(task = taskId) })
        if (alarms.isNotEmpty()) {
            changed = true
        }
        if (snoozedInFuture) {
            notifier.cancel(listOf(taskId), CancelReason.SNOOZE)
        }
        if (changed) {
            refreshBroadcaster.broadcastRefresh()
        }
        return changed
    }

    suspend fun snooze(time: Long, taskIds: List<Long>) {
        notifier.cancel(taskIds, CancelReason.SNOOZE)
        taskDao.inTransaction {
            alarmDao.deleteSnoozed(taskIds)
            alarmDao.insert(taskIds.map { Alarm(task = it, time = time, type = TYPE_SNOOZE) })
            taskDao.touch(taskIds)
            dirtyDao.setDirty(taskIds, TYPES_ALARMS)
        }
        notifier.triggerNotifications()
    }

    suspend fun triggerAlarms(
        trigger: suspend (List<Notification>) -> Collection<Long>,
    ): Long {
        if (preferences.isCurrentlyQuietHours()) {
            return preferences.adjustForQuietHours(currentTimeMillis())
        }
        val cutoff = currentTimeMillis().startOfMinute() + ONE_MINUTE
        val (overdue, upcoming) = alarmsDueBefore(cutoff)
        val snoozed = overdue
            .map { it.taskId }
            .distinct()
            .chunkedMap { alarmDao.getSnoozed(it, cutoff) }
        val start = currentTimeMillis()
        val handled = overdue
            .sortedBy { it.timestamp }
            .map { it.copy(timestamp = start) }
            .let { trigger(it) }
            .toSet()
        snoozed
            .filter { it.task in handled }
            .map { it.id }
            .eachChunk { alarmDao.deleteByIds(it) }
        val alreadyTriggered = overdue.map { it.taskId }.toSet()

        val (moreOverdue, future) = if (handled.isEmpty()) {
            overdue to upcoming
        } else {
            alarmsDueBefore(cutoff)
        }
        return moreOverdue
            .filterNot { it.type == Alarm.TYPE_RANDOM || alreadyTriggered.contains(it.taskId) }
            .plus(future)
            .minOfOrNull { it.timestamp }
            ?: NO_ALARM
    }

    internal suspend fun alarmsDueBefore(
        cutoff: Long,
    ): Pair<List<Notification>, List<Notification>> {
        val start = currentTimeMillis()
        val overdue = ArrayList<Notification>()
        val future = ArrayList<Notification>()
        val defaultDueTime = preferences.defaultDueTime()
        val byTask = alarmDao.getActiveAlarms().groupBy { it.task }
        val tasks = taskDao.fetch(byTask.keys.toList()).associateBy { it.id }
        byTask
            .forEach { (taskId, alarms) ->
                val task = tasks[taskId] ?: return@forEach
                val alarmEntries = alarms.mapNotNull {
                    alarmCalculator.toAlarmEntry(task, it, defaultDueTime)
                }
                val (now, later) = alarmEntries.partition {
                    it.timestamp < cutoff
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
        Logger.d("AlarmService") { "took ${currentTimeMillis() - start}ms overdue=${overdue.size} future=${future.size}" }
        return overdue to future
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
