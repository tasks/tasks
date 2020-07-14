package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class DueDateTests : BaseTaskEditViewModelTest() {
    @Test
    fun adjustHideUntilWhenChangingDate() {
        val task = newTask(
                with(TaskMaker.DUE_TIME, DateTime(2020, 7, 14, 16, 30, 0, 0)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE_TIME))
        setup(task)

        val newDueDate = viewModel.dueDate!! + TimeUnit.DAYS.toMillis(1)
        viewModel.dueDate = newDueDate

        assertEquals(newDueDate, viewModel.hideUntil)
    }
}