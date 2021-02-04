package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class RepeatTests : BaseTaskEditViewModelTest() {
    @Test
    fun changeRepeatAfterCompletion() = runBlocking {
        val task = newTask(with(TaskMaker.RECUR, "RRULE:FREQ=DAILY;INTERVAL=1"))
        setup(task)

        viewModel.repeatAfterCompletion = true

        save()

        assertEquals(
                "RRULE:FREQ=DAILY;INTERVAL=1;FROM=COMPLETION",
                taskDao.fetch(task.id)!!.recurrence)
    }

    @Test
    fun removeRepeatAfterCompletion() = runBlocking {
        val task = newTask()
        task.recurrence = "RRULE:FREQ=DAILY;INTERVAL=1;FROM=COMPLETION"
        setup(task)

        viewModel.repeatAfterCompletion = false

        save()

        assertEquals(
                "RRULE:FREQ=DAILY;INTERVAL=1",
                taskDao.fetch(task.id)!!.recurrence)
    }
}