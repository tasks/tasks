/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import org.tasks.data.TaskDao
import org.tasks.jobs.NotificationQueue
import org.tasks.jobs.ReminderEntry
import org.tasks.preferences.Preferences
import org.tasks.reminders.Random
import org.tasks.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderService internal constructor(
        private val preferences: Preferences,
        private val jobs: NotificationQueue,
        private val random: Random,
        private val taskDao: TaskDao) {

    @Inject
    internal constructor(
            preferences: Preferences,
            notificationQueue: NotificationQueue,
            taskDao: TaskDao
    ) : this(preferences, notificationQueue, Random(), taskDao)

    suspend fun scheduleAlarm(id: Long) = scheduleAllAlarms(listOf(id))

    suspend fun scheduleAllAlarms(taskIds: List<Long>) = scheduleAlarms(taskDao.fetch(taskIds))

    suspend fun scheduleAllAlarms() = scheduleAlarms(taskDao.getTasksWithReminders())

    fun scheduleAlarm(task: Task) = scheduleAlarms(listOf(task))

    private fun scheduleAlarms(tasks: List<Task>) =
            tasks
                    .mapNotNull { getReminderEntry(it) }
                    .let { jobs.add(it) }

    fun cancelReminder(taskId: Long) {
        jobs.cancelReminder(taskId)
    }

    private fun getReminderEntry(task: Task?): ReminderEntry? {
        if (task == null || !task.isSaved) {
            return null
        }
        val taskId = task.id

        // Make sure no alarms are scheduled other than the next one. When that one is shown, it
        // will schedule the next one after it, and so on and so forth.
        cancelReminder(taskId)
        if (task.isCompleted || task.isDeleted) {
            return null
        }

        // snooze reminder
        val whenSnooze = calculateNextSnoozeReminder(task)

        // random reminders
        val whenRandom = calculateNextRandomReminder(task)

        val whenStartDate = calculateStartDateReminder(task)

        // notifications at due date
        val whenDueDate = calculateNextDueDateReminder(task)

        // notifications after due date
        val whenOverdue = calculateNextOverdueReminder(task)

        // snooze trumps all
        if (whenSnooze != NO_ALARM) {
            return ReminderEntry(taskId, whenSnooze, TYPE_SNOOZE)
        } else if (
            whenRandom < whenDueDate &&
            whenRandom < whenOverdue &&
            whenRandom < whenStartDate
        ) {
            return ReminderEntry(taskId, whenRandom, TYPE_RANDOM)
        } else if (whenStartDate < whenDueDate) {
            return ReminderEntry(taskId, whenStartDate, TYPE_START)
        } else if (whenDueDate < whenOverdue) {
            return ReminderEntry(taskId, whenDueDate, TYPE_DUE)
        } else if (whenOverdue != NO_ALARM) {
            return ReminderEntry(taskId, whenOverdue, TYPE_OVERDUE)
        }
        return null
    }

    private fun calculateNextSnoozeReminder(task: Task): Long {
        return if (task.reminderSnooze > task.reminderLast) {
            task.reminderSnooze
        } else NO_ALARM
    }

    private fun calculateNextOverdueReminder(task: Task): Long {
        // Uses getNowValue() instead of DateUtilities.now()
        if (task.hasDueDate() && task.isNotifyAfterDeadline) {
            var overdueDate = DateTime(task.dueDate).plusDays(1)
            if (!task.hasDueTime()) {
                overdueDate = overdueDate.withMillisOfDay(preferences.defaultDueTime)
            }
            val lastReminder = DateTime(task.reminderLast)
            if (overdueDate.isAfter(lastReminder)) {
                return overdueDate.millis
            }
            overdueDate = lastReminder.withMillisOfDay(overdueDate.millisOfDay)
            return if (overdueDate.isAfter(lastReminder)) overdueDate.millis else overdueDate.plusDays(1).millis
        }
        return NO_ALARM
    }

    private fun calculateStartDateReminder(task: Task): Long {
        if (task.hasStartDate() && task.isNotifyAtStart) {
            val startDate = task.hideUntil
            val startDateAlarm = if (task.hasStartTime()) {
                startDate
            } else {
                DateTime(startDate).withMillisOfDay(preferences.defaultDueTime).millis
            }
            if (task.reminderLast < startDateAlarm) {
                return startDateAlarm
            }
        }
        return NO_ALARM
    }

    /**
     * Calculate the next alarm time for due date reminders.
     *
     *
     * This alarm always returns the due date, and is triggered if the last reminder time occurred
     * before the due date. This means it is possible to return due dates in the past.
     *
     *
     * If the date was indicated to not have a due time, we read from preferences and assign a
     * time.
     */
    private fun calculateNextDueDateReminder(task: Task): Long {
        if (task.hasDueDate() && task.isNotifyAtDeadline) {
            val dueDate = task.dueDate
            val dueDateAlarm = if (task.hasDueTime()) {
                dueDate
            } else {
                DateTime(dueDate).withMillisOfDay(preferences.defaultDueTime).millis
            }
            if (task.reminderLast < dueDateAlarm) {
                return dueDateAlarm
            }
        }
        return NO_ALARM
    }

    /**
     * Calculate the next alarm time for random reminders.
     *
     *
     * We take the last reminder time and add approximately the reminder period. If it's still in
     * the past, we set it to some time in the near future.
     */
    private fun calculateNextRandomReminder(task: Task): Long {
        val reminderPeriod = task.reminderPeriod
        if (reminderPeriod > 0) {
            var `when` = task.reminderLast
            if (`when` == 0L) {
                `when` = task.creationDate
            }
            `when` += (reminderPeriod * (0.85f + 0.3f * random.nextFloat())).toLong()
            if (`when` < DateUtilities.now()) {
                `when` = DateUtilities.now() + ((0.5f + 6 * random.nextFloat()) * DateUtilities.ONE_HOUR).toLong()
            }
            return `when`
        }
        return NO_ALARM
    }

    companion object {
        const val TYPE_DUE = 0
        const val TYPE_OVERDUE = 1
        const val TYPE_RANDOM = 2
        const val TYPE_SNOOZE = 3
        const val TYPE_ALARM = 4
        const val TYPE_GEOFENCE_ENTER = 5
        const val TYPE_GEOFENCE_EXIT = 6
        const val TYPE_START = 7
        private const val NO_ALARM = Long.MAX_VALUE
    }
}