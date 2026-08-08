package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TaskListTest
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class CompletedStartDateTest : TaskListTest() {
    @Test
    fun aCompletedTaskThatNeverStartedStaysInTheList() = runBlocking {
        newTask(UNSTARTED, completed = true, hideUntil = future)

        assertEquals(listOf(UNSTARTED), titles())
    }

    @Test
    fun anUnstartedTaskIsStillHiddenUntilItIsCompleted() = runBlocking {
        val task = newTask(UNSTARTED, hideUntil = future)

        assertEquals(emptyList<String>(), titles())

        taskDao.update(task.copy(completionDate = COMPLETED_AT))

        assertEquals(listOf(UNSTARTED), titles())
    }

    @Test
    fun aCompletedTaskThatNeverStartedGoesBackUnderItsParent() = runBlocking {
        val parent = newTask(PARENT)
        newTask(UNSTARTED, parent = parent.id, completed = true, hideUntil = future)

        assertEquals(listOf(PARENT, UNSTARTED), titles())
        assertEquals(1, rows().first { it.title == UNSTARTED }.indent)
        assertEquals(1, rows().first { it.title == PARENT }.children)
    }

    @Test
    fun hidingCompletedTasksHidesOneThatNeverStarted() = runBlocking {
        newTask(UNSTARTED, completed = true, hideUntil = future)

        assertEquals(emptyList<String>(), titles(showCompleted = false))
    }

    @Test
    fun hidingCompletedSubtasksHidesASubtaskThatNeverStarted() = runBlocking {
        val parent = newTask(PARENT)
        newTask(UNSTARTED, parent = parent.id, completed = true, hideUntil = future)

        assertEquals(listOf(PARENT), titles(showCompletedSubtasks = false))
        assertEquals(0, rows(showCompletedSubtasks = false).single().children)
    }

    companion object {
        private const val UNSTARTED = "unstarted"
        private val future: Long
            get() = currentTimeMillis() + 86_400_000L
    }
}
