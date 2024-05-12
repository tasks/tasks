package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.injection.ProductionModule
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TitleTests : BaseTaskEditViewModelTest() {
    @Test
    fun changeTitleCausesChange() {
        setup(newTask())

        viewModel.title = "Test"

        assertTrue(viewModel.hasChanges())
    }

    @Test
    fun saveWithEmptyTitle() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.priority.value = HIGH

        save()

        assertEquals("(No title)", taskDao.fetch(task.id)!!.title)
    }

    @Test
    fun newTaskPrepopulatedWithTitleHasChanges() {
        setup(newTask(with(TaskMaker.TITLE, "some title")))

        assertTrue(viewModel.hasChanges())
    }
}