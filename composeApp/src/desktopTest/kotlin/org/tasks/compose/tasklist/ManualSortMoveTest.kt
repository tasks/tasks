package org.tasks.compose.tasklist

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.tasklist.SectionedDataSource

/**
 * Pure-logic tests for the vertical manual-sort move planner. No database, no Compose:
 * the planner only sees the visible pre-order rows (a `SectionedDataSource` with headers
 * disabled, exactly as the list is built when "My order" is active) and must pick the
 * same effective order value the native app's `moveCaldavTask` same-indent branch would.
 *
 * TooManyFunctions is suppressed: the class holds one cohesive @Test per planner
 * behaviour, and splitting those across classes would not make them clearer.
 */
@Suppress("TooManyFunctions")
class ManualSortMoveTest {
    private fun uuid(id: Long) = "uuid-$id"

    private fun t(
        id: Long,
        title: String,
        parent: Long = 0,
        indent: Int = 0,
        order: Long = 0,
    ) = TaskContainer(
        task = Task(id = id, title = title, remoteId = uuid(id), parent = parent),
        primarySort = if (indent == 0) order else 0,
        secondarySort = if (indent > 0) order else 0,
        indent = indent,
    )

    private fun list(vararg tasks: TaskContainer) =
        SectionedDataSource(tasks = tasks.toList(), disableHeaders = true)

    private fun groupedList(vararg tasks: TaskContainer) =
        SectionedDataSource(
            tasks = tasks.toList(),
            disableHeaders = false,
            groupMode = SortHelper.SORT_LIST,
        )

    // ---- Top-level, dense orders 1, 2, 3 ----

    @Test
    fun movingDownTakesOnePastTheNeighbourBelow() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        val move = list(a, b, c).planManualSortMove(emptyMap(), taskId = 2, up = false)

        assertNotNull(move)
        assertEquals(2, move!!.task.id)
        assertEquals(4, move.newPosition) // C.order + 1
    }

    @Test
    fun movingUpTakesTheSlotAboveTheNeighbour() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        val move = list(a, b, c).planManualSortMove(emptyMap(), taskId = 3, up = true)

        assertNotNull(move)
        assertEquals(3, move!!.task.id)
        assertEquals(2, move.newPosition) // B.order: C takes B's slot, B shifts down
    }

    @Test
    fun movingUpToTheVeryTopTakesTheFirstSlot() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val move = list(a, b).planManualSortMove(emptyMap(), taskId = 2, up = true)

        assertNotNull(move)
        assertEquals(1, move!!.newPosition) // A's slot; shiftDown pushes A down to 2
    }

    @Test
    fun theTopTaskCannotMoveUp() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        assertNull(list(a, b).planManualSortMove(emptyMap(), taskId = 1, up = true))
    }

    @Test
    fun theBottomTaskCannotMoveDown() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        assertNull(list(a, b).planManualSortMove(emptyMap(), taskId = 2, up = false))
    }

    @Test
    fun anUnknownTaskPlansNothing() {
        val a = t(1, "A", order = 1)
        assertNull(list(a).planManualSortMove(emptyMap(), taskId = 99, up = true))
        assertNull(list(a).planManualSortMove(emptyMap(), taskId = 99, up = false))
    }

    // ---- Subtrees: moving down must skip the task's own children ----

    @Test
    fun movingDownSkipsTheTasksOwnSubtree() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val b1 = t(3, "B1", parent = 2, indent = 1, order = 1)
        val c = t(4, "C", order = 3)
        val move = list(a, b, b1, c).planManualSortMove(emptyMap(), taskId = 2, up = false)

        // B1 is B's own child (deeper indent) and stays with B; C is the real neighbour.
        assertNotNull(move)
        assertEquals(4, move!!.newPosition) // C.order + 1
    }

    @Test
    fun aTaskWithSubtasksStillMovesUpAcrossTheRowAboveIt() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val b1 = t(3, "B1", parent = 2, indent = 1, order = 1)
        val move = list(a, b, b1).planManualSortMove(emptyMap(), taskId = 2, up = true)

        assertNotNull(move)
        assertEquals(1, move!!.newPosition) // A's slot; A shifts down
    }

    // ---- Indent boundaries: no reparenting in phase 1 ----

    @Test
    fun aSubtaskCannotBeDraggedUpToTheTopLevel() {
        val p = t(1, "P", order = 1)
        val p1 = t(2, "P1", parent = 1, indent = 1, order = 1)
        assertNull(list(p, p1).planManualSortMove(emptyMap(), taskId = 2, up = true))
    }

    @Test
    fun movingDownPastASiblingLandsBelowItsSubtree() {
        val a = t(1, "A", order = 1)
        val p = t(2, "P", order = 2)
        val p1 = t(3, "P1", parent = 2, indent = 1, order = 1)
        val move = list(a, p, p1).planManualSortMove(emptyMap(), taskId = 1, up = false)

        // The row below A is P (a real top-level neighbour); P1 is P's child and moves
        // with it, so A lands past both: P.order + 1.
        assertNotNull(move)
        assertEquals(3, move!!.newPosition)
    }

    @Test
    fun siblingsUnderDifferentParentsAreNotNeighbours() {
        val p1 = t(1, "P1", order = 1)
        val p1a = t(2, "P1a", parent = 1, indent = 1, order = 1)
        val p2 = t(3, "P2", order = 2)
        val p2a = t(4, "P2a", parent = 3, indent = 1, order = 1)
        // P2a's visible row above is P2 (its own parent), a different indent: rejected.
        assertNull(list(p1, p1a, p2, p2a).planManualSortMove(emptyMap(), taskId = 4, up = true))
        // P1a's visible row below is P2 (its sibling's parent): a different parent at a
        // shallower indent - not a same-parent neighbour either way.
        assertNull(list(p1, p1a, p2, p2a).planManualSortMove(emptyMap(), taskId = 2, up = false))
    }

    @Test
    fun subtasksUnderTheSameParentCanReorder() {
        val p = t(1, "P", order = 1)
        val p1 = t(2, "P1", parent = 1, indent = 1, order = 1)
        val p2 = t(3, "P2", parent = 1, indent = 1, order = 2)
        val move = list(p, p1, p2).planManualSortMove(emptyMap(), taskId = 3, up = true)

        assertNotNull(move)
        assertEquals(1, move!!.newPosition) // P1's slot; P1 shifts down
    }

    // ---- Deletion overlays ----

    @Test
    fun hiddenRowsAreNotNeighbours() {
        val a = t(1, "A", order = 1)
        val h = t(2, "H", order = 2)
        val b = t(3, "B", order = 3)
        val deletions = mapOf(uuid(2) to false) // H is going away (hidden)
        val move = list(a, h, b).planManualSortMove(deletions, taskId = 3, up = true)

        assertNotNull(move)
        assertEquals(1, move!!.newPosition) // A's slot, skipping the hidden H
    }

    @Test
    fun aDoomedRowStillCountsAsANeighbour() {
        // A doomed (marked, still drawn) row occupies its position until the list reloads.
        val a = t(1, "A", order = 1)
        val d = t(2, "D", order = 2)
        val b = t(3, "B", order = 3)
        val deletions = mapOf(uuid(2) to true)
        val move = list(a, d, b).planManualSortMove(deletions, taskId = 3, up = true)

        assertNotNull(move)
        assertEquals(2, move!!.newPosition) // D's slot: D is still visible
    }

    // ---- Drop planner (drag-and-drop) ----

    @Test
    fun aOneStepDropMatchesTheButtonPlanner() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        val ds = list(a, b, c)
        assertEquals(
            ds.planManualSortMove(emptyMap(), taskId = 2, up = false)!!.newPosition,
            ds.planManualSortDrop(emptyMap(), taskId = 2, dropIndex = 2)!!.newPosition,
        )
        assertEquals(
            ds.planManualSortMove(emptyMap(), taskId = 3, up = true)!!.newPosition,
            ds.planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 1)!!.newPosition,
        )
    }

    @Test
    fun dropDownLandsOnTheDroppedRow() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        val d = t(4, "D", order = 4)
        // A drops onto D's row: final display B, C, D, A -> ordinal 3 -> D.order + 1.
        val move = list(a, b, c, d).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 3)
        assertNotNull(move)
        assertEquals(5, move!!.newPosition)
    }

    @Test
    fun dropUpTwoRowsTakesTheTopSlot() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        val d = t(4, "D", order = 4)
        // D drops onto A's row: final display D, A, B, C -> ordinal 0 -> A's slot.
        val move = list(a, b, c, d).planManualSortDrop(emptyMap(), taskId = 4, dropIndex = 0)
        assertNotNull(move)
        assertEquals(1, move!!.newPosition)
    }

    @Test
    fun dropToTheBottomRowLandsBelowTheLastSibling() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        val c = t(3, "C", order = 3)
        // A drops onto C's row (the last row): ordinal 2 -> C.order + 1.
        val move = list(a, b, c).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 2)
        assertNotNull(move)
        assertEquals(4, move!!.newPosition)
    }

    @Test
    fun aDropOntoTheTasksOwnRowPlansNothing() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 2)
        assertNull(list(a, b).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 0))
    }

    @Test
    fun aDropIntoTheTasksOwnSubtreePlansNothing() {
        val a = t(1, "A", order = 1)
        val a1 = t(2, "A1", parent = 1, indent = 1, order = 1)
        val b = t(3, "B", order = 2)
        // A's own child row is inside A's subtree: the drop is a no-op.
        assertNull(list(a, a1, b).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 1))
    }

    @Test
    fun aDropIntoASiblingsSubtreeSnapsToBeforeThatSibling() {
        val a = t(1, "A", order = 1)
        val a1 = t(2, "A1", parent = 1, indent = 1, order = 1)
        val b = t(3, "B", order = 2)
        // B drops onto A1's row: a top-level task cannot land inside A's subtree, so it
        // snaps to the gap before A -> ordinal 0 -> A's slot.
        val move = list(a, a1, b).planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 1)
        assertNotNull(move)
        assertEquals(1, move!!.newPosition)
    }

    @Test
    fun aDropIntoASiblingsSubtreeSnapsPastIt() {
        val a = t(1, "A", order = 1)
        val p = t(2, "P", order = 2)
        val p1 = t(3, "P1", parent = 2, indent = 1, order = 1)
        val c = t(4, "C", order = 3)
        // A drops onto P1's row (inside P's subtree): a top-level task cannot land
        // there, so it snaps to the gap before P -> ordinal 1 -> P.order + 1,
        // i.e. A lands past P and its child.
        val move = list(a, p, p1, c).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 2)
        assertNotNull(move)
        assertEquals(3, move!!.newPosition)
        // A onto C's row instead lands after C: ordinal 2 -> C.order + 1.
        assertEquals(4, list(a, p, p1, c).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 3)!!.newPosition)
    }

    @Test
    fun dropsWorkWithGappedSortOrders() {
        val a = t(1, "A", order = 1)
        val b = t(2, "B", order = 101)
        val c = t(3, "C", order = 201)
        // Down onto B's row: ordinal 1 -> B.order + 1 (shiftDown breaks at the gap
        // between A's new order 102 and C at 201, leaving C untouched).
        assertEquals(102, list(a, b, c).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 1)!!.newPosition)
        // Down onto C's row: ordinal 2 -> C.order + 1.
        assertEquals(202, list(a, b, c).planManualSortDrop(emptyMap(), taskId = 1, dropIndex = 2)!!.newPosition)
        // Up: C onto A's row -> ordinal 0 -> A's slot (bounded shiftDown breaks at the
        // gap and leaves B untouched).
        assertEquals(1, list(a, b, c).planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 0)!!.newPosition)
    }

    @Test
    fun hiddenRowsAreNotDropTargetsAndNotSiblings() {
        val a = t(1, "A", order = 1)
        val h = t(2, "H", order = 2)
        val b = t(3, "B", order = 3)
        val deletions = mapOf(uuid(2) to false)
        // B drops onto A's row, skipping the hidden H: ordinal 0 -> A's slot.
        val move = list(a, h, b).planManualSortDrop(deletions, taskId = 3, dropIndex = 0)
        assertNotNull(move)
        assertEquals(1, move!!.newPosition)
        // B onto the hidden H's row is a no-op: H is not rendered, so B's slot is
        // unchanged either way.
        assertNull(list(a, h, b).planManualSortDrop(deletions, taskId = 3, dropIndex = 1))
    }

    @Test
    fun aDropOntoAHeaderRowPlansNothing() {
        val a = t(1, "A", order = 1).copy(sortGroup = 1000)
        val b = t(2, "B", order = 2).copy(sortGroup = 1000)
        val c = t(3, "C", order = 3).copy(sortGroup = 2000)
        // Rows: header(0), A(1), B(2), header(3), C(4).
        val ds = groupedList(a, b, c)
        assertTrue(ds.isHeader(0))
        assertTrue(ds.isHeader(3))
        assertNull(ds.planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 0))
        assertNull(ds.planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 3))
    }

    @Test
    fun aDropAcrossASectionHeaderPlansNothing() {
        val a = t(1, "A", order = 1).copy(sortGroup = 1000)
        val b = t(2, "B", order = 2).copy(sortGroup = 1000)
        val c = t(3, "C", order = 3).copy(sortGroup = 2000)
        // Rows: header(0), A(1), B(2), header(3), C(4). Phase 1 keeps moves inside the
        // task's own section: C onto A's or B's row crosses a header -> rejected.
        val ds = groupedList(a, b, c)
        assertNull(ds.planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 1))
        assertNull(ds.planManualSortDrop(emptyMap(), taskId = 3, dropIndex = 2))
        // ...and symmetrically B onto C's row.
        assertNull(ds.planManualSortDrop(emptyMap(), taskId = 2, dropIndex = 4))
    }
}
