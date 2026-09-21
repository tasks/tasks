package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.entity.Task
import org.tasks.filters.TodayFilter
import org.tasks.preferences.DefaultQueryPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class NonRecursiveQueryTest : DatabaseTest() {
    private val taskDao = db.taskDao()

    private val filter = TodayFilter(title = "Today")

    private suspend fun newTask(
        title: String,
        completed: Boolean = false,
        hideUntil: Long = 0,
    ): Task {
        val task = Task(
            title = title,
            dueDate = currentTimeMillis(),
            hideUntil = hideUntil,
            completionDate = if (completed) currentTimeMillis() else 0,
        )
        taskDao.createNew(task)
        return task
    }

    private suspend fun rows() = taskDao.fetchTasks(
        TaskListQuery.getQuery(
            DefaultQueryPreferences().apply {
                isAstridSort = true
                completedTasksAtBottom = true
            },
            filter,
        )
    )

    @Test
    fun aCompletedTaskIsFlaggedAsTheCompletedSection() = runBlocking {
        newTask(TODO)
        newTask(DONE, completed = true)

        val rows = rows().associateBy { it.title }
        assertEquals(setOf(TODO, DONE), rows.keys)
        assertFalse(rows.getValue(TODO).parentComplete)
        assertTrue(rows.getValue(DONE).parentComplete)
    }

    @Test
    fun completedTasksAreParkedAtTheBottom() = runBlocking {
        newTask(DONE, completed = true)
        newTask(TODO)

        assertEquals(listOf(TODO, DONE), rows().map { it.title })
    }

    @Test
    fun aCompletedTaskThatNeverStartedIsStillListed() = runBlocking {
        newTask(DONE, completed = true, hideUntil = currentTimeMillis() + 86_400_000L)

        assertEquals(listOf(DONE), rows().map { it.title })
    }

    companion object {
        private const val TODO = "todo"
        private const val DONE = "done"
    }
}
