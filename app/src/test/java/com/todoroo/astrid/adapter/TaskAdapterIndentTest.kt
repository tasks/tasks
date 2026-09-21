package com.todoroo.astrid.adapter

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.data.TaskContainer
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.Task

class TaskAdapterIndentTest {
    private lateinit var rows: List<TaskContainer>

    private val adapter = TaskAdapter(
        newTasksOnTop = false,
        googleTaskDao = mock(),
        caldavDao = mock(),
        taskDao = mock(),
        taskSaver = mock(),
        dirtyDao = mock(),
        refreshBroadcaster = mock(),
        taskMover = mock(),
    ).apply {
        setDataSource(object : TaskAdapterDataSource {
            override fun getItem(position: Int) = rows[position]

            override fun getTaskCount() = rows.size
        })
    }

    private fun row(
        id: Long,
        indent: Int = 0,
        parent: Long = 0,
        completed: Boolean = false,
        accountType: Int = CaldavAccount.TYPE_LOCAL,
    ) = TaskContainer(
        task = Task(
            id = id,
            parent = parent,
            completionDate = if (completed) COMPLETED_AT else 0,
        ),
        accountType = accountType,
        indent = indent,
    )

    private fun maxIndent(previous: TaskContainer, dragged: TaskContainer): Int {
        rows = listOf(previous, dragged)
        return adapter.maxIndent(0, dragged)
    }

    @Test
    fun anActiveRowCanTakeANewChild() {
        assertEquals(2, maxIndent(previous = row(id = 1, indent = 1), dragged = row(id = 2)))
    }

    @Test
    fun aCompletedRowIsNotOfferedAsAParentForAnActiveRow() {
        assertEquals(
            1,
            maxIndent(previous = row(id = 1, indent = 1, completed = true), dragged = row(id = 2)),
        )
    }

    @Test
    fun anActiveRowCannotBeDroppedAnywhereInsideACompletedSubtree() {
        val parent = row(id = 1)
        val dragged = row(id = 2, indent = 1, parent = parent.id)
        val done = row(id = 3, indent = 1, parent = parent.id, completed = true)
        rows = listOf(
            parent,
            dragged,
            done,
            row(id = 4, indent = 2, parent = done.id, completed = true),
        )

        assertEquals(1, adapter.maxIndent(3, dragged))
    }

    @Test
    fun aCompletedRowStillNestsUnderACompletedRow() {
        assertEquals(
            2,
            maxIndent(
                previous = row(id = 1, indent = 1, completed = true),
                dragged = row(id = 2, indent = 2, completed = true),
            ),
        )
    }

    @Test
    fun theIndentCeilingWinsWhenTheRowsBelowAskForACompletedParent() {
        val parent = row(id = 1)
        val done = row(id = 2, indent = 1, parent = parent.id, completed = true)
        val dragged = row(id = 4)
        rows = listOf(
            parent,
            done,
            row(id = 3, indent = 2, parent = done.id, completed = true),
            dragged,
        )
        val maxIndent = adapter.maxIndent(1, dragged)

        assertEquals(1, maxIndent)
        assertEquals(2, adapter.minIndent(2, dragged))
        assertEquals(1, adapter.minIndent(2, dragged, maxIndent))
    }

    @Test
    fun theIndentFloorSurvivesWhenItAlreadyFitsUnderTheCeiling() {
        val parent = row(id = 1)
        val dragged = row(id = 4)
        rows = listOf(
            parent,
            row(id = 2, indent = 1, parent = parent.id),
            row(id = 3),
            dragged,
        )
        val maxIndent = adapter.maxIndent(1, dragged)

        assertEquals(2, maxIndent)
        assertEquals(0, adapter.minIndent(2, dragged, maxIndent))
    }

    @Test
    fun aCompletedSingleLevelRowIsNotOfferedAsAParentForAnActiveRow() {
        assertEquals(
            0,
            maxIndent(
                previous = row(id = 1, completed = true, accountType = TYPE_GOOGLE_TASKS),
                dragged = row(id = 2, accountType = TYPE_GOOGLE_TASKS),
            ),
        )
    }

    @Test
    fun anActiveSingleLevelRowStillTakesANewChild() {
        assertEquals(
            1,
            maxIndent(
                previous = row(id = 1, accountType = TYPE_GOOGLE_TASKS),
                dragged = row(id = 2, accountType = TYPE_GOOGLE_TASKS),
            ),
        )
    }

    @Test
    fun aCompletedSingleLevelRowStillNestsUnderACompletedRow() {
        assertEquals(
            1,
            maxIndent(
                previous = row(id = 1, completed = true, accountType = TYPE_GOOGLE_TASKS),
                dragged = row(id = 2, completed = true, accountType = TYPE_GOOGLE_TASKS),
            ),
        )
    }

    companion object {
        private const val COMPLETED_AT = 1_700_000_000_000L
    }
}
