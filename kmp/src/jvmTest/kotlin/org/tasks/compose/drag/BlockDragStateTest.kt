package org.tasks.compose.drag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockDragStateTest {
    private fun state(vararg heights: Pair<Int, Int>) = BlockDragState().apply {
        heights.forEach { (index, height) -> bounds.put(index, index * 100f, height) }
    }

    @Test
    fun nothingIsCarriedUntilADragStarts() {
        val block = state()

        assertEquals(IntRange.EMPTY, block.carried)
        assertEquals(0, block.reservedPx)
        assertEquals(-1, block.draggedIndex)
        assertFalse(block.isCarried(0))
        assertFalse(block.isDragging(0))
    }

    @Test
    fun theRowsUnderTheDraggedOneAreCarriedAndTheirHeightHeldOpen() {
        val block = state(1 to 40, 2 to 60, 3 to 30)

        block.started(index = 0, block = 1..2)

        assertTrue(block.isCarried(1))
        assertTrue(block.isCarried(2))
        assertFalse(block.isCarried(3))
        assertEquals(100, block.reservedPx)
        assertTrue(block.isDragging(0))
        assertFalse(block.isDragging(1))
    }

    @Test
    fun aRowWithNothingUnderItCarriesNothingAndHoldsNoHeightOpen() {
        val block = state(0 to 40, 1 to 40)

        block.started(index = 0, block = IntRange.EMPTY)

        assertEquals(0, block.reservedPx)
        assertTrue(block.isDragging(0))
        assertFalse(block.isCarried(1))
    }

    @Test
    fun aDropThatMovedSomethingLeavesTheRebuildToTheListItChanged() {
        val block = state(1 to 40)
        block.started(index = 0, block = 1..1)
        val before = block.rebuildKey

        block.stopped(IndentDrop.Move(landing = 3, indent = 1))

        assertEquals(before, block.rebuildKey)
        assertEquals(IntRange.EMPTY, block.carried)
        assertEquals(0, block.reservedPx)
        assertEquals(-1, block.draggedIndex)
    }

    @Test
    fun aDropThatChangedNothingAsksForTheRebuildItself() {
        val block = state(1 to 40)
        block.started(index = 0, block = 1..1)
        val before = block.rebuildKey

        block.stopped(IndentDrop.Nothing)

        assertEquals(before + 1, block.rebuildKey)
    }

    @Test
    fun aReNestAsksForNoRebuildEither() {
        val block = state()
        block.started(index = 0, block = IntRange.EMPTY)
        val before = block.rebuildKey

        block.stopped(IndentDrop.ReNest(indent = 1))

        assertEquals(before, block.rebuildKey)
    }

    @Test
    fun aDropOntoTheBlockTheRowIsCarryingMeansNothing() {
        val block = state()
        block.started(index = 2, block = 3..5)

        assertEquals(null, block.landingOf(2, 2))
        assertEquals(null, block.landingOf(2, 3))
        assertEquals(null, block.landingOf(2, 5))
        assertEquals(6, block.landingOf(2, 6))
        assertEquals(1, block.landingOf(2, 1))
    }

    @Test
    fun everythingIsALandingOnceTheDragIsOver() {
        val block = state()
        block.started(index = 2, block = 3..5)
        block.stopped(IndentDrop.Nothing)

        assertEquals(3, block.landingOf(2, 3))
    }
}
