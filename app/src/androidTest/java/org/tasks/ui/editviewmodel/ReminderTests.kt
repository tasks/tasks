package org.tasks.ui.editviewmodel

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker.newTask

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class ReminderTests : BaseTaskEditViewModelTest() {
    @Test
    fun whenDueReminder() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.whenDue = true

        save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyAtDeadline)
    }

    @Test
    fun whenOverDueReminder() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.whenOverdue = true

        save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyAfterDeadline)
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
}