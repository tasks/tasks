package com.todoroo.astrid.alarms

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
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
import org.tasks.injection.ProductionModule
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
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
                        type = Alarm.TYPE_REL_END
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