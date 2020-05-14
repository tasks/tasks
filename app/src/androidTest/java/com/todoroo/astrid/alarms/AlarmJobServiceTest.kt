package com.todoroo.astrid.alarms

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.data.Alarm
import org.tasks.data.AlarmDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.jobs.AlarmEntry
import org.tasks.jobs.NotificationQueue
import org.tasks.makers.TaskMaker.REMINDER_LAST
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class AlarmJobServiceTest : InjectingTestCase() {
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var jobs: NotificationQueue
    @Inject lateinit var alarmService: AlarmService

    @Test
    fun scheduleAlarm() {
        val task = newTask()
        taskDao.createNew(task)
        val alarmTime = DateTime(2017, 9, 24, 19, 57)
        val alarm = Alarm(task.id, alarmTime.millis)
        alarm.id = alarmDao.insert(alarm)
        alarmService.scheduleAllAlarms()

        assertEquals(listOf(AlarmEntry(alarm)), jobs.getJobs())
    }

    @Test
    fun ignoreStaleAlarm() {
        val alarmTime = DateTime(2017, 9, 24, 19, 57)
        val task = newTask(with(REMINDER_LAST, alarmTime.endOfMinute()))
        taskDao.createNew(task)
        alarmDao.insert(Alarm(task.id, alarmTime.millis))
        alarmService.scheduleAllAlarms()

        assertTrue(jobs.getJobs().isEmpty())
    }

    override fun inject(component: TestComponent) = component.inject(this)
}