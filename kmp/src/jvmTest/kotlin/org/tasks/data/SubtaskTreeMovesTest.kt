package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Task

class SubtaskTreeMovesTest {
    private fun rows(vararg depths: Int): List<SubtaskRow> {
        val parents = mutableMapOf(0 to "root")
        return depths.mapIndexed { index, indent ->
            val key = "k$index"
            parents[indent + 1] = key
            SubtaskRow(
                node = SubtaskNode(
                    key = key,
                    parentKey = parents.getValue(indent),
                    sequence = index.toLong(),
                    task = Task(id = index + 1L, title = "t$index"),
                ),
                indent = indent,
            )
        }
    }

    private fun List<SubtaskRow>.deleting(index: Int): List<SubtaskRow> =
        mapIndexed { at, row ->
            if (at == index) row.copy(node = row.node.copy(deleted = true)) else row
        }

    private fun List<SubtaskRow>.collapsing(index: Int): List<SubtaskRow> =
        mapIndexed { at, row ->
            if (at == index) {
                row.copy(
                    node = row.node.copy(task = row.node.task.copy(isCollapsed = true)),
                    children = 1,
                )
            } else {
                row
            }
        }

    @Test
    fun aRowCannotNestUnderOneOnItsWayOut() {
        val rows = rows(0, 0, 0).deleting(1)

        val range = rows.dropRange(from = 0, to = 1)

        assertEquals(0..0, range)
    }

    @Test
    fun aRowCanNestUnderOneThatIsFoldedAway() {
        val rows = rows(0, 0, 0).collapsing(1)

        val range = rows.dropRange(from = 0, to = 1)

        assertEquals(0..1, range)
    }

    @Test
    fun aDropBelowARowOnItsWayOutLandsBesideIt() {
        val rows = rows(0, 1, 1).deleting(1)

        val landing = rows.resolveMove(from = 2, to = 1, rootKey = "root", desiredIndent = 2)

        assertEquals(SubtaskLanding(parentKey = "k0", after = null, indent = 1), landing)
    }

    @Test
    fun aRowDraggedBelowAnotherCanNestUnderIt() {
        val rows = rows(0, 0, 0)

        val range = rows.dropRange(from = 0, to = 1)

        assertEquals(0..1, range)
    }

    @Test
    fun aRowDraggedToTheTopCannotNest() {
        val rows = rows(0, 0, 0)

        val range = rows.dropRange(from = 1, to = 0)

        assertEquals(0..0, range)
    }

    @Test
    fun aRowStaysWhereItIsWhenNothingIsAboveIt() {
        val rows = rows(0, 1)

        val range = rows.dropRange(from = 0, to = 0)

        assertEquals(0..0, range)
    }

    @Test
    fun droppingBelowARowTakesTheDepthTheDragAskedFor() {
        val rows = rows(0, 0, 0)

        val landing = rows.resolveMove(from = 0, to = 1, rootKey = "root", desiredIndent = 1)

        assertEquals("k1", landing?.parentKey)
        assertEquals(1, landing?.indent)
    }

    @Test
    fun aDepthTheDropPositionCannotHoldIsClampedToWhatItCan() {
        val rows = rows(0, 0, 0)

        val landing = rows.resolveMove(from = 1, to = 0, rootKey = "root", desiredIndent = 1)

        assertEquals("root", landing?.parentKey)
        assertEquals(0, landing?.indent)
    }

    @Test
    fun aRowDroppedBelowASubtreeLandsAfterAllOfIt() {
        val rows = rows(0, 1, 1, 0)

        val landing = rows.resolveMove(from = 3, to = 0, rootKey = "root")

        assertEquals("root", landing?.parentKey)
        assertEquals(null, landing?.after)
    }

    @Test
    fun aRowKeepsItsDepthWhereTheDropAllowsIt() {
        val rows = rows(0, 1, 0)

        val landing = rows.resolveMove(from = 1, to = 2, rootKey = "root")

        assertEquals("k2", landing?.parentKey)
        assertEquals(1, landing?.indent)
    }

    @Test
    fun whatIsNestedInARowDoesNotConstrainWhereItCanGo() {
        val rows = rows(0, 1, 2, 0)

        assertEquals(0..1, rows.dropRange(from = 0, to = 3))
    }

    @Test
    fun aPreviewOfADropOntoAnExpandedParentAgreesWithWhereItLands() {
        val rows = rows(0, 0, 1, 0)

        val range = rows.dropRange(from = 0, to = 1)
        val landing = rows.resolveMove(from = 0, to = 1, rootKey = "root", desiredIndent = 0)

        assertEquals(0..2, range)
        assertEquals(SubtaskLanding(parentKey = "root", after = "k1", indent = 0), landing)
    }

    @Test
    fun aRowCannotBeDroppedInsideItself() {
        val rows = rows(0, 1)

        assertEquals(null, rows.resolveMove(from = 0, to = 1, rootKey = "root"))
    }

    @Test
    fun aListThatCannotNestOffersNothingDeeperThanTheTopLevel() {
        val rows = rows(0, 0)

        assertEquals(0..0, rows.dropRange(from = 1, to = 1, allowsNesting = false))
    }

    @Test
    fun aRowAlreadyNestedOnSuchAListIsStillOfferedEveryDepthUpToItsOwn() {
        val rows = rows(0, 1, 2)

        assertEquals(0..2, rows.dropRange(from = 2, to = 2, allowsNesting = false))
    }

    @Test
    fun aListThatCanNestOffersTheDepthBelowTheRowAboveInstead() {
        val rows = rows(0, 0)

        assertEquals(0..1, rows.dropRange(from = 1, to = 1, allowsNesting = true))
    }
}
