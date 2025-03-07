package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Test
import org.tasks.data.entity.Task
import org.tasks.makers.TaskMaker

@HiltAndroidTest
class PriorityTests : BaseTaskEditViewModelTest() {
    @Test
    fun changePriorityCausesChange() {
        setup(TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)))

        viewModel.setPriority(Task.Priority.MEDIUM)

        Assert.assertTrue(viewModel.hasChanges())
    }

    @Test
    fun applyPriorityChange() {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH))
        setup(task)
        viewModel.setPriority(Task.Priority.MEDIUM)

        save()

        Assert.assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun noChangeWhenRevertingPriority() {
        setup(TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)))

        viewModel.setPriority(Task.Priority.MEDIUM)
        viewModel.setPriority(Task.Priority.HIGH)

        Assert.assertFalse(viewModel.hasChanges())
    }
}