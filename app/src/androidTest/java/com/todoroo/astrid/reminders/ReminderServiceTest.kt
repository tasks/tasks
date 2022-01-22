package com.todoroo.astrid.reminders

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze.Companion.freezeClock
import org.tasks.data.TaskDao
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.jobs.NotificationQueue
import org.tasks.jobs.ReminderEntry
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.RANDOM_REMINDER_PERIOD
import org.tasks.makers.TaskMaker.REMINDERS
import org.tasks.makers.TaskMaker.REMINDER_LAST
import org.tasks.makers.TaskMaker.SNOOZE_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.preferences.Preferences
import org.tasks.reminders.Random
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class ReminderServiceTest : InjectingTestCase() {
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var jobs: NotificationQueue

    private lateinit var service: ReminderService
    private lateinit var random: RandomStub

    @Before
    override fun setUp() {
        super.setUp()
        random = RandomStub()
        preferences.clear()
        service = ReminderService(jobs, random, taskDao)
    }

    @Test
    fun ignoreStaleSnoozeTime() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime()),
                with(SNOOZE_TIME, newDateTime().minusMinutes(5)),
                with(REMINDER_LAST, newDateTime().minusMinutes(4))
        )

        service.scheduleAlarm(task)

        assertTrue(jobs.isEmpty())
    }

    @Test
    fun dontIgnoreMissedSnoozeTime() {
        val dueDate = newDateTime()
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, dueDate),
                with(SNOOZE_TIME, dueDate.minusMinutes(4)),
                with(REMINDER_LAST, dueDate.minusMinutes(5)),
                with(REMINDERS, Task.NOTIFY_AT_DEADLINE))

        service.scheduleAlarm(task)

        verify(ReminderEntry(1, task.reminderSnooze, ReminderService.TYPE_SNOOZE))
    }

    @Test
    fun scheduleInitialRandomReminder() {
        random.seed = 0.3865f

        freezeClock {
            val now = newDateTime()
            val task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, null as DateTime?),
                    with(CREATION_TIME, now.minusDays(1)),
                    with(RANDOM_REMINDER_PERIOD, DateUtilities.ONE_WEEK))

            service.scheduleAlarm(task)

            verify(ReminderEntry(1L, now.minusDays(1).millis + 584206592, ReminderService.TYPE_RANDOM))
        }
    }

    @Test
    fun scheduleNextRandomReminder() {
        random.seed = 0.3865f

        freezeClock {
            val now = newDateTime()
            val task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, now.minusDays(1)),
                    with(CREATION_TIME, now.minusDays(30)),
                    with(RANDOM_REMINDER_PERIOD, DateUtilities.ONE_WEEK))

            service.scheduleAlarm(task)

            verify(ReminderEntry(1L, now.minusDays(1).millis + 584206592, ReminderService.TYPE_RANDOM))
        }
    }

    @Test
    fun scheduleOverdueRandomReminder() {
        random.seed = 0.3865f

        freezeClock {
            val now = newDateTime()
            val task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, now.minusDays(14)),
                    with(CREATION_TIME, now.minusDays(30)),
                    with(RANDOM_REMINDER_PERIOD, DateUtilities.ONE_WEEK))

            service.scheduleAlarm(task)

            verify(ReminderEntry(1L, now.millis + 10148400, ReminderService.TYPE_RANDOM))
        }
    }


    @Test
    fun snoozeOverridesAll() {
        val now = newDateTime()
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, now),
                with(SNOOZE_TIME, now.plusMonths(12)),
                with(REMINDERS, Task.NOTIFY_AT_DEADLINE or Task.NOTIFY_AFTER_DEADLINE),
                with(RANDOM_REMINDER_PERIOD, DateUtilities.ONE_HOUR))

        service.scheduleAlarm(task)

        verify(ReminderEntry(1, now.plusMonths(12).millis, ReminderService.TYPE_SNOOZE))
    }

    private fun verify(vararg reminders: ReminderEntry) = assertEquals(reminders.toList(), jobs.getJobs())

    internal class RandomStub : Random() {
        var seed = 1.0f

        override fun nextFloat() = seed
    }
}