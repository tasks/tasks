package org.tasks.compose.drag

import org.junit.Assert.assertEquals
import org.junit.Test

class RowBoundsTest {
    private fun rows(count: Int = 3, height: Int = 40) = RowBounds().apply {
        repeat(count) { put(it, (it * height).toFloat(), height) }
    }

    @Test
    fun aRowThatHasNotMovedIsOverItself() {
        assertEquals(0, rows().targetIndex(from = 0, dy = 0f, count = 3))
    }

    @Test
    fun aRowDraggedLessThanHalfWayIsStillOverItself() {
        assertEquals(0, rows().targetIndex(from = 0, dy = 19f, count = 3))
    }

    @Test
    fun aRowDraggedPastTheOneBelowIsOverIt() {
        assertEquals(1, rows().targetIndex(from = 0, dy = 21f, count = 3))
    }

    @Test
    fun aRowDraggedPastTwoIsOverTheFurthestItPassed() {
        assertEquals(2, rows().targetIndex(from = 0, dy = 61f, count = 3))
    }

    @Test
    fun aRowDraggedUpwardsIsOverTheFirstOneItPassed() {
        assertEquals(1, rows().targetIndex(from = 2, dy = -21f, count = 3))
    }

    @Test
    fun aRowDraggedToTheTopIsOverTheFirst() {
        assertEquals(0, rows().targetIndex(from = 2, dy = -61f, count = 3))
    }

    @Test
    fun rowsOfDifferentHeightsArePassedAtTheirOwnMiddles() {
        val bounds = RowBounds().apply {
            put(0, 0f, 40)
            put(1, 40f, 80)
            put(2, 120f, 40)
        }

        assertEquals(0, bounds.targetIndex(from = 0, dy = 39f, count = 3))
        assertEquals(1, bounds.targetIndex(from = 0, dy = 41f, count = 3))
    }

    @Test
    fun rowsNotYetLaidOutAreLeftAlone() {
        assertEquals(1, RowBounds().targetIndex(from = 1, dy = 100f, count = 3))
    }

    @Test
    fun measureHowTallARunOfRowsIs() {
        assertEquals(80, rows().heightOf(1..2))
    }
}
