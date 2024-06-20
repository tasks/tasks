package org.tasks.tasklist

import android.util.SparseArray
import androidx.core.util.forEach
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.TaskContainer
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import org.tasks.ui.TaskListViewModel.UiItem

class SectionedDataSource(
    tasks: List<TaskContainer> = emptyList(),
    disableHeaders: Boolean = false,
    val groupMode: Int = SortHelper.GROUP_NONE,
    val subtaskMode: Int = SortHelper.SORT_MANUAL,
    private val collapsed: Set<Long> = emptySet(),
    private val completedAtBottom: Boolean = true,
): List<UiItem> {
    private val tasks = tasks.toMutableList()

    private val sections = if (disableHeaders || groupMode == SortHelper.GROUP_NONE) {
        SparseArray()
    } else {
        getSections()
    }

    fun getItem(position: Int): TaskContainer = tasks[sectionedPositionToPosition(position)]

    fun getHeaderValue(position: Int): Long = getSection(position).value

    fun isHeader(position: Int) = sections[position] != null

    private fun sectionedPositionToPosition(sectionedPosition: Int): Int {
        if (isHeader(sectionedPosition)) {
            return sections[sectionedPosition].firstPosition
        }

        var offset = 0
        for (i in 0 until sections.size()) {
            val section = sections.valueAt(i)
            if (section.sectionedPosition > sectionedPosition) {
                break
            }
            --offset
        }
        return sectionedPosition + offset
    }

    val taskCount: Int
        get() = tasks.size

    override val size: Int
        get() = tasks.size + sections.size()

    override fun get(index: Int) =
        sections[index]
            ?.let { UiItem.Header(it.value) }
            ?: UiItem.Task(getItem(index))

    override fun isEmpty() = size == 0

    override fun iterator(): Iterator<UiItem> {
        return object : Iterator<UiItem> {
            private var index = 0
            override fun hasNext() = index < size
            override fun next(): UiItem = get(index++)
        }
    }

    override fun listIterator(): ListIterator<UiItem> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<UiItem> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<UiItem> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: UiItem): Int {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: UiItem): Int {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<UiItem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: UiItem): Boolean {
        TODO("Not yet implemented")
    }

    fun getSection(position: Int): AdapterSection = sections[position]

    fun add(position: Int, task: TaskContainer) = tasks.add(sectionedPositionToPosition(position), task)

    fun removeAt(position: Int): TaskContainer = tasks.removeAt(sectionedPositionToPosition(position))

    private fun getSections(): SparseArray<AdapterSection> {
        val sections = ArrayList<AdapterSection>()
        val startOfToday = currentTimeMillis().startOfDay()
        for (i in tasks.indices) {
            val task = tasks[i]
            val sortGroup = task.sortGroup
            val header = if (completedAtBottom && task.parentComplete) {
                HEADER_COMPLETED
            } else if (sortGroup == null) {
                continue
            } else if (
                groupMode == SortHelper.SORT_LIST ||
                groupMode == SortHelper.SORT_IMPORTANCE ||
                sortGroup == 0L
            ) {
                sortGroup
            } else if (groupMode == SortHelper.SORT_DUE) {
                when {
                    sortGroup == 0L -> 0
                    sortGroup < startOfToday -> HEADER_OVERDUE
                    else -> sortGroup.startOfDay()
                }
            } else {
                sortGroup.startOfDay()
            }
            val isCollapsed = collapsed.contains(header)
            if (i == 0) {
                sections.add(AdapterSection(i, header, 0, isCollapsed))
            } else {
                val previousTask = tasks[i - 1]
                val previous = previousTask.sortGroup ?: 0L
                when {
                    completedAtBottom && task.parentComplete -> {
                        if (!previousTask.parentComplete) {
                            sections.add(AdapterSection(i, header, 0, isCollapsed))
                        }
                    }
                    groupMode == SortHelper.SORT_LIST ||
                    groupMode == SortHelper.SORT_IMPORTANCE ->
                        if (header != previous) {
                            sections.add(AdapterSection(i, header, 0, isCollapsed))
                        }
                    groupMode == SortHelper.SORT_DUE -> {
                        val previousOverdue = previous < startOfToday
                        val currentOverdue = header == HEADER_OVERDUE
                        if (previous > 0 &&
                            ((currentOverdue != previousOverdue) ||
                                    (!currentOverdue && header != previous.startOfDay()))
                        ) {
                            sections.add(AdapterSection(i, header, 0, isCollapsed))
                        }
                    }
                    else -> if (previous > 0 && header != previous.startOfDay()) {
                        sections.add(AdapterSection(i, header, 0, isCollapsed))
                    }
                }
            }
        }

        var adjustment = 0
        for (i in sections.indices) {
            val section = sections[i]
            section.firstPosition -= adjustment
            if (section.collapsed) {
                val next = sections.getOrNull(i + 1)?.firstPosition?.minus(adjustment) ?: tasks.size
                tasks.subList(section.firstPosition, next).clear()
                adjustment += next - section.firstPosition
            }
        }

        return setSections(sections)
    }

    private fun setSections(newSections: List<AdapterSection>): SparseArray<AdapterSection> {
        val sections = SparseArray<AdapterSection>()
        newSections.forEachIndexed { index, section ->
            section.sectionedPosition = section.firstPosition + index
            sections.append(section.sectionedPosition, section)
        }
        return sections
    }

    fun moveSection(toPosition: Int, offset: Int) {
        val old = sections[toPosition]
        sections.remove(toPosition)
        val newSectionedPosition = old.sectionedPosition + offset
        val previousSection = if (isHeader(newSectionedPosition - 1)) sections[newSectionedPosition - 1] else null
        val newFirstPosition = previousSection?.firstPosition ?: (old.firstPosition + offset)
        val new = AdapterSection(newFirstPosition, old.value, newSectionedPosition, old.collapsed)
        sections.append(new.sectionedPosition, new)
    }

    tailrec fun getNearestHeader(sectionedPosition: Int): Long =
        if (sectionedPosition < 0) {
            -1
        } else if (isHeader(sectionedPosition)) {
            getHeaderValue(sectionedPosition)
        } else {
            getNearestHeader(sectionedPosition - 1)
        }

    fun getSectionValues(): List<Long> {
        val values = ArrayList<Long>()
        sections.forEach { _, header -> values.add(header.value) }
        return values
    }

    companion object {
        const val HEADER_OVERDUE = -1L
        const val HEADER_COMPLETED = -2L
    }
}