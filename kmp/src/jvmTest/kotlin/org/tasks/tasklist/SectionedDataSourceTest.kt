package org.tasks.tasklist

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

class SectionedDataSourceTest {
    private val today = currentTimeMillis().startOfDay()
    private val tomorrow = today + 86_400_000L
    private val dayAfter = today + 2 * 86_400_000L

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

    @Test
    fun sectionValuesStayInPositionOrderAfterMovingASection() {
        val source = dataSource(
            task(ACTIVE, group = today),
            task(DONE, group = tomorrow),
            task(DONE_SUB, group = dayAfter),
        )
        assertEquals(listOf(today, tomorrow, dayAfter), source.getSectionValues())

        source.moveSection(2, -1)

        assertEquals(listOf(today, tomorrow, dayAfter), source.getSectionValues())
    }

    @Test
    fun rowsStayAddressableAfterMovingASection() {
        val source = dataSource(
            task(ACTIVE, group = today),
            task(DONE, group = tomorrow),
            task(DONE_SUB, group = dayAfter),
        )

        source.moveSection(2, -1)

        assertEquals(
            listOf(ACTIVE, DONE, DONE_SUB),
            (0 until source.size).filterNot { source.isHeader(it) }.map { source.getItem(it).title },
        )
    }

    @Test
    fun findsHeadersAndTasksByValue() {
        val active = task(ACTIVE)
        val done = task(DONE, completed = true)
        val source = dataSource(active, done)
        val completedHeader = UiItem.Header(HEADER_COMPLETED, false)

        assertEquals(1, source.indexOf(UiItem.Task(active)))
        assertEquals(2, source.indexOf(completedHeader))
        assertEquals(-1, source.indexOf(UiItem.Task(task(DONE_SUB))))
        assertTrue(source.contains(UiItem.Task(done)))
        assertFalse(source.contains(UiItem.Header(HEADER_COMPLETED, true)))
        assertTrue(source.containsAll(listOf(completedHeader, UiItem.Task(active))))
        assertFalse(source.containsAll(listOf(completedHeader, UiItem.Task(task(DONE_SUB)))))
    }

    @Test
    fun lastIndexOfFindsTheLastDuplicate() {
        val active = task(ACTIVE)
        val source = dataSource(active, task(DONE), active)

        assertEquals(1, source.indexOf(UiItem.Task(active)))
        assertEquals(3, source.lastIndexOf(UiItem.Task(active)))
        assertEquals(-1, source.lastIndexOf(UiItem.Task(task(DONE_SUB))))
    }

    @Test
    fun listIteratorWalksBothWays() {
        val source = dataSource(task(ACTIVE), task(DONE, completed = true))
        val items = (0 until source.size).map { source[it] }

        assertEquals(items, source.listIterator().asSequence().toList())

        val iterator = source.listIterator(2)
        assertEquals(2, iterator.nextIndex())
        assertEquals(items[2], iterator.next())
        assertEquals(items[2], iterator.previous())
        assertEquals(items[1], iterator.previous())
        assertEquals(0, iterator.previousIndex())
    }

    companion object {
        private const val ACTIVE = "active"
        private const val DONE = "done"
        private const val DONE_SUB = "done-sub"
    }
}
