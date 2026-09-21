package org.tasks.viewmodel

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TaskListTest
import org.tasks.data.fetchTasks
import org.tasks.preferences.DefaultQueryPreferences
import org.tasks.preferences.QueryPreferences

class ClearCompletedTest : TaskListTest() {
    private suspend fun toClear(preferences: QueryPreferences): Set<String?> =
        taskDao.completedTasksToClear(preferences, filter).map { it.title }.toSet()

    private suspend fun listed(preferences: QueryPreferences): Set<String?> =
        taskDao.fetchTasks(preferences, filter)
            .filter { it.isCompleted }
            .map { it.title }
            .toSet()

    private fun listPreferences(showCompletedSubtasks: Boolean) =
        DefaultQueryPreferences().apply {
            this.showCompleted = false
            this.showCompletedSubtasks = showCompletedSubtasks
        }

    @Test
    fun clearsACompletedSubtaskThatTheListIsHiding() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE_CHILD, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)

        assertEquals(
            setOf(DONE_CHILD, DONE_ROOT),
            toClear(listPreferences(showCompletedSubtasks = false)),
        )
    }

    @Test
    fun clearsTheSameTasksWhicheverWayTheToggleIsSet() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE_CHILD, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)

        assertEquals(
            toClear(listPreferences(showCompletedSubtasks = true)),
            toClear(listPreferences(showCompletedSubtasks = false)),
        )
    }

    @Test
    fun theListItselfStillHidesWhatTheToggleSaysToHide() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE_CHILD, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)

        assertEquals(
            setOf(DONE_ROOT),
            listed(DefaultQueryPreferences().apply { showCompletedSubtasks = false }),
        )
    }

    @Test
    fun clearsACompletedSubtaskUnderAFoldedParent() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE_CHILD, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)
        taskDao.setCollapsed(listOf(parent.id), true)

        assertEquals(setOf(DONE_ROOT), listed(DefaultQueryPreferences()))
        assertEquals(setOf(DONE_CHILD, DONE_ROOT), toClear(DefaultQueryPreferences()))
    }

    @Test
    fun clearsACompletedTaskUnderAFoldedAncestor() = runBlocking {
        val parent = newTask(PARENT)
        val child = newTask(CHILD, parent = parent.id)
        newTask(DONE_CHILD, parent = child.id, completed = true)
        taskDao.setCollapsed(listOf(parent.id), true)

        assertEquals(setOf(DONE_CHILD), toClear(DefaultQueryPreferences()))
    }

    companion object {
        private const val DONE_CHILD = "done-child"
    }
}
