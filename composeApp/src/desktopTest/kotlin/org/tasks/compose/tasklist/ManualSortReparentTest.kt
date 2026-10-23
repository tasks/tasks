package org.tasks.compose.tasklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
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
