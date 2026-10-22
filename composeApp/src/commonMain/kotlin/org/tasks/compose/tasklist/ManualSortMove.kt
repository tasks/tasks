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
 * Plans a one-step vertical manual-sort move of the task with [taskId] within the same
 * parent.
 *
 * Rules (phase 1: no reparenting, no cross-list):
 * - Moving up: the nearest *visible* row above must be a task at the same indent under
 *   the same parent. The new order value is that neighbour's effective order (the task
 *   takes its slot and the neighbour shifts down).
 * - Moving down: rows deeper than [taskId]'s indent (its own subtree in the pre-order
 *   display) are skipped; the first visible row at the same or shallower indent must be
 *   a task at the same indent under the same parent. The new order value is that
 *   neighbour's effective order plus one. In the pre-order display every row deeper
 *   than a task until the first shallower-or-equal row is that task's own descendant,
 *   so the down-walk skips exactly the rows that move with the task.
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
        previousRenderedTask(index, deletions)
    } else {
        nextRenderedTaskOutsideOwnSubtree(index, deletions)
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

/**
 * Plans a manual-sort drop of the task with [taskId] at visible row [dropIndex] - the
 * row index the dragged task will occupy in the display after the drop (equivalently,
 * the insertion index into the list with the task removed). [dropIndex] must be a
 * rendered task row: header and hidden rows are rejected. This is the same convention
 * the native Android drag commit uses (`TaskAdapter.moved(from, to)` with `to` = final
 * position after the `to++` adjustment for downward moves - exactly this reading, since
 * the rows between `from` and `to` shift one).
 *
 * Semantics, matching the native `moveCaldavTask` neighbour rules:
 * - Moving up to [dropIndex]: the task takes the slot of the k-th visible same-parent
 *   sibling, where k is the number of such siblings displayed above [dropIndex]
 *   (newPosition == that sibling's order; `CaldavDao.move` shifts the intervening
 *   siblings down via bounded `shiftDown`).
 * - Moving down to [dropIndex]: the task lands just below the (k+1)-th sibling, where
 *   k is the number of such siblings displayed at or above [dropIndex], excluding the
 *   task itself (newPosition == that sibling's order + 1, and the unbounded `shiftDown`
 *   closes the gap behind the task).
 * - A drop point on a subtree row snaps to the gap before that subtree's owner - the
 *   nearest visible row at or above the drop point with an indent at most the task's.
 *   A drop into the task's own subtree, past a section header, onto a header or hidden
 *   row, or at the task's own row is rejected with null (the drop is a no-op).
 * - Both formulas hold for arbitrary drop distances and for gapped (non-dense) CalDAV
 *   sort orders, because `shiftDown` tolerates gaps by breaking at the first
 *   non-dense row.
 *
 * [deletions] is `SubtaskTreeRegistry.deletions`; rows in [RowState.Hidden] are not
 * rendered and must not count as siblings or drop points.
 *
 * The one-step [planManualSortMove] is a special case of this planner (drop at the
 * adjacent row), but is kept as its own implementation so the button path retains its
 * tested neighbour-walk behaviour.
 */
internal fun SectionedDataSource.planManualSortDrop(
    deletions: Map<String, Boolean>,
    taskId: Long,
    dropIndex: Int,
): ManualSortMove? {
    val index = indexOf(taskId) ?: return null
    if (dropIndex < 0 || dropIndex >= size || dropIndex == index) return null
    if (isHeader(dropIndex)) return null
    if (rowState(deletions, getItem(dropIndex)) == RowState.Hidden) return null
    val task = getItem(index)

    // A drop point on a subtree row snaps to the gap before the subtree's owner: the
    // nearest visible row at or above the drop point with indent at most the task's.
    var gap = dropIndex
    while (getItem(gap).indent > task.indent) {
        var j = gap
        while (j >= 0 && !isHeader(j) && getItem(j).indent > task.indent) j--
        if (j < 0 || isHeader(j)) return null // crossed a section header
        if (getItem(j).id == task.id) return null // inside the task's own subtree
        gap = j
    }

    // The drop must stay inside the task's own section.
    val (lo, hi) = if (index < gap) index to gap else gap to index
    for (i in lo..hi) {
        if (i != index && i != gap && isHeader(i)) return null
    }

    // Visible same-parent siblings with their row indices, in display order.
    val siblings = ArrayList<Pair<Int, TaskContainer>>()
    for (i in 0 until size) {
        if (isHeader(i)) continue
        val row = getItem(i)
        if (rowState(deletions, row) == RowState.Hidden) continue
        if (row.parent == task.parent) siblings.add(i to row)
    }
    val taskOrdinal = siblings.indexOfFirst { it.second.id == task.id }
    // The task's final ordinal among its siblings, read off the drop row: the
    // siblings above the drop row when moving up, or at-or-above it (the task itself
    // excluded) when moving down.
    val finalOrdinal = if (gap < index) {
        siblings.count { it.first < gap }
    } else {
        siblings.count { it.first <= gap } - 1
    }
    if (finalOrdinal !in 0 until siblings.size) return null
    if (finalOrdinal == taskOrdinal) return null // no-op (same sibling slot)

    val anchor = siblings[finalOrdinal].second
    return ManualSortMove(
        task,
        if (finalOrdinal < taskOrdinal) {
            anchor.caldavSortOrder // take the slot; anchor shifts down
        } else {
            anchor.caldavSortOrder + 1 // land just below the anchor
        },
    )
}

private fun SectionedDataSource.indexOf(taskId: Long): Int? {
    for (i in 0 until size) {
        if (!isHeader(i) && getItem(i).id == taskId) return i
    }
    return null
}

private fun SectionedDataSource.previousRenderedTask(
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

private fun SectionedDataSource.nextRenderedTaskOutsideOwnSubtree(
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

    /**
     * Commits a drop of [taskId] at visible row [dropIndex] through the shared
     * [CaldavDao]. Returns true when a move was performed, false when the plan rejected
     * the drop (no-op, own subtree, or cross-section).
     */
    suspend fun dropMove(
        dataSource: SectionedDataSource,
        deletions: Map<String, Boolean>,
        taskId: Long,
        dropIndex: Int,
    ): Boolean {
        val plan = dataSource.planManualSortDrop(deletions, taskId, dropIndex) ?: return false
        caldavDao.move(
            task = plan.task,
            previousParent = plan.task.parent,
            newParent = plan.task.parent,
            newPosition = plan.newPosition,
        )
        return true
    }
}
