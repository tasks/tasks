package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TaskListTest
import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Task
import org.tasks.data.sql.Criterion
import org.tasks.data.sql.QueryTemplate
import org.tasks.filters.FilterImpl

class SubtaskMatchingFilterTest : TaskListTest() {
    override val filter = FilterImpl(sql = QueryTemplate().where(activeAndVisible()).toString())

    private val dueFilter = FilterImpl(
        sql = QueryTemplate()
            .where(Criterion.and(activeAndVisible(), Task.DUE_DATE.gt(0)))
            .toString()
    )

    @Test
    fun anIncompleteSubtaskSortsWithItsParent() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, dueDate = CHILD_DUE)

        assertEquals(1, row(CHILD).indent)
        assertEquals(row(PARENT).primarySort, row(CHILD).primarySort)
        assertEquals(row(PARENT).sortGroup, row(CHILD).sortGroup)
    }

    @Test
    fun aCompletedSubtaskStaysUnderItsParent() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)

        assertEquals(1, row(CHILD).indent)
        assertEquals(row(PARENT).primarySort, row(CHILD).primarySort)
        assertEquals(row(PARENT).sortGroup, row(CHILD).sortGroup)
    }

    @Test
    fun aCompletedSubtaskSortsWithItsParentInPlace() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)

        assertEquals(1, row(CHILD, atBottom = false).indent)
        assertEquals(
            row(PARENT, atBottom = false).primarySort,
            row(CHILD, atBottom = false).primarySort,
        )
    }

    @Test
    fun hidingACompletedSubtaskRemovesItRatherThanPromotingIt() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)

        assertEquals(listOf(PARENT), rows(showCompletedSubtasks = false).map { it.title })
    }

    @Test
    fun hidingACompletedSubtaskLeavesACompletedRootAlone() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)
        newTask(DONE_ROOT, completed = true, dueDate = CHILD_DUE)

        assertEquals(
            listOf(PARENT, DONE_ROOT),
            rows(showCompletedSubtasks = false).map { it.title },
        )
    }

    @Test
    fun aCompletedSubtaskWhoseParentIsNotInTheListStaysAsARoot() = runBlocking {
        val parent = newTask(PARENT)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)

        val rows = rows(showCompletedSubtasks = false, filter = dueFilter)
        assertEquals(listOf(CHILD), rows.map { it.title })
        assertEquals(0, rows.single().indent)
        assertTrue(rows.single().parentComplete)
    }

    @Test
    fun hidingACompletedSubtaskAlsoTakesAwayItsSubtreeCount() = runBlocking {
        val parent = newTask(PARENT, dueDate = PARENT_DUE)
        newTask(CHILD, parent = parent.id, completed = true, dueDate = CHILD_DUE)

        assertEquals(1, row(PARENT).children)
        assertEquals(
            0,
            rows(showCompletedSubtasks = false).first { it.title == PARENT }.children,
        )
    }

    companion object {
        private const val PARENT_DUE = 1_700_000_000_000L
        private const val CHILD_DUE = 1_900_000_000_000L
    }
}
