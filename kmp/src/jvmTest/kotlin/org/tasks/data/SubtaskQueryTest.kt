package org.tasks.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.filters.SubtaskFilter
import org.tasks.preferences.SubtaskQueryPreferences

class SubtaskQueryTest {
    private fun taskList(isGoogleTasks: Boolean) =
        TaskListQuery.getQuery(SubtaskQueryPreferences(isGoogleTasks), SubtaskFilter(PARENT))

    @Test
    fun theTaskListsOwnQueryStillFoldsRowsAway() {
        listOf(true, false).forEach { isGoogleTasks ->
            val query = taskList(isGoogleTasks)

            assertTrue(query, query.contains(COLLAPSED))
        }
    }

    @Test
    fun theEditorsQueryReturnsWhatIsFoldedAway() {
        listOf(true, false).forEach { isGoogleTasks ->
            val query = subtaskQuery(parentId = PARENT, isGoogleTasks = isGoogleTasks)

            assertFalse(query, query.contains(COLLAPSED))
            assertTrue(query, query.contains(PARENT.toString()))
        }
    }

    companion object {
        private const val PARENT = 42L
        private const val COLLAPSED = "recursive_tasks.collapsed > 0"
    }
}
