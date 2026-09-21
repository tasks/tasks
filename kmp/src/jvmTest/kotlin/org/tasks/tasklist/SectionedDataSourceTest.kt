package org.tasks.tasklist

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

class SectionedDataSourceTest {
    private val today = currentTimeMillis().startOfDay()
    private val tomorrow = today + 86_400_000L

    private fun task(title: String, completed: Boolean = false, group: Long = today) =
        TaskContainer(
            task = Task(id = title.hashCode().toLong(), title = title),
            parentComplete = completed,
            sortGroup = group,
        )

    private fun dataSource(
        vararg tasks: TaskContainer,
        collapsed: Set<Long> = emptySet(),
    ) = SectionedDataSource(
        tasks = tasks.toList(),
        disableHeaders = false,
        groupMode = SortHelper.SORT_DUE,
        collapsed = collapsed,
        completedAtBottom = true,
    )

    private fun SectionedDataSource.render(): List<String> = (0 until size).map {
        if (isHeader(it)) {
            if (getHeaderValue(it) == HEADER_COMPLETED) "completed" else "group"
        } else {
            getItem(it).title!!
        }
    }

    @Test
    fun collapsingCompletedTakesAwayTheWholeBlock() {
        val source = dataSource(
            task(ACTIVE),
            task(DONE, completed = true),
            task(DONE_SUB, completed = true, group = tomorrow),
            collapsed = setOf(HEADER_COMPLETED),
        )

        assertEquals(
            listOf("group", ACTIVE, "completed"),
            source.render(),
        )
    }

    @Test
    fun theCompletedSectionCoversItsHeaderAndItsRows() {
        val source = dataSource(task(ACTIVE), task(DONE, completed = true))

        assertEquals(listOf("group", ACTIVE, "completed", DONE), source.render())
    }

    @Test
    fun thereIsNoCompletedSectionWhenCompletedSortInPlace() {
        val source = SectionedDataSource(
            tasks = listOf(task(ACTIVE), task(DONE, completed = true)),
            disableHeaders = false,
            groupMode = SortHelper.SORT_DUE,
            completedAtBottom = false,
        )

        assertEquals(listOf("group", ACTIVE, DONE), source.render())
    }

    companion object {
        private const val ACTIVE = "active"
        private const val DONE = "done"
        private const val DONE_SUB = "done-sub"
    }
}
