package org.tasks.tasklist

import com.todoroo.astrid.adapter.TaskAdapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task

class DiffCallbackTest {
    private inline fun <reified T> stub(): T = Mockito.mock(T::class.java)

    private val adapter = TaskAdapter(
        newTasksOnTop = false,
        googleTaskDao = stub(),
        caldavDao = stub(),
        taskDao = stub(),
        taskSaver = stub(),
        dirtyDao = stub(),
        refreshBroadcaster = stub(),
        taskMover = stub(),
    )

    private val task = Task(id = 1, title = "child")

    /** A query result: a new container every time, over equal copies of the same row. */
    private fun rows(task: Task = this.task, indent: Int = 1) = SectionedDataSource(
        tasks = listOf(TaskContainer(task = task.copy(), indent = indent))
    )

    /** The indent writes [TaskListRecyclerAdapter.onBindViewHolder] makes on every bind. */
    private fun SectionedDataSource.bound() = also {
        for (position in 0 until size) {
            val row = getItem(position)
            row.indent = adapter.getIndent(row)
            row.targetIndent = row.indent
        }
    }

    private fun unchanged(old: SectionedDataSource, new: SectionedDataSource) =
        DiffCallback(old, new, adapter).areContentsTheSame(0, 0)

    @Test
    fun requeryingLeavesABoundSubtaskAlone() {
        assertTrue(unchanged(old = rows().bound(), new = rows()))
    }

    @Test
    fun requeryingLeavesABoundTopLevelTaskAlone() {
        assertTrue(unchanged(old = rows(indent = 0).bound(), new = rows(indent = 0)))
    }

    @Test
    fun anEditedTitleIsStillReportedAsAChange() {
        assertFalse(unchanged(old = rows().bound(), new = rows(task = task.copy(title = "new"))))
    }
}
