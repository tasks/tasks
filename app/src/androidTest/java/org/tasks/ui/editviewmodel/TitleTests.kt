package org.tasks.ui.editviewmodel

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask

@HiltAndroidTest
class TitleTests : BaseTaskEditViewModelTest() {
    @Test
    fun changeTitleCausesChange() {
        setup(newTask())

        viewModel.setTitle("Test")

        assertTrue(viewModel.hasChanges())
    }

    @Test
    fun saveWithEmptyTitle() = runBlocking {
        val task = newTask()
        setup(task)

        viewModel.setPriority(HIGH)

        save()

        assertEquals("(No title)", taskDao.fetch(task.id)!!.title)
    }

    @Test
    fun newTaskPrepopulatedWithTitleHasChanges() {
        setup(newTask(with(TaskMaker.TITLE, "some title")))

        assertTrue(viewModel.hasChanges())
    }
}