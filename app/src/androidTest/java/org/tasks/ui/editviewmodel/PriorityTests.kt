package org.tasks.ui.editviewmodel

import androidx.test.annotation.UiThreadTest
import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class PriorityTests : BaseTaskEditViewModelTest() {
    @Test
    fun changePriorityCausesChange() {
        viewModel.setup(TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)))

        viewModel.priority = Task.Priority.MEDIUM

        Assert.assertTrue(viewModel.hasChanges())
    }

    @Test
    @UiThreadTest
    fun applyPriorityChange() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH))
        viewModel.setup(task)
        viewModel.priority = Task.Priority.MEDIUM

        viewModel.save()

        Assert.assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun noChangeWhenRevertingPriority() {
        viewModel.setup(TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)))

        viewModel.priority = Task.Priority.MEDIUM
        viewModel.priority = Task.Priority.HIGH

        Assert.assertFalse(viewModel.hasChanges())
    }
}