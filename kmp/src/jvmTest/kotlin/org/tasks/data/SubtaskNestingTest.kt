package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtaskNestingTest {
    @Test
    fun aRowMayBeNestedOneLevelInsideTheOneAboveIt() {
        assertEquals(1, deepestNestingUnder(indent = 0))
        assertEquals(3, deepestNestingUnder(indent = 2))
    }

    @Test
    fun nothingMayBeNestedInsideARowOnItsWayOut() {
        assertEquals(1, deepestNestingUnder(indent = 1, deleted = true))
    }

    @Test
    fun theParentOfADropIsTheFirstShallowerRowAboveIt() {
        val indents = listOf(0, 1, 2, 1)

        assertEquals(0, findParentIndex(indent = 1, landing = 4) { indents[it] })
        assertEquals(3, findParentIndex(indent = 2, landing = 4) { indents[it] })
    }

    @Test
    fun theTopLevelHasNoParent() {
        val indents = listOf(0, 1)

        assertNull(findParentIndex(indent = 0, landing = 2) { indents[it] })
        assertNull(findParentIndex(indent = 1, landing = 0) { indents[it] })
    }
}
