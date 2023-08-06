package org.tasks.tasklist

import android.util.SparseArray
import androidx.core.util.forEach
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.TaskContainer
import org.tasks.time.DateTimeUtils.startOfDay

class SectionedDataSource(
    tasks: List<TaskContainer>,
    disableHeaders: Boolean,
    val groupMode: Int,
    val subtaskMode: Int,
    private val collapsed: Set<Long>,
    private val completedAtBottom: Boolean,
) {
    private val tasks = tasks.toMutableList()

    private val sections = if (disableHeaders || groupMode == SortHelper.GROUP_NONE) {
        SparseArray()
    } else {
        getSections()
    }

    fun getItem(position: Int): TaskContainer? = tasks.getOrNull(sectionedPositionToPosition(position))

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

    val size: Int
        get() = tasks.size + sections.size()

    fun getSection(position: Int): AdapterSection = sections[position]

    fun add(position: Int, task: TaskContainer) = tasks.add(sectionedPositionToPosition(position), task)

    fun removeAt(position: Int): TaskContainer = tasks.removeAt(sectionedPositionToPosition(position))

    private fun getSections(): SparseArray<AdapterSection> {
        val sections = ArrayList<AdapterSection>()
        val startOfToday = now().startOfDay()
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