package org.tasks.ui.editviewmodel

import androidx.test.annotation.UiThreadTest
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task.Priority.Companion.HIGH
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
        viewModel.setup(newTask())

        viewModel.title = "Test"

        assertTrue(viewModel.hasChanges())
    }

    @Test
    @UiThreadTest
    fun saveWithEmptyTitle() = runBlocking {
        val task = newTask()
        viewModel.setup(task)

        viewModel.priority = HIGH

        viewModel.save()

        assertEquals("(No title)", taskDao.fetch(task.id)!!.title)
    }

    @Test
    @UiThreadTest
    fun newTaskPrepopulatedWithTitleHasChanges() {
        viewModel.setup(newTask(with(TaskMaker.TITLE, "some title")))

        assertTrue(viewModel.hasChanges())
    }
}