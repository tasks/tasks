package com.todoroo.astrid.alarms

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.data.createDueDate
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
class AlarmJobServiceTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmService: AlarmService

    @Test
    fun testNoAlarms() = runBlocking {
        testResults(emptyList(), 0)
    }

    @Test
    fun futureAlarmWithNoPastAlarm() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 18).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(1, mutableSetOf(Alarm(type = Alarm.TYPE_REL_END)))

            testResults(emptyList(), DateTime(2024, 5, 18, 18, 0).millis)
        }
    }

    @Test
    fun pastAlarmWithNoFutureAlarm() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(1, mutableSetOf(Alarm(type = Alarm.TYPE_REL_END)))

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_REL_END
                    )
                ),
                0
            )
        }
    }

    @Test
    fun pastRecurringAlarmWithFutureRecurrence() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 1,
                        interval = TimeUnit.HOURS.toMillis(6)
                    )
                )
            )

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_REL_END
                    )
                ),
                DateTime(2024, 5, 18, 0, 0).millis
            )
        }
    }

    @Test
    fun pastAlarmsRemoveSnoozed() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(type = Alarm.TYPE_REL_END),
                    Alarm(time = DateTimeUtils2.currentTimeMillis(), type = Alarm.TYPE_SNOOZE)
                )
            )

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE
                    )
                ),
                0
            )

            assertEquals(
                listOf(Alarm(id = 1, task = 1, time = 0, type = Alarm.TYPE_REL_END)),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun alarmsOneMinuteApart() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 20).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(1, mutableSetOf(Alarm(type = Alarm.TYPE_REL_END)))
            taskDao.insert(Task())
            alarmService.synchronizeAlarms(
                taskId = 2,
                alarms = mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_SNOOZE,
                        time = DateTime(2024, 5, 17, 23, 21).millis)
                )
            )

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_REL_END
                    )
                ),
                DateTime(2024, 5, 17, 23, 21).millis
            )
        }
    }

    @Test
    fun futureSnoozeOverrideOverdue() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(type = Alarm.TYPE_REL_END),
                    Alarm(
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5),
                        type = Alarm.TYPE_SNOOZE
                    )
                )
            )

            testResults(
                emptyList(),
                DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
            )
        }
    }

    @Test
    fun snoozePreservesRepeatingReminderPattern() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 20).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                )
            )
            taskDao.setLastNotified(1, DateTime(DateTimeUtils2.currentTimeMillis()).endOfMinute().millis)

            alarmService.snooze(DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1), listOf(1))

            assertEquals(
                listOf(
                    Alarm(
                        id = 1,
                        task = 1,
                        time = 0,
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    ),
                    Alarm(
                        id = 2,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun snoozingRepeatingSnoozeResetsPatternFromBaseReminder() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 20).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    ),
                    Alarm(
                        time = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 1,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                )
            )
            taskDao.setLastNotified(1, DateTime(DateTimeUtils2.currentTimeMillis()).endOfMinute().millis)

            alarmService.snooze(DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1), listOf(1))

            assertEquals(
                listOf(
                    Alarm(
                        id = 1,
                        task = 1,
                        time = 0,
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    ),
                    Alarm(
                        id = 3,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun snoozingExistingSnoozeFallsBackToSnoozeTemplateWhenBaseMissing() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(Task())
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        time = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                )
            )

            alarmService.snooze(
                DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                listOf(1)
            )

            assertEquals(
                listOf(
                    Alarm(
                        id = 2,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun snoozingAfterRepeatingReminderFinishesPreservesPattern() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 30)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 20).millis
                    ),
                    reminderLast = DateTime(2024, 5, 17, 23, 25, 30).endOfMinute().millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                )
            )

            alarmService.snooze(
                DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                listOf(1)
            )

            assertEquals(
                listOf(
                    Alarm(
                        id = 1,
                        task = 1,
                        time = 0,
                        type = Alarm.TYPE_REL_END,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    ),
                    Alarm(
                        id = 2,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun snoozingAfterChainFinishesUsesMostRecentBaseTemplate() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 30)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 20).millis
                    ),
                    reminderLast = DateTime(2024, 5, 17, 23, 25, 30).endOfMinute().millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(type = Alarm.TYPE_REL_END),
                    Alarm(
                        time = TimeUnit.MINUTES.toMillis(1),
                        type = Alarm.TYPE_REL_END,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                )
            )

            alarmService.snooze(
                DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                listOf(1)
            )

            assertEquals(
                listOf(
                    Alarm(id = 1, task = 1, time = 0, type = Alarm.TYPE_REL_END),
                    Alarm(
                        id = 2,
                        task = 1,
                        time = TimeUnit.MINUTES.toMillis(1),
                        type = Alarm.TYPE_REL_END,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    ),
                    Alarm(
                        id = 3,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun repeatingSnoozeSchedulesNextOccurrence() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    )
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    ),
                    Alarm(
                        time = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                )
            )

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE
                    )
                ),
                DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
            )

            assertEquals(
                listOf(
                    Alarm(
                        id = 1,
                        task = 1,
                        time = 0,
                        type = Alarm.TYPE_REL_END,
                        repeat = 2,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    ),
                    Alarm(
                        id = 2,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 1,
                        interval = TimeUnit.MINUTES.toMillis(5)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun overdueSnoozeOverridesOverdueBaseReminder() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 25)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(2024, 5, 17, 23, 10).millis
                    ),
                    reminderLast = DateTime(2024, 5, 17, 23, 11).millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(
                    Alarm(
                        type = Alarm.TYPE_REL_END,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    ),
                    Alarm(
                        time = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                )
            )

            testResults(
                listOf(
                    Notification(
                        taskId = 1L,
                        timestamp = DateTimeUtils2.currentTimeMillis(),
                        type = Alarm.TYPE_SNOOZE
                    )
                ),
                DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)
            )

            assertEquals(
                listOf(
                    Alarm(
                        id = 1,
                        task = 1,
                        time = 0,
                        type = Alarm.TYPE_REL_END,
                        repeat = 5,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    ),
                    Alarm(
                        id = 2,
                        task = 1,
                        time = DateTimeUtils2.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1),
                        type = Alarm.TYPE_SNOOZE,
                        repeat = 4,
                        interval = TimeUnit.MINUTES.toMillis(1)
                    )
                ),
                alarmService.getAlarms(1)
            )
        }
    }

    @Test
    fun ignoreStaleAlarm() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    ),
                    reminderLast = DateTime(2024, 5, 17, 18, 0).millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(Alarm(type = Alarm.TYPE_REL_END))
            )

            testResults(
                emptyList(),
                0
            )
        }
    }

    @Test
    fun dontScheduleForCompletedTask() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    ),
                    completionDate = DateTime(2024, 5, 17, 14, 0).millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(Alarm(type = Alarm.TYPE_REL_END))
            )

            testResults(
                emptyList(),
                0
            )
        }
    }

    @Test
    fun dontScheduleForDeletedTask() = runBlocking {
        freezeAt(DateTime(2024, 5, 17, 23, 20)) {
            taskDao.insert(
                Task(
                    dueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime(2024, 5, 17).millis
                    ),
                    deletionDate = DateTime(2024, 5, 17, 14, 0).millis,
                )
            )
            alarmService.synchronizeAlarms(
                1,
                mutableSetOf(Alarm(type = Alarm.TYPE_REL_END))
            )

            testResults(
                emptyList(),
                0
            )
        }
    }

    private suspend fun testResults(notifications: List<Notification>, nextAlarm: Long) {
        val actualNextAlarm = alarmService.triggerAlarms {
            assertEquals(notifications, it)
            it.forEach { taskDao.setLastNotified(it.taskId, DateTimeUtils2.currentTimeMillis()) }
        }
        assertEquals(nextAlarm, actualNextAlarm)
    }
}
