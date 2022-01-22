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
import org.tasks.reminders.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderService internal constructor(
        private val jobs: NotificationQueue,
        private val random: Random,
        private val taskDao: TaskDao,
) {

    @Inject
    internal constructor(
            notificationQueue: NotificationQueue,
            taskDao: TaskDao
    ) : this(notificationQueue, Random(), taskDao)

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

        // snooze trumps all
        return when {
            whenSnooze != NO_ALARM -> ReminderEntry(taskId, whenSnooze, TYPE_SNOOZE)
            whenRandom != NO_ALARM -> ReminderEntry(taskId, whenRandom, TYPE_RANDOM)
            else -> null
        }
    }

    private fun calculateNextSnoozeReminder(task: Task): Long {
        return if (task.reminderSnooze > task.reminderLast) {
            task.reminderSnooze
        } else NO_ALARM
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