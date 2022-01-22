package com.todoroo.astrid.alarms

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_DUE
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_DUE_TIME
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.data.AlarmDao
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.NotificationQueue
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DELETION_TIME
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.REMINDER_LAST
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class AlarmJobServiceTest : InjectingTestCase() {
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var jobs: NotificationQueue
    @Inject lateinit var alarmService: AlarmService

    @Test
    fun scheduleAlarm() = runBlocking {
        val task = taskDao.createNew(newTask())
        val alarm = insertAlarm(Alarm(task, DateTime(2017, 9, 24, 19, 57).millis))

        verify(AlarmEntry(alarm, task, DateTime(2017, 9, 24, 19, 57).millis))
    }

    @Test
    fun ignoreStaleAlarm() = runBlocking {
        val alarmTime = DateTime(2017, 9, 24, 19, 57)
        val task = taskDao.createNew(newTask(with(REMINDER_LAST, alarmTime.endOfMinute())))
        alarmDao.insert(Alarm(task, alarmTime.millis))

        verify()
    }

    @Test
    fun scheduleReminderAtDefaultDue() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_DATE, now)))
        val alarm = alarmDao.insert(whenDue(task))

        verify(AlarmEntry(alarm, task, now.startOfDay().withHourOfDay(18).millis))
    }

    @Test
    fun scheduleReminderAtDefaultDueTime() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, now)))
        val alarm = alarmDao.insert(whenDue(task))

        verify(AlarmEntry(alarm, task, now.startOfMinute().millis + 1000))
    }

    @Test
    fun scheduleReminderAtDefaultStart() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_DATE, now), with(HIDE_TYPE, HIDE_UNTIL_DUE)))
        val alarm = alarmDao.insert(whenStarted(task))

        verify(AlarmEntry(alarm, task, now.startOfDay().withHourOfDay(18).millis))
    }

    @Test
    fun scheduleReminerAtDefaultStartTime() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, now), with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME)))
        val alarm = alarmDao.insert(whenStarted(task))

        verify(AlarmEntry(alarm, task, now.startOfMinute().millis + 1000))
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
    fun scheduleRelativeAfterDue() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_DATE, now)))
        val alarm = alarmDao.insert(Alarm(task, TimeUnit.DAYS.toMillis(1), Alarm.TYPE_REL_END))

        verify(AlarmEntry(alarm, task, now.plusDays(1).startOfDay().withHourOfDay(18).millis))
    }

    @Test
    fun scheduleRelativeAfterDueTime() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, now)))
        val alarm = alarmDao.insert(Alarm(task, TimeUnit.DAYS.toMillis(1), Alarm.TYPE_REL_END))

        verify(AlarmEntry(alarm, task, now.plusDays(1).startOfMinute().millis + 1000))
    }

    @Test
    fun scheduleRelativeAfterStart() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_DATE, now), with(HIDE_TYPE, HIDE_UNTIL_DUE)))
        val alarm = alarmDao.insert(Alarm(task, TimeUnit.DAYS.toMillis(1), Alarm.TYPE_REL_START))

        verify(AlarmEntry(alarm, task, now.plusDays(1).startOfDay().withHourOfDay(18).millis))
    }

    @Test
    fun scheduleRelativeAfterStartTime() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, now), with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME)))
        val alarm = alarmDao.insert(Alarm(task, TimeUnit.DAYS.toMillis(1), Alarm.TYPE_REL_START))

        verify(AlarmEntry(alarm, task, now.plusDays(1).startOfMinute().millis + 1000))
    }

    @Test
    fun scheduleFirstRepeatReminder() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(4)))
        )
        val alarm = alarmDao.insert(Alarm(task, 0, Alarm.TYPE_REL_END, 1, TimeUnit.MINUTES.toMillis(5)))

        verify(AlarmEntry(alarm, task, now.plusMinutes(5).startOfMinute().millis + 1000))
    }

    @Test
    fun scheduleSecondRepeatReminder() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(6)))
        )
        val alarm = alarmDao.insert(Alarm(task, 0, Alarm.TYPE_REL_END, 2, TimeUnit.MINUTES.toMillis(5)))

        verify(AlarmEntry(alarm, task, now.plusMinutes(10).startOfMinute().millis + 1000))
    }

    @Test
    fun terminateRepeatReminder() = runBlocking {
        val now = Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, now()).toDateTime()
        val task = taskDao.insert(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(10)))
        )
        alarmDao.insert(Alarm(task, 0, Alarm.TYPE_REL_END, 2, TimeUnit.MINUTES.toMillis(5)))

        verify()
    }

    @Test
    fun dontScheduleRelativeEndWithNoEnd() = runBlocking {
        val task = taskDao.insert(newTask())
        alarmDao.insert(whenDue(task))

        verify()
    }

    @Test
    fun dontScheduleRelativeStartWithNoStart() = runBlocking {
        val now = newDateTime()
        val task = taskDao.insert(newTask(with(DUE_DATE, now)))
        alarmDao.insert(whenStarted(task))

        verify()
    }

    @Test
    fun reminderOverdueEveryDay() = runBlocking {
        val dueDate = Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis).toDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, dueDate), with(REMINDER_LAST, dueDate.plusDays(6))))
        val alarm = alarmDao.insert(whenOverdue(task))

        verify(AlarmEntry(alarm, task, dueDate.plusDays(7).millis))
    }

    @Test
    fun endDailyOverdueReminder() = runBlocking {
        val dueDate = Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis).toDateTime()
        val task = taskDao.insert(newTask(with(DUE_TIME, dueDate), with(REMINDER_LAST, dueDate.plusDays(7))))
        alarmDao.insert(whenOverdue(task))

        verify()
    }

    private suspend fun insertAlarm(alarm: Alarm): Long {
        alarm.id = alarmDao.insert(alarm)
        return alarm.id
    }

    private suspend fun verify(vararg alarms: AlarmEntry) {
        alarmService.scheduleAllAlarms()

        assertEquals(alarms.toList(), jobs.getJobs())
    }
}