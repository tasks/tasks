package org.tasks.compose.tasklist

import org.tasks.data.TaskContainer
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
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

/**
 * A planned reparent: [task] becomes a direct child of [newParent], appended to the
 * end of its children.
 */
internal data class ManualSortReparent(
    val task: TaskContainer,
    val newParent: TaskContainer,
)

/**
 * A planned outdent: [task] moves one indent level shallower, under [newParent]
 * (0 = top level), at [newPosition] among its new parent's children.
 */
internal data class ManualSortOutdent(
    val task: TaskContainer,
    val newParent: Long,
    val newPosition: Long,
)

/**
 * Plans an outdent drop of the task with [taskId]: a drag released more than half an
 * indent step to the left of the grab point moves the task one indent level
 * shallower, at visible row [dropIndex] (the row the task settles on).
 *
 * The new parent is the nearest rendered row above the settled position whose indent
 * is strictly shallower than the task's new indent; none means top level (0). An
 * indent-1 task always outdents to the top level, matching the "drag back to a
 * normal task" affordance. The position uses the same dense-order convention as
 * [planManualSortDrop]: the task takes the slot of the (k+1)-th rendered new
 * sibling, k = the number of rendered new siblings above the settled position -
 * moving down lands just below that sibling, moving up or in place takes its
 * slot. An in-place outdent keeps the task in its own visual slot; a drop below
 * the task's own subtree is legal, since the subtree follows the task and the
 * settle lands at the task's own slot among the new siblings. Rejected with
 * null: a top-level task (nothing to outdent), a header or hidden drop point,
 * and a drop that crosses a section header.
 */
internal fun SectionedDataSource.planManualSortOutdent(
    deletions: Map<String, Boolean>,
    taskId: Long,
    dropIndex: Int,
): ManualSortOutdent? {
    val index = indexOf(taskId) ?: return null
    if (dropIndex < 0 || dropIndex >= size) return null
    if (isHeader(dropIndex)) return null
    val task = getItem(index)
    if (task.indent == 0) return null
    if (dropIndex != index && rowState(deletions, getItem(dropIndex)) == RowState.Hidden) return null
    // The drop must stay inside the task's own section.
    val (lo, hi) = if (index < dropIndex) index to dropIndex else dropIndex to index
    for (i in lo..hi) {
        if (i != index && i != dropIndex && isHeader(i)) return null
    }
    val newIndent = task.indent - 1

    // The new parent: nearest rendered row above the settled position strictly
    // shallower than the new indent; none means top level. Same start-row rule as
    // reparentTargetForDrop (downward settles count the row at dropIndex, upward and
    // in-place settle at dropIndex - 1).
    val start = if (dropIndex > index) dropIndex else dropIndex - 1
    var newParent = 0L
    for (i in start downTo 0) {
        if (i == index) continue
        if (isHeader(i)) break
        val row = getItem(i)
        if (rowState(deletions, row) == RowState.Hidden) continue
        if (row.indent < newIndent) {
            newParent = row.id
            break
        }
    }

    // Visible children of the new parent with their row indices, in display order.
    // The task is not among them (outdenting always changes the parent), so the count
    // needs no self-exclusion. Position rule: the task takes the slot of the (k+1)-th
    // rendered new sibling, where k is the number of rendered new siblings above the
    // settled position (rows 0..dropIndex-1 - the rows above the settle in the
    // post-move display, the task's own row vacated and its descendants still
    // following it). Moving down lands just below that sibling (its order + 1);
    // moving up or in place takes its slot (the sibling shifts down). k equal to the
    // sibling count appends past the last one - always legal, since the task is not
    // already a child of the new parent. A drop below the task's own subtree is not
    // rejected: the subtree follows the task, so the settle simply lands at the
    // task's own slot among the new siblings.
    val siblings = ArrayList<Pair<Int, TaskContainer>>()
    for (i in 0 until size) {
        if (isHeader(i)) continue
        val row = getItem(i)
        if (rowState(deletions, row) == RowState.Hidden) continue
        if (row.parent == newParent) siblings.add(i to row)
    }
    val finalOrdinal = siblings.count { it.first < dropIndex }
    val newPosition = if (finalOrdinal == siblings.size) {
        if (siblings.isEmpty()) 0L else siblings.last()!!.second.caldavSortOrder + 1
    } else {
        val anchor = siblings[finalOrdinal].second
        if (dropIndex > index) anchor.caldavSortOrder + 1 else anchor.caldavSortOrder
    }
    return ManualSortOutdent(task, newParent, newPosition)
}

/**
 * Plans a drop of the task with [taskId] onto the task row at [targetSectionIndex],
 * making the dropped task a subtask of the target.
 *
 * Rejected with null (the drop is a no-op):
 * - the target is a header or hidden row, or is the task itself;
 * - the target is the task's current parent (it is already a child; sibling
 *   placement belongs to the gap zones);
 * - the target is in a different CalDAV calendar (cross-list reparenting is a
 *   follow-up, mirroring the native app's separate `taskMover` path);
 * - a section header separates the task and the target;
 * - the target is inside the task's own subtree (would create a cycle).
 *
 * [deletions] is `SubtaskTreeRegistry.deletions`; hidden rows are not render
 * targets and are rejected.
 */
internal fun SectionedDataSource.planManualSortReparent(
    deletions: Map<String, Boolean>,
    taskId: Long,
    targetSectionIndex: Int,
): ManualSortReparent? {
    if (targetSectionIndex < 0 || targetSectionIndex >= size) return null
    if (isHeader(targetSectionIndex)) return null
    val target = getItem(targetSectionIndex)
    if (rowState(deletions, target) == RowState.Hidden) return null
    val index = indexOf(taskId) ?: return null
    val task = getItem(index)
    if (target.id == taskId) return null
    if (task.parent == target.id) return null // already a direct child of the target
    if (task.caldav != target.caldav) return null // v1: same calendar only
    // The drop must stay inside the task's own section.
    val (lo, hi) = if (index < targetSectionIndex) index to targetSectionIndex else targetSectionIndex to index
    for (i in lo..hi) {
        if (i != index && i != targetSectionIndex && isHeader(i)) return null
    }
    // In the pre-order display, every row after the task with a deeper indent than
    // the task's (until the first shallower-or-equal row) is its own subtree; the
    // target being one of them would make the task its own descendant.
    // In the pre-order display, the target is inside the task's own subtree exactly
    // when it is deeper than the task and no row between them carries an indent at or
    // below the task's (the boundary back out of the subtree) or a section header.
    // Making the task a child of one of its own descendants would cycle, so reject.
    if (target.indent > task.indent) {
        var insideOwnSubtree = true
        for (i in index + 1 until targetSectionIndex) {
            if (isHeader(i) || getItem(i).indent <= task.indent) {
                insideOwnSubtree = false
                break
            }
        }
        if (insideOwnSubtree) return null
    }
    return ManualSortReparent(task, target)
}

/**
 * The parent candidate for an indented drop: the nearest rendered row above the
 * settled position [dropIndex] whose indent is at most the dragged task's indent -
 * the port of the native app's `findParentIndex`.
 *
 * The native app decides between sibling reorder and subtask from the drag's
 * horizontal offset; the caller applies that threshold, and this function is the
 * vertical half of the decision. An indented drop places the task one indent level
 * deeper than its current indent, so the parent is the nearest row whose indent is
 * at most the task's current one - which includes rows at the same indent: dropping
 * a row indented below its sibling makes it that sibling's subtask (the native app's
 * two-item case).
 *
 * [dropIndex] is the task's settled row, i.e. the display with the task removed plus
 * the task inserted there. Walking the pre-settle data list, the rows above the
 * settled position are rows `0..dropIndex-1` for an upward drag and the shifted rows
 * `index+1..dropIndex` then `0..index-1` for a downward drag - so the walk starts at
 * the row now occupying the slot directly above the drop point (the current
 * `dropIndex` row when moving down) and skips the task's own vacated row.
 *
 * Returns null when no such row exists (for example a drop at the top of the list):
 * the caller then falls back to the plain sibling drop at the settled position.
 * A section header ends the walk; hidden (not rendered) rows are skipped.
 */
internal fun SectionedDataSource.reparentTargetForDrop(
    deletions: Map<String, Boolean>,
    taskId: Long,
    dropIndex: Int,
): Long? {
    val index = indexOf(taskId) ?: return null
    val task = getItem(index)
    // In place (dropIndex == index) the rows above are simply 0..index-1; the formula
    // below covers that. An in-place indented drag reparents the task in its own slot
    // (the native app's two-item case), so it is not rejected here.
    // An upward settle at row 0 starts at -1: an empty range, nothing above it.
    val start = if (dropIndex > index) dropIndex else dropIndex - 1
    for (i in start downTo 0) {
        if (i == index) continue // the task's own vacated row is not a parent
        if (isHeader(i)) return null
        val row = getItem(i)
        if (rowState(deletions, row) == RowState.Hidden) continue
        if (row.indent <= task.indent) return row.id
    }
    return null
}

internal fun SectionedDataSource.indexOf(taskId: Long): Int? {
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
    private val taskDao: TaskDao,
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

    /**
     * Commits a drop of [taskId] onto the row at [targetSectionIndex]: the task
     * becomes a direct subtask of that row's task, appended to the end of its
     * children (the native app's `findLastTask(...)?.plus(1)` position convention,
     * since a null order would sort by creation time rather than last).
     *
     * Mirrors the native `changeCaldavParent` commit: the order goes through the
     * shared `CaldavDao.move` (dense renumbering + dirty marking for CalDAV sync),
     * and the local `tasks.parent` column through `TaskDao.setParent`. Returns true
     * when the reparent was performed, false when the plan rejected the drop.
     */
    suspend fun reparentMove(
        dataSource: SectionedDataSource,
        deletions: Map<String, Boolean>,
        taskId: Long,
        targetSectionIndex: Int,
    ): Boolean {
        val plan = dataSource.planManualSortReparent(deletions, taskId, targetSectionIndex)
            ?: return false
        val newPosition = caldavDao.findLastTask(plan.task.caldav!!, plan.newParent.id)?.plus(1)
            ?: 0L
        return commitParentChange(plan.task, plan.newParent.id, newPosition)
    }

    /**
     * Commits an outdent of [taskId] to visible row [dropIndex]: one indent level
     * shallower, at the planned position among the new parent's children. Same
     * commit recipe as [reparentMove]. Returns true when the outdent was performed,
     * false when the plan rejected it.
     */
    suspend fun outdentMove(
        dataSource: SectionedDataSource,
        deletions: Map<String, Boolean>,
        taskId: Long,
        dropIndex: Int,
    ): Boolean {
        val plan = dataSource.planManualSortOutdent(deletions, taskId, dropIndex) ?: return false
        return commitParentChange(plan.task, plan.newParent, plan.newPosition)
    }

    /**
     * The shared reparent/outdent commit: the order goes through the shared
     * `CaldavDao.move` (dense renumbering + dirty marking for CalDAV sync) and the
     * local `tasks.parent` column through `TaskDao.setParent`. The remote parent
     * must also be written (`caldav_tasks.cd_remote_parent`, mirroring the native
     * `TaskMover.nestUnderParent`): the CalDAV upload builds the VTODO's RELATED-TO
     * parent from that column, so without it the server keeps the old parent and the
     * next sync's `updateParents` re-derivation reverts the change locally. A
     * top-level move writes null, and the force `updateParents` pass keeps the
     * calendar's derived parents consistent immediately.
     */
    private suspend fun commitParentChange(
        task: TaskContainer,
        newParent: Long,
        newPosition: Long,
    ): Boolean {
        caldavDao.move(
            task = task,
            previousParent = task.parent,
            newParent = newParent,
            newPosition = newPosition,
        )
        taskDao.setParent(newParent, listOf(task.id))
        val caldavTask = caldavDao.getTask(task.id) ?: return false
        val remoteParent = if (newParent == 0L) null else caldavDao.getTask(newParent)?.remoteId
        caldavDao.update(caldavTask.id, remoteParent)
        caldavDao.updateParents(task.caldav, force = true)
        return true
    }
}
