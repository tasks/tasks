package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TaskListTest
import org.tasks.data.fetchTasks
import org.tasks.preferences.DefaultQueryPreferences

class ManualSortQueryTest : TaskListTest() {

    private suspend fun threeTasksInManualOrderBCA() {
        // Created (and, alphabetically, titled) A, B, C, but the manual `order` field —
        // the one other devices set by drag-to-reorder and sync over CalDAV — says B, C, A.
        val a = newTask("A")
        val b = newTask("B")
        val c = newTask("C")
        caldavDao.setTaskOrder(a.id, 3)
        caldavDao.setTaskOrder(b.id, 1)
        caldavDao.setTaskOrder(c.id, 2)
    }

    @Test
    fun manualSortListsTasksInTheOrderField() = runBlocking {
        threeTasksInManualOrderBCA()

        val preferences = DefaultQueryPreferences().apply { isManualSort = true }

        assertEquals(
            listOf("B", "C", "A"),
            taskDao.fetchTasks(preferences, filter).map { it.title },
        )
    }

    @Test
    fun withoutManualSortTasksAreNotListedInTheOrderField() = runBlocking {
        threeTasksInManualOrderBCA()

        // The default (auto) sort has no due dates or importance to break ties,
        // so the title is the effective order: A, B, C — not the manual order.
        val preferences = DefaultQueryPreferences()

        assertEquals(
            listOf("A", "B", "C"),
            taskDao.fetchTasks(preferences, filter).map { it.title },
        )
    }

    @Test
    fun manualSortOnlyAppliesToFiltersThatSupportIt() = runBlocking {
        threeTasksInManualOrderBCA()

        // MyTasksFilter does not support manual sort, so the manual flag is ignored
        // (the query falls through to the regular sort) and nothing crashes.
        val preferences = DefaultQueryPreferences().apply { isManualSort = true }

        assertEquals(
            listOf("A", "B", "C"),
            taskDao.fetchTasks(preferences, org.tasks.filters.MyTasksFilter(title = "My Tasks"))
                .map { it.title },
        )
    }
}
