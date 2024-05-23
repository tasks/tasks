package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.service.TaskCreator.Companion.setDefaultReminders
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.R
import org.tasks.data.createDueDate
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.data.entity.Task
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.START_DATE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class ReminderTests : BaseTaskEditViewModelTest() {
    @Test
    fun whenStartReminder() = runBlocking {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(Task.NOTIFY_AT_START.toString())
        )
        val task = newTask(with(START_DATE, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            listOf(Alarm(type = Alarm.TYPE_REL_START)),
            viewModel.selectedAlarms.value
        )
    }

    @Test
    fun whenDueReminder() = runBlocking {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(Task.NOTIFY_AT_DEADLINE.toString())
        )
        val task = newTask(with(DUE_TIME, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            listOf(Alarm(type = Alarm.TYPE_REL_END)),
            viewModel.selectedAlarms.value
        )
    }

    @Test
    fun whenOverDueReminder() = runBlocking {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(Task.NOTIFY_AFTER_DEADLINE.toString())
        )
        val task = newTask(with(DUE_TIME, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            listOf(whenOverdue(0)),
            viewModel.selectedAlarms.value
        )
    }

    @Test
    fun ringFiveTimes() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.ringFiveTimes = true

        save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeFive)
    }

    @Test
    fun ringNonstop() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.ringNonstop = true

        save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
    }

    @Test
    fun ringFiveTimesCantRingNonstop() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.ringNonstop = true
        viewModel.ringFiveTimes = true

        save()

        assertFalse(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeFive)
    }

    @Test
    fun ringNonStopCantRingFiveTimes() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.ringFiveTimes = true
        viewModel.ringNonstop = true

        save()

        assertFalse(taskDao.fetch(task.id)!!.isNotifyModeFive)
        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
    }

    @Test
    fun noDefaultRemindersWithNoDates() = runBlocking {
        val task = newTask()
        task.setDefaultReminders(preferences)

        setup(task)

        save()

        assertTrue(alarmDao.getAlarms(task.id).isEmpty())
    }

    @Test
    fun addDefaultRemindersWhenAddingDueDate() = runBlocking {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(
                Task.NOTIFY_AT_DEADLINE.toString(),
                Task.NOTIFY_AFTER_DEADLINE.toString(),
            )
        )
        val task = newTask()
        setup(task)

        viewModel.setDueDate(
            createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                currentTimeMillis()
            )
        )

        save()

        assertEquals(
            listOf(whenDue(1).copy(id = 1), whenOverdue(1).copy(id = 2)),
            alarmDao.getAlarms(task.id)
        )
    }

    @Test
    fun addDefaultRemindersWhenAddingStartDate() = runBlocking {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            hashSetOf(Task.NOTIFY_AT_START.toString())
        )
        val task = newTask()
        setup(task)

        viewModel.setStartDate(
            createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                currentTimeMillis()
            )
        )

        save()

        assertEquals(
            listOf(whenStarted(1).copy(id = 1)),
            alarmDao.getAlarms(task.id)
        )
    }
}
