package com.todoroo.astrid.alarms

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.jobs.AlarmEntry
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.REMINDER_LAST
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class AlarmJobServiceTest : InjectingTestCase() {
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmService: AlarmService

    @Test
    fun scheduleAlarm() = runBlocking {
        val task = taskDao.createNew(newTask())
        val alarm = insertAlarm(Alarm(task, DateTime(2017, 9, 24, 19, 57).millis, TYPE_DATE_TIME))

        verify(overdue = listOf(AlarmEntry(alarm, task, DateTime(2017, 9, 24, 19, 57).millis, TYPE_DATE_TIME)))
    }

    @Test
    fun ignoreStaleAlarm() = runBlocking {
        val alarmTime = DateTime(2017, 9, 24, 19, 57)
        val task = taskDao.createNew(newTask(with(REMINDER_LAST, alarmTime.endOfMinute())))
        alarmDao.insert(Alarm(task, alarmTime.millis, TYPE_DATE_TIME))

        verify()
    }

    @Test
    fun dontScheduleReminderForCompletedTask() = runBlocking {
        val task = taskDao.insert(
            newTask(
                with(DUE_DATE, newDateTime()),
                with(COMPLETION_TIME, newDateTime())
            )
        )
        alarmDao.insert(whenDue(task))

        verify()
    }

    @Test
    fun dontScheduleReminderForDeletedTask() = runBlocking {
        val task = taskDao.insert(
            newTask(
                with(DUE_DATE, newDateTime()),
                with(DELETION_TIME, newDateTime())
            )
        )
        alarmDao.insert(whenDue(task))

        verify()
    }

    @Test
    fun snoozeOverridesAll() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, now)))

        alarmDao.insert(whenDue(task))
        alarmDao.insert(whenOverdue(task))
        alarmDao.insert(Alarm(task, DateUtilities.ONE_HOUR, TYPE_RANDOM))
        val alarm = alarmDao.insert(Alarm(task, now.plusMonths(12).millis, TYPE_SNOOZE))

        verify(future = listOf(AlarmEntry(alarm, task, now.plusMonths(12).millis, TYPE_SNOOZE)))
    }

    private suspend fun insertAlarm(alarm: Alarm): Long {
        alarm.id = alarmDao.insert(alarm)
        return alarm.id
    }

    private suspend fun verify(
        overdue: List<AlarmEntry> = emptyList(),
        future: List<AlarmEntry> = emptyList(),
    ) {
        val (actualOverdue, actualFuture) = alarmService.getAlarms()

        assertEquals(overdue, actualOverdue)
        assertEquals(future, actualFuture)
    }
}