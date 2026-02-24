@file:Suppress("ClassName")

package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.R
import org.tasks.data.dao.AlarmDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import javax.inject.Inject

@HiltAndroidTest
class Upgrade_14_13_Test : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var alarmDao: AlarmDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var upgrader: Upgrade_14_13

    @Before
    override fun setUp() {
        super.setUp()
        preferences.setBoolean(R.string.p_rmd_time_enabled, true)
    }

    @Test
    fun deleteAlarmsForAllDayTasksWhenSettingOff() = runBlocking {
        disableAllDayReminders()
        val taskId = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        alarmDao.insert(whenDue(taskId))

        upgrader.deleteAlarmsForAllDayTasks()

        assertTrue(alarmDao.getAlarms(taskId).isEmpty())
    }

    @Test
    fun preserveAlarmsForTimedTasksWhenSettingOff() = runBlocking {
        disableAllDayReminders()
        val taskId = taskDao.createNew(newTask(with(DUE_TIME, DateTime())))
        alarmDao.insert(whenDue(taskId))

        upgrader.deleteAlarmsForAllDayTasks()

        assertEquals(1, alarmDao.getAlarms(taskId).size)
    }

    @Test
    fun preserveAlarmsWhenSettingOn() = runBlocking {
        val taskId = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        alarmDao.insert(whenDue(taskId))

        upgrader.deleteAlarmsForAllDayTasks()

        assertEquals(1, alarmDao.getAlarms(taskId).size)
    }

    @Test
    fun deleteMultipleAlarmsForAllDayTask() = runBlocking {
        disableAllDayReminders()
        val taskId = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        alarmDao.insert(whenDue(taskId))
        alarmDao.insert(whenOverdue(taskId))

        upgrader.deleteAlarmsForAllDayTasks()

        assertTrue(alarmDao.getAlarms(taskId).isEmpty())
    }

    @Test
    fun deleteAlarmsForMultipleAllDayTasks() = runBlocking {
        disableAllDayReminders()
        val task1 = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        val task2 = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        alarmDao.insert(whenDue(task1))
        alarmDao.insert(whenDue(task2))

        upgrader.deleteAlarmsForAllDayTasks()

        assertTrue(alarmDao.getAlarms(task1).isEmpty())
        assertTrue(alarmDao.getAlarms(task2).isEmpty())
    }

    @Test
    fun preserveAlarmsForTasksWithNoDueDate() = runBlocking {
        disableAllDayReminders()
        val taskId = taskDao.createNew(newTask())
        alarmDao.insert(Alarm(task = taskId, type = TYPE_DATE_TIME, time = DateTime().millis))

        upgrader.deleteAlarmsForAllDayTasks()

        assertEquals(1, alarmDao.getAlarms(taskId).size)
    }

    @Test
    fun deleteStartAlarmForAllDayTask() = runBlocking {
        disableAllDayReminders()
        val taskId = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        alarmDao.insert(whenStarted(taskId))

        upgrader.deleteAlarmsForAllDayTasks()

        assertTrue(alarmDao.getAlarms(taskId).isEmpty())
    }

    @Test
    fun onlyDeleteAllDayTaskAlarms() = runBlocking {
        disableAllDayReminders()
        val allDay = taskDao.createNew(newTask(with(DUE_DATE, DateTime())))
        val timed = taskDao.createNew(newTask(with(DUE_TIME, DateTime())))
        alarmDao.insert(whenDue(allDay))
        alarmDao.insert(whenDue(timed))

        upgrader.deleteAlarmsForAllDayTasks()

        assertTrue(alarmDao.getAlarms(allDay).isEmpty())
        assertEquals(1, alarmDao.getAlarms(timed).size)
    }

    private fun disableAllDayReminders() {
        preferences.setBoolean(R.string.p_rmd_time_enabled, false)
    }
}
