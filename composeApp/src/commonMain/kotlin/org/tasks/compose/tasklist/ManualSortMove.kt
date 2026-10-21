package org.tasks.compose.tasklist

import org.tasks.data.TaskContainer
import org.tasks.data.dao.CaldavDao
import org.tasks.tasklist.SectionedDataSource

/**
 * A planned vertical manual-sort move of [task] within its current parent.
 *
 * [newPosition] is the effective sort order the task will take, computed to match
 * `CaldavDao.move`'s dense-order semantics:
 * - moving up takes the neighbour's slot (the neighbour shifts down one via
 *   `shiftDown(cal, parent, neighbourOrder, taskOrder)`), so newPosition == neighbour order;
 * - moving down lands just below the neighbour, so newPosition == neighbour order + 1.
 * The parent does not change.
 */
internal data class ManualSortMove(
    val task: TaskContainer,
    val newPosition: Long,
)

/**
 * Plans a vertical manual-sort move of the task with [taskId] within the same parent.
 *
 * Rules (phase 1: no reparenting, no cross-list):
 * - Moving up: the nearest *visible* row above must be a task at the same indent under
 *   the same parent. The new order value is that neighbour's effective order (the task
 *   takes its slot and the neighbour shifts down).
 * - Moving down: rows deeper than [taskId]'s indent (its own subtree in the pre-order
 *   display) are skipped; the first visible row at the same or shallower indent must be
 *   a task at the same indent under the same parent. The new order value is that
 *   neighbour's effective order plus one.
 * - A group header above or below, or any indent/parent mismatch, means the task cannot
 *   legally move in that direction and null is returned.
 *
 * [deletions] is `SubtaskTreeRegistry.deletions`; rows in [RowState.Hidden] are not
 * rendered and must not count as neighbours.
 *
 * The caller performs the move with [CaldavDao.move], which renumbers the affected
 * sibling range (`shiftDown`), writes the order and marks the tasks dirty for CalDAV
 * sync - the exact primitive the native Android drag path commits through.
 */
internal fun SectionedDataSource.planManualSortMove(
    deletions: Map<String, Boolean>,
    taskId: Long,
    up: Boolean,
): ManualSortMove? {
    val index = indexOf(taskId) ?: return null
    val task = getItem(index)
    val neighbour = if (up) {
        previousVisibleTask(index, deletions)
    } else {
        nextVisibleTask(index, deletions)
    }
    return neighbour
        ?.takeIf { it.indent == task.indent && it.parent == task.parent }
        ?.let {
            ManualSortMove(
                task,
                if (up) it.caldavSortOrder else it.caldavSortOrder + 1,
            )
        }
}

private fun SectionedDataSource.indexOf(taskId: Long): Int? {
    for (i in 0 until size) {
        if (!isHeader(i) && getItem(i).id == taskId) return i
    }
    return null
}

/** The nearest rendered task row above [index], or null at the top / past a header. */
private fun SectionedDataSource.previousVisibleTask(
    index: Int,
    deletions: Map<String, Boolean>,
): TaskContainer? {
    for (i in index - 1 downTo 0) {
        if (isHeader(i)) return null
        val task = getItem(i)
        if (rowState(deletions, task) == RowState.Hidden) continue
        return task
    }
    return null
}

/**
 * The nearest rendered task row below [index] that is not inside the task's own
 * subtree, or null at the bottom. In the pre-order display every row deeper than the
 * task until the first shallower-or-equal row is a descendant of the task, so skipping
 * deeper rows lands exactly on the row the task's subtree will move past.
 */
private fun SectionedDataSource.nextVisibleTask(
    index: Int,
    deletions: Map<String, Boolean>,
): TaskContainer? {
    val task = getItem(index)
    for (i in index + 1 until size) {
        if (isHeader(i)) return null
        val row = getItem(i)
        if (rowState(deletions, row) == RowState.Hidden) continue
        if (row.indent <= task.indent) return row
    }
    return null
}

/**
 * Executes a planned manual-sort move through the shared [CaldavDao].
 *
 * `caldavDao.move` is a `@Transaction` doing the dense renumbering (`shiftDown`), the
 * `tasks.order` write and `dirtyDao.setDirty` for the X-APPLE-SORT-ORDER sync - the same
 * path the native app's drag-and-drop commits through, so desktop reorders round-trip
 * identically.
 *
 * Returns true when a move was performed, false when the plan rejected it (the caller
 * should reflect that in button/pointer state, not as an error).
 */
internal class CaldavTaskReorderer(
    private val caldavDao: CaldavDao,
) {
    suspend fun move(
        dataSource: SectionedDataSource,
        deletions: Map<String, Boolean>,
        taskId: Long,
        up: Boolean,
    ): Boolean {
        val plan = dataSource.planManualSortMove(deletions, taskId, up) ?: return false
        caldavDao.move(
            task = plan.task,
            previousParent = plan.task.parent,
            newParent = plan.task.parent,
            newPosition = plan.newPosition,
        )
        return true
    }
}
