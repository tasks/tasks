/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms

import co.touchlab.kermit.Logger
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfMinute

class AlarmService(
    private val alarmDao: AlarmDao,
    private val taskDao: TaskDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val notifier: Notifier,
    private val alarmCalculator: AlarmCalculator,
    private val preferences: AppPreferences,
) {
    private data class AlarmTrigger(
        val alarm: Alarm,
        val notification: Notification,
    )

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
            refreshBroadcaster.broadcastRefresh()
        }
        return changed
    }

    suspend fun snooze(time: Long, taskIds: List<Long>) {
        notifier.cancel(taskIds, CancelReason.SNOOZE)
        val templates = taskIds.associateWith { taskId ->
            getSnoozeTemplate(taskId)
        }
        alarmDao.deleteSnoozed(taskIds)
        val snoozed = taskIds.map { taskId ->
            val template = templates[taskId]
            Alarm(
                task = taskId,
                time = time,
                type = TYPE_SNOOZE,
                repeat = template?.repeat ?: 0,
                interval = template?.interval ?: 0,
            )
        }
        alarmDao.insert(snoozed)
        taskDao.touch(taskIds)
        notifier.triggerNotifications()
    }

    suspend fun triggerAlarms(
        trigger: suspend (List<Notification>) -> Unit
    ): Long {
        if (preferences.isCurrentlyQuietHours()) {
            return preferences.adjustForQuietHours(currentTimeMillis())
        }
        val (overdue, _) = getAlarms()
        val triggered = overdue
            .sortedBy { it.notification.timestamp }
            .map { trigger ->
                trigger.copy(
                    notification = trigger.notification.copy(timestamp = currentTimeMillis())
                )
            }
        trigger(triggered.map { it.notification })
        triggered.forEach { trigger ->
            if (trigger.alarm.type == TYPE_SNOOZE) {
                if (trigger.alarm.repeat > 0 && trigger.alarm.interval > 0) {
                    val updated = trigger.alarm.copy(
                        time = trigger.notification.timestamp + trigger.alarm.interval,
                        repeat = trigger.alarm.repeat - 1,
                    )
                    alarmDao.update(updated)
                } else {
                    alarmDao.deleteSnoozed(listOf(trigger.notification.taskId))
                }
            }
        }
        val alreadyTriggered = triggered.map { it.notification.taskId }.toSet()
        val (moreOverdue, future) = getAlarms()
        val nextAlarm = moreOverdue
            .map { it.notification }
            .filterNot { it.type == Alarm.TYPE_RANDOM || alreadyTriggered.contains(it.taskId) }
            .plus(future.map { it.notification })
            .minOfOrNull { it.timestamp }
            ?: 0
        return nextAlarm
    }

    private suspend fun getAlarms(): Pair<List<AlarmTrigger>, List<AlarmTrigger>> {
        val start = currentTimeMillis()
        val overdue = ArrayList<AlarmTrigger>()
        val future = ArrayList<AlarmTrigger>()
        val nextMinute = currentTimeMillis().startOfMinute() + 60_000
        alarmDao.getActiveAlarms()
            .groupBy { it.task }
            .forEach { (taskId, alarms) ->
                val task = taskDao.fetch(taskId) ?: return@forEach
                val (now, later) = getAlarms(task, alarms, nextMinute)
                now?.let { overdue.add(it) }
                later?.let { future.add(it) }
            }
        Logger.d("AlarmService") { "took ${currentTimeMillis() - start}ms overdue=${overdue.size} future=${future.size}" }
        return overdue to future
    }

    private fun getAlarms(
        task: Task,
        alarms: List<Alarm>,
        nextMinute: Long,
    ): Pair<AlarmTrigger?, AlarmTrigger?> {
        val alarmEntries = alarms.mapNotNull { alarm ->
            alarmCalculator.toAlarmEntry(task, alarm)?.let { notification ->
                AlarmTrigger(alarm, notification)
            }
        }
        val snoozed = alarmEntries
            .filter { it.notification.type == TYPE_SNOOZE }
            .maxByOrNull { it.notification.timestamp }
        if (snoozed != null) {
            return if (snoozed.notification.timestamp < nextMinute) {
                snoozed to null
            } else {
                null to snoozed
            }
        }
        val (now, later) = alarmEntries.partition {
            it.notification.timestamp < nextMinute
        }
        return now.firstOrNull() to later.minByOrNull { it.notification.timestamp }
    }

    private suspend fun getSnoozeTemplate(taskId: Long): Alarm? {
        val task = taskDao.fetch(taskId) ?: return null
        val alarms = alarmDao.getActiveAlarms(taskId)
        val baseAlarms = alarms.filter { it.type != TYPE_SNOOZE }
        val nextMinute = currentTimeMillis().startOfMinute() + 60_000
        val (baseOverdue, baseFuture) = getAlarms(task, baseAlarms, nextMinute)
        if (baseOverdue != null || baseFuture != null) {
            return baseOverdue?.alarm ?: baseFuture?.alarm
        }
        val (overdue, future) = getAlarms(task, alarms, nextMinute)
        if (overdue != null || future != null) {
            return overdue?.alarm ?: future?.alarm
        }
        return baseAlarms
            .mapNotNull { alarm ->
                alarmCalculator.latestTriggerAtOrBefore(task, alarm, task.reminderLast)?.let { timestamp ->
                    alarm to timestamp
                }
            }
            .maxByOrNull { it.second }
            ?.first
    }

    companion object {
        internal const val NO_ALARM = 0L
    }
}
