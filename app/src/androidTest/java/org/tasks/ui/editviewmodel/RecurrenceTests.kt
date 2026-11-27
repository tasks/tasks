package org.tasks.ui.editviewmodel

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.tasks.data.entity.Task
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.noon

@HiltAndroidTest
class RecurrenceTests : BaseTaskEditViewModelTest() {
    @Test
    fun recurrenceGivesUndatedTaskAllDayDueDateAtNoonToday() {
        setup(newTask())

        viewModel.setRecurrence("FREQ=DAILY")

        val dueDate = viewModel.viewState.value.task.dueDate
        assertEquals(currentTimeMillis().noon(), dueDate)
        assertFalse(Task.hasDueTime(dueDate))
    }
}
