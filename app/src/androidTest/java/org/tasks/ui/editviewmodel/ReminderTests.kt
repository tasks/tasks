package org.tasks.ui.editviewmodel

import androidx.test.annotation.UiThreadTest
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
    @UiThreadTest
    fun whenDueReminder() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.whenDue = true
        viewModel.save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyAtDeadline)
    }

    @Test
    @UiThreadTest
    fun whenOverDueReminder() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.whenOverdue = true
        viewModel.save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyAfterDeadline)
    }

    @Test
    @UiThreadTest
    fun ringFiveTimes() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.ringFiveTimes = true
        viewModel.save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeFive)
    }

    @Test
    @UiThreadTest
    fun ringNonstop() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.ringNonstop = true
        viewModel.save()

        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
    }

    @Test
    @UiThreadTest
    fun ringFiveTimesCantRingNonstop() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.ringNonstop = true
        viewModel.ringFiveTimes = true
        viewModel.save()

        assertFalse(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeFive)
    }

    @Test
    @UiThreadTest
    fun ringNonStopCantRingFiveTimes() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.ringFiveTimes = true
        viewModel.ringNonstop = true
        viewModel.save()

        assertFalse(taskDao.fetch(task.id)!!.isNotifyModeFive)
        assertTrue(taskDao.fetch(task.id)!!.isNotifyModeNonstop)
    }
}