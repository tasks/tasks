package org.tasks.compose.tasklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.subtaskKey
import org.tasks.tasklist.SectionedDataSource

/**
 * Pure-logic tests for the reparent planner (drag a task onto another task's row to
 * make it a subtask). No database, no Compose: the planner only sees the visible
 * pre-order rows and must accept legal reparents while rejecting cycles, cross-section
 * and cross-calendar drops.
 */
@Suppress("TooManyFunctions")
class ManualSortReparentTest {
    private fun uuid(id: Long) = "uuid-$id"

    private fun t(
        id: Long,
        title: String = "T$id",
        parent: Long = 0,
        indent: Int = 0,
        order: Long = 0,
        calendar: String = "cal",
    ) = TaskContainer(
        task = Task(id = id, title = title, remoteId = uuid(id), parent = parent),
        caldavTask = CaldavTask(task = id, calendar = calendar),
        primarySort = if (indent == 0) order else 0,
        secondarySort = if (indent > 0) order else 0,
        indent = indent,
    )

    private fun list(vararg tasks: TaskContainer) =
        SectionedDataSource(tasks = tasks.toList(), disableHeaders = true)

    private fun sectionIndex(
        tasks: SectionedDataSource,
        id: Long,
    ): Int {
        for (i in 0 until tasks.size) {
            if (!tasks.isHeader(i) && tasks.getItem(i).id == id) return i
        }
        throw AssertionError("task $id not in list")
    }

    @Test
    fun dropOntoUnrelatedTaskIsPlanned() {
        val a = t(1, parent = 0, order = 1)
        val b = t(2, parent = 0, order = 2)
        val c = t(3, parent = 2, indent = 1, order = 1) // B already has a child
        val ds = list(a, b, c)

        val plan = ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 2))
        assertNotNull(plan)
        assertEquals(1, plan!!.task.id)
        assertEquals(2, plan.newParent.id)
    }

    @Test
    fun dropOntoSelfIsRejected() {
        val a = t(1)
        val b = t(2)
        val ds = list(a, b)
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 1)))
    }

    @Test
    fun dropOntoCurrentParentIsRejected() {
        // A is already B's child; dropping it "onto B" is a no-op (sibling placement
        // belongs to the gap zones).
        val b = t(2, order = 1)
        val a = t(1, parent = 2, indent = 1, order = 1)
        val ds = list(b, a)
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 2)))
    }

    @Test
    fun dropOntoOwnDescendantIsRejected() {
        // A (indent 0) with children B, C (indent 1); dropping A onto C would cycle.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, parent = 1, indent = 1, order = 2)
        val d = t(4, order = 2) // another top-level task after the subtree
        val ds = list(a, b, c, d)
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 3)))
    }

    @Test
    fun dropOntoOwnGrandchildIsRejected() {
        // A > B > C: dropping A onto C (two levels down) would also cycle.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, parent = 2, indent = 2, order = 1)
        val ds = list(a, b, c)
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 3)))
    }

    @Test
    fun dropOntoAncestorOutsideOwnSubtreeIsAllowed() {
        // A > B > C: dropping C onto A re-parents C to the top of the chain.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, parent = 2, indent = 2, order = 1)
        val ds = list(a, b, c)
        val plan = ds.planManualSortReparent(emptyMap(), taskId = 3, targetSectionIndex = sectionIndex(ds, 1))
        assertNotNull(plan)
        assertEquals(3, plan!!.task.id)
        assertEquals(1, plan.newParent.id)
    }

    @Test
    fun dropAcrossDifferentCalendarsIsRejected() {
        val a = t(1, calendar = "cal-a")
        val b = t(2, calendar = "cal-b")
        val ds = list(a, b)
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 1, targetSectionIndex = sectionIndex(ds, 2)))
    }

    @Test
    fun dropOntoHeaderIsRejected() {
        // Rows: header(0), A(1), B(2), header(3), C(4) — same shape as the planner
        // tests in ManualSortMoveTest.
        val a = t(1, order = 1).copy(sortGroup = 1000)
        val b = t(2, order = 2).copy(sortGroup = 1000)
        val c = t(3, order = 3).copy(sortGroup = 2000)
        val ds = SectionedDataSource(
            tasks = listOf(a, b, c),
            disableHeaders = false,
            groupMode = com.todoroo.astrid.core.SortHelper.SORT_LIST,
        )
        assertTrue(ds.isHeader(0))
        assertTrue(ds.isHeader(3))
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 3, targetSectionIndex = 0))
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 3, targetSectionIndex = 3))
    }

    // ------------------------------------------------------------------
    // reparentTargetForDrop - the horizontal-indent decision (native
    // findParentIndex): the nearest rendered row above the settled position whose
    // indent is at most the dragged task's becomes the new parent.

    @Test
    fun indentedInPlaceDragTargetsTheRowAbove() {
        // The TEST 2 case: A(0), B(0). Dragging B indented without any vertical
        // travel settles it on its own row; the parent candidate is still the
        // nearest row above - A.
        val a = t(1, order = 1)
        val b = t(2, order = 2)
        val ds = list(a, b)
        assertEquals(1L, ds.reparentTargetForDrop(emptyMap(), taskId = 2, dropIndex = 1))
    }

    @Test
    fun indentedDropBelowSiblingAtSameIndentTargetsThatSibling() {
        // The two-item case: A(0), B(0). Dragging A indented below B settles at row
        // 1; walking back, B (indent 0 <= 0) is the parent candidate.
        val a = t(1, order = 1)
        val b = t(2, order = 2)
        val ds = list(a, b)
        assertEquals(2L, ds.reparentTargetForDrop(emptyMap(), taskId = 1, dropIndex = 1))
    }

    @Test
    fun indentedDropAtTopOfListHasNoParent() {
        // Settling at row 0 leaves nothing above to become the parent; the caller
        // falls back to the plain sibling drop.
        val a = t(1, order = 1)
        val b = t(2, order = 2)
        val ds = list(a, b)
        assertNull(ds.reparentTargetForDrop(emptyMap(), taskId = 2, dropIndex = 0))
    }

    @Test
    fun indentedDropTargetsNearestShallowerRowNotFirstInList() {
        // P(0), C(1), D(1): dragging C indented below D settles at row 2; the walk
        // must stop at D (indent 1 <= 1) and not continue to P.
        val p = t(1, order = 1)
        val c = t(2, parent = 1, indent = 1, order = 1)
        val d = t(3, parent = 1, indent = 1, order = 2)
        val ds = list(p, c, d)
        assertEquals(3L, ds.reparentTargetForDrop(emptyMap(), taskId = 2, dropIndex = 2))
    }

    @Test
    fun indentedDropIntoSubtreeTargetsSubtreeOwner() {
        // P(0), Q(0), C(1, P's child): dragging Q indented below C settles at row 2;
        // C is deeper than Q so the walk continues to P, which becomes the parent.
        val p = t(1, order = 1)
        val q = t(2, order = 2)
        val c = t(3, parent = 1, indent = 1, order = 1)
        val ds = list(p, q, c)
        assertEquals(1L, ds.reparentTargetForDrop(emptyMap(), taskId = 2, dropIndex = 2))
    }

    @Test
    fun indentedDropWalkSkipsHiddenRows() {
        // P(0), Q(0), H(0, hidden), R(1, Q's child): dragging Q indented to settle at
        // row 2 puts the hidden H directly above the drop point in the pre-settle
        // list; the walk must skip it and target P (the rendered row above).
        val p = t(1, order = 1)
        val q = t(2, order = 2)
        val h = t(3, order = 3)
        val r = t(4, parent = 2, indent = 1, order = 1)
        val ds = list(p, q, h, r)
        val deletions = mapOf(subtaskKey(h.task) to false) // false = RowState.Hidden
        assertEquals(1L, ds.reparentTargetForDrop(deletions, taskId = 2, dropIndex = 2))
    }

    @Test
    fun indentedDropWalkStopsAtSectionHeader() {
        // header(0), A(1) | header(2), B(3): settling B at the top of its section
        // crosses a header walking back - no parent, plain sibling drop.
        val a = t(1, order = 1).copy(sortGroup = 1000)
        val b = t(2, order = 2).copy(sortGroup = 2000)
        val ds = SectionedDataSource(
            tasks = listOf(a, b),
            disableHeaders = false,
            groupMode = com.todoroo.astrid.core.SortHelper.SORT_LIST,
        )
        assertTrue(ds.isHeader(0))
        assertTrue(ds.isHeader(2))
        assertNull(ds.reparentTargetForDrop(emptyMap(), taskId = 2, dropIndex = 3))
    }

    // ------------------------------------------------------------------
    // planManualSortOutdent - a drag released more than half an indent step to the
    // left moves the task one indent level shallower at the settled position.

    @Test
    fun outdentInPlaceKeepsTheSlotAndGoesTopLevel() {
        // A(0), B(1, A's child): dragging B left without vertical travel keeps B at
        // row 1; as a top-level task that is after A (append).
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val ds = list(a, b)
        val plan = ds.planManualSortOutdent(emptyMap(), taskId = 2, dropIndex = 1)
        assertNotNull(plan)
        assertEquals(0L, plan!!.newParent)
        assertEquals(2L, plan.newPosition) // a.order + 1
    }

    @Test
    fun outdentAboveSiblingTakesItsSlot() {
        // A(0), B(0), C(1, B's child): dragging C left to row 1 (above B) makes C
        // top-level between A and B - it takes B's slot, B shifts down.
        val a = t(1, order = 1)
        val b = t(2, order = 2)
        val c = t(3, parent = 2, indent = 1, order = 1)
        val ds = list(a, b, c)
        val plan = ds.planManualSortOutdent(emptyMap(), taskId = 3, dropIndex = 1)
        assertNotNull(plan)
        assertEquals(0L, plan!!.newParent)
        assertEquals(2L, plan.newPosition) // b.order (take the slot)
    }

    @Test
    fun outdentBelowLastSiblingAppends() {
        // A(0), B(1, A's child), C(0): dragging B left to row 2 (below C) appends B
        // past the last top-level sibling.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, order = 3)
        val ds = list(a, b, c)
        val plan = ds.planManualSortOutdent(emptyMap(), taskId = 2, dropIndex = 2)
        assertNotNull(plan)
        assertEquals(0L, plan!!.newParent)
        assertEquals(4L, plan.newPosition) // c.order + 1
    }

    @Test
    fun outdentGrandchildLandsUnderNearestTopLevelRow() {
        // A(0), B(1, A's child), C(2, B's child): dragging C left in place moves it
        // one level shallower - under A, not to the top level.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, parent = 2, indent = 2, order = 1)
        val ds = list(a, b, c)
        val plan = ds.planManualSortOutdent(emptyMap(), taskId = 3, dropIndex = 2)
        assertNotNull(plan)
        assertEquals(1L, plan!!.newParent) // A
        assertEquals(2L, plan.newPosition) // b.order + 1 (append under A)
    }

    @Test
    fun outdentTopLevelTaskIsRejected() {
        val a = t(1, order = 1)
        val b = t(2, order = 2)
        val ds = list(a, b)
        assertNull(ds.planManualSortOutdent(emptyMap(), taskId = 2, dropIndex = 0))
        assertNull(ds.planManualSortOutdent(emptyMap(), taskId = 1, dropIndex = 1))
    }

    @Test
    fun outdentBelowOwnSubtreeKeepsTheSlotAmongNewSiblings() {
        // A(0), B(1, A's child), C(2, B's child), D(2, B's child): dragging B left
        // to row 3 (below its own grandchild) is legal - the subtree follows B, so
        // the settle lands at B's own slot among the new top-level siblings (after
        // A), keeping the visual order A, B, C, D with B one level shallower.
        val a = t(1, order = 1)
        val b = t(2, parent = 1, indent = 1, order = 1)
        val c = t(3, parent = 2, indent = 2, order = 1)
        val d = t(4, parent = 2, indent = 2, order = 2)
        val ds = list(a, b, c, d)
        val plan = ds.planManualSortOutdent(emptyMap(), taskId = 2, dropIndex = 3)
        assertNotNull(plan)
        assertEquals(0L, plan!!.newParent)
        assertEquals(2L, plan.newPosition) // append past A
    }

    // ------------------------------------------------------------------

    @Test
    fun crossSectionDropIsRejected() {
        // A and B in one section, C in the next: C onto A or B crosses a header.
        val a = t(1, order = 1).copy(sortGroup = 1000)
        val b = t(2, order = 2).copy(sortGroup = 1000)
        val c = t(3, order = 3).copy(sortGroup = 2000)
        val ds = SectionedDataSource(
            tasks = listOf(a, b, c),
            disableHeaders = false,
            groupMode = com.todoroo.astrid.core.SortHelper.SORT_LIST,
        )
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 3, targetSectionIndex = sectionIndex(ds, 1)))
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 3, targetSectionIndex = sectionIndex(ds, 2)))
        // ...and symmetrically B onto C's row.
        assertNull(ds.planManualSortReparent(emptyMap(), taskId = 2, targetSectionIndex = sectionIndex(ds, 3)))
    }
}
