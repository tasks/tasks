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
import org.tasks.data.entity.Task
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
        val templates = taskIds.associateWith { getSnoozeTemplate(it) }
        recordDismissal(taskIds, currentTimeMillis()) {
            alarmDao.deleteSnoozed(taskIds)
            alarmDao.insert(
                taskIds.map { taskId ->
                    val template = templates[taskId]
                    Alarm(
                        task = taskId,
                        time = time,
                        type = TYPE_SNOOZE,
                        repeat = template?.repeat ?: 0,
                        interval = template?.interval ?: 0,
                    )
                }
            )
        }
        notifier.triggerNotifications()
    }

    suspend fun markDismissed(taskIds: List<Long>, time: Long = currentTimeMillis()) =
        recordDismissal(taskIds, time)

    private suspend fun recordDismissal(
        taskIds: List<Long>,
        time: Long,
        alsoInTransaction: suspend () -> Unit = {},
    ) {
        if (taskIds.isEmpty()) {
            return
        }
        taskDao.inTransaction {
            alsoInTransaction()
            taskDao.setReminderDismissed(taskIds, time)
            dirtyDao.setDirty(taskIds, TYPES_ALARMS)
        }
        refreshBroadcaster.broadcastRefresh()
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
        val handledSnoozed = snoozed.filter { it.task in handled }
        handledSnoozed
            .filter { it.repeat > 0 && it.interval > 0 }
            .forEach { alarm ->
                alarmDao.update(
                    alarm.copy(
                        time = start + alarm.interval,
                        repeat = alarm.repeat - 1,
                    )
                )
            }
        handledSnoozed
            .filterNot { it.repeat > 0 && it.interval > 0 }
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
                val snoozed = alarmEntries
                    .filter { it.type == TYPE_SNOOZE }
                    .maxByOrNull { it.timestamp }
                if (snoozed != null) {
                    if (snoozed.timestamp < cutoff) {
                        overdue.add(snoozed)
                    } else {
                        future.add(snoozed)
                    }
                } else {
                    val (now, later) = alarmEntries.partition {
                        it.timestamp < cutoff
                    }
                    now.minByOrNull { it.timestamp }?.let { overdue.add(it) }
                    later.minByOrNull { it.timestamp }?.let { future.add(it) }
                }
            }
        Logger.d("AlarmService") { "took ${currentTimeMillis() - start}ms overdue=${overdue.size} future=${future.size}" }
        return overdue to future
    }

    private fun getActiveAlarm(
        task: Task,
        alarms: List<Alarm>,
        defaultDueTime: Int,
        cutoff: Long,
    ): Notification? {
        val alarmEntries = alarms.mapNotNull { alarm ->
            alarmCalculator.toAlarmEntry(task, alarm, defaultDueTime)
        }
        val snoozed = alarmEntries
            .filter { it.type == TYPE_SNOOZE }
            .maxByOrNull { it.timestamp }
        if (snoozed != null) {
            return snoozed
        }
        val (now, later) = alarmEntries.partition { it.timestamp < cutoff }
        return now.minByOrNull { it.timestamp } ?: later.minByOrNull { it.timestamp }
    }

    private suspend fun getSnoozeTemplate(taskId: Long): Alarm? {
        val task = taskDao.fetch(taskId) ?: return null
        val alarms = alarmDao.getActiveAlarms(taskId)
        val baseAlarms = alarms.filter { it.type != TYPE_SNOOZE }
        val defaultDueTime = preferences.defaultDueTime()
        val cutoff = currentTimeMillis().startOfMinute() + ONE_MINUTE
        val baseTemplate = getActiveAlarm(task, baseAlarms, defaultDueTime, cutoff)
        if (baseTemplate != null) {
            return baseAlarms.firstOrNull { alarm ->
                alarmCalculator.toAlarmEntry(task, alarm, defaultDueTime) == baseTemplate
            }
        }
        val historicalBaseTemplate = baseAlarms
            .mapNotNull { alarm ->
                alarmCalculator.latestTriggerAtOrBefore(task, alarm, defaultDueTime, task.reminderLast)
                    ?.let { timestamp -> alarm to timestamp }
            }
            .maxByOrNull { it.second }
            ?.first
        if (historicalBaseTemplate != null) {
            return historicalBaseTemplate
        }
        val activeTemplate = getActiveAlarm(task, alarms, defaultDueTime, cutoff)
        return activeTemplate?.let { notification ->
            alarms.firstOrNull { alarm ->
                alarmCalculator.toAlarmEntry(task, alarm, defaultDueTime) == notification
            }
        }
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
