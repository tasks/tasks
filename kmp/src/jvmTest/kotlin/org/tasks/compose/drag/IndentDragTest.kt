package org.tasks.compose.drag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IndentDragTest {
    @Test
    fun aRowFollowsThePointerToTheNearestLevel() {
        assertEquals(0, indentPreview(base = 0, draggedX = 9f, step = 20f, range = 0..2))
        assertEquals(1, indentPreview(base = 0, draggedX = 11f, step = 20f, range = 0..2))
        assertEquals(2, indentPreview(base = 0, draggedX = 31f, step = 20f, range = 0..2))
    }

    @Test
    fun aRowDoesNotPreviewADepthTheSlotWouldNotAllow() {
        assertEquals(1, indentPreview(base = 0, draggedX = 200f, step = 20f, range = 0..1))
        assertEquals(1, indentPreview(base = 2, draggedX = -200f, step = 20f, range = 1..2))
    }

    @Test
    fun aRowDraggedBackwardsComesOutAgain() {
        assertEquals(0, indentPreview(base = 2, draggedX = -41f, step = 20f, range = 0..2))
    }

    @Test
    fun aDragThatOnlyWentSidewaysReNestsWhereItStands() {
        assertEquals(IndentDrop.ReNest(1), indentDrop(landing = null, target = 1, base = 0))
    }

    @Test
    fun aDragThatWentNowhereAtAllDoesNothing() {
        assertEquals(IndentDrop.Nothing, indentDrop(landing = null, target = 0, base = 0))
        assertEquals(IndentDrop.Nothing, indentDrop(landing = null, target = null, base = 0))
    }

    @Test
    fun aDragThatMovedHandsItsDepthToTheMove() {
        assertEquals(IndentDrop.Move(3, 1), indentDrop(landing = 3, target = 1, base = 0))
        assertEquals(IndentDrop.Move(3, null), indentDrop(landing = 3, target = null, base = 0))
    }

    @Test
    fun onlyADropThatChangedNothingAsksForTheColumnToBeRebuilt() {
        assertEquals(IndentDrop.Nothing, indentDrop(landing = null, target = null, base = 0))
        assertNotEquals(IndentDrop.Nothing, indentDrop(landing = null, target = 1, base = 0))
        assertNotEquals(IndentDrop.Nothing, indentDrop(landing = 3, target = null, base = 0))
    }

    @Test
    fun tabNestsAndShiftTabComesBackOut() {
        assertEquals(1, tabTarget(indent = 0, shift = false, range = 0..1))
        assertEquals(0, tabTarget(indent = 1, shift = true, range = 0..1))
    }

    @Test
    fun aTabWithNowhereToGoLeavesTheRowWhereItIs() {
        assertEquals(0, tabTarget(indent = 0, shift = false, range = 0..0))
        assertEquals(0, tabTarget(indent = 0, shift = true, range = 0..2))
    }
}
