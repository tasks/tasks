package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.service.TaskCreator.Companion.setDefaultReminders
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.persistentSetOf
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
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.START_DATE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@HiltAndroidTest
class ReminderTests : BaseTaskEditViewModelTest() {
    @Test
    fun whenStartReminder() = runBlocking {
        preferences.setDefaultAlarms(listOf(whenStarted(0)))
        val task = newTask(with(START_DATE, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            persistentSetOf(Alarm(type = Alarm.TYPE_REL_START)),
            viewModel.viewState.value.alarms
        )
    }

    @Test
    fun whenDueReminder() = runBlocking {
        preferences.setDefaultAlarms(listOf(whenDue(0)))
        val task = newTask(with(DUE_TIME, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            persistentSetOf(Alarm(type = Alarm.TYPE_REL_END)),
            viewModel.viewState.value.alarms
        )
    }

    @Test
    fun whenOverDueReminder() = runBlocking {
        preferences.setDefaultAlarms(listOf(whenOverdue(0)))
        val task = newTask(with(DUE_TIME, DateTime()))
        task.setDefaultReminders(preferences)

        setup(task)

        assertEquals(
            persistentSetOf(whenOverdue(0)),
            viewModel.viewState.value.alarms
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
        val task = setupWithDefaultAlarms(whenDue(0), whenOverdue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        save()

        assertEquals(
            listOf(whenDue(1).copy(id = 1), whenOverdue(1).copy(id = 2)),
            alarmDao.getAlarms(task.id)
        )
    }

    @Test
    fun addDefaultRemindersWhenAddingStartDate() = runBlocking {
        val task = setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        save()

        assertEquals(
            listOf(whenStarted(1).copy(id = 1)),
            alarmDao.getAlarms(task.id)
        )
    }

    @Test
    fun addRemindersWhenAddingAllDayDueDate() = runBlocking {
        val task = setupWithDefaultAlarms(whenDue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, currentTimeMillis())
        )

        assertAlarms(whenDue(0).copy(task = task.id))
    }

    @Test
    fun dontAddRemindersWhenAddingAllDayDueDateToggleOff() = runBlocking {
        disableAllDayReminders()
        setupWithDefaultAlarms(whenDue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, currentTimeMillis())
        )

        assertNoAlarms()
    }

    @Test
    fun addRemindersWhenAddingTimedDueDateToggleOff() = runBlocking {
        disableAllDayReminders()
        val task = setupWithDefaultAlarms(whenDue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        assertAlarms(whenDue(0).copy(task = task.id))
    }

    @Test
    fun addRemindersWhenChangingAllDayToTimedToggleOff() = runBlocking {
        disableAllDayReminders()
        val task = setupWithDefaultAlarms(whenDue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, currentTimeMillis())
        )
        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        assertAlarms(whenDue(0).copy(task = task.id))
    }

    @Test
    fun dontAddRemindersWhenChangingAllDayToTimedToggleOn() = runBlocking {
        val task = setupWithDefaultAlarms(whenDue(0))

        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, currentTimeMillis())
        )
        viewModel.setDueDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        // reminders were already added when going from no date -> all day
        assertAlarms(whenDue(0).copy(task = task.id))
    }

    @Test
    fun addRemindersWhenAddingAllDayStartDate() = runBlocking {
        val task = setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(DateTime().startOfDay().millis)

        assertAlarms(whenStarted(0).copy(task = task.id))
    }

    @Test
    fun dontAddRemindersWhenAddingAllDayStartDateToggleOff() = runBlocking {
        disableAllDayReminders()
        setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(DateTime().startOfDay().millis)

        assertNoAlarms()
    }

    @Test
    fun addRemindersWhenAddingTimedStartDateToggleOff() = runBlocking {
        disableAllDayReminders()
        val task = setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        assertAlarms(whenStarted(0).copy(task = task.id))
    }

    @Test
    fun addRemindersWhenChangingAllDayToTimedStartDateToggleOff() = runBlocking {
        disableAllDayReminders()
        val task = setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(DateTime().startOfDay().millis)
        viewModel.setStartDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        assertAlarms(whenStarted(0).copy(task = task.id))
    }

    @Test
    fun dontAddRemindersWhenChangingAllDayToTimedStartDateToggleOn() = runBlocking {
        val task = setupWithDefaultAlarms(whenStarted(0))

        viewModel.setStartDate(DateTime().startOfDay().millis)
        viewModel.setStartDate(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, currentTimeMillis())
        )

        // reminders were already added when going from no date -> all day
        assertAlarms(whenStarted(0).copy(task = task.id))
    }

    private fun disableAllDayReminders() {
        preferences.setBoolean(R.string.p_rmd_time_enabled, false)
    }

    private fun setupWithDefaultAlarms(vararg alarms: Alarm): Task {
        preferences.setDefaultAlarms(alarms.toList())
        val task = newTask()
        setup(task)
        return task
    }

    private fun assertAlarms(vararg alarms: Alarm) {
        assertEquals(persistentSetOf(*alarms), viewModel.viewState.value.alarms)
    }

    private fun assertNoAlarms() {
        assertEquals(persistentSetOf<Alarm>(), viewModel.viewState.value.alarms)
    }
}
