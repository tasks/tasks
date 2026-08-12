package org.tasks.compose.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.entity.Task

class SubtaskTitleFieldTest {
    private fun rows(count: Int, vararg deleted: Int): List<SubtaskRow> =
        nested(List(count) { 0 }, *deleted)

    private fun nested(indents: List<Int>, vararg deleted: Int): List<SubtaskRow> =
        indents.mapIndexed { index, indent ->
            SubtaskRow(
                node = SubtaskNode(
                    key = "k$index",
                    parentKey = if (indent == 0) "root" else "k${index - 1}",
                    sequence = index.toLong(),
                    task = Task(id = index + 1L, title = "t$index"),
                    deleted = index in deleted,
                ),
                indent = indent,
            )
        }

    @Test
    fun theNextRowDown() {
        assertEquals(1, rows(3).nextEditable(from = 0, step = 1))
    }

    @Test
    fun theNextRowUp() {
        assertEquals(1, rows(3).nextEditable(from = 2, step = -1))
    }

    @Test
    fun stepOverARowOnItsWayOut() {
        assertEquals(2, rows(3, 1).nextEditable(from = 0, step = 1))
    }

    @Test
    fun stepOverAsManyAsItTakes() {
        assertEquals(4, rows(5, 1, 2, 3).nextEditable(from = 0, step = 1))
    }

    @Test
    fun stepOverThemGoingUpToo() {
        assertEquals(0, rows(4, 1, 2).nextEditable(from = 3, step = -1))
    }

    @Test
    fun stayPutWhenEverythingThatWayIsOnItsWayOut() {
        assertNull(rows(3, 1, 2).nextEditable(from = 0, step = 1))
    }

    @Test
    fun stayPutAtTheEndOfTheList() {
        assertNull(rows(3).nextEditable(from = 2, step = 1))
        assertNull(rows(3).nextEditable(from = 0, step = -1))
    }

    @Test
    fun backspaceJoinsOntoTheEndOfTheRowAbove() {
        assertEquals(
            "k0" to CaretLanding.TextEnd,
            rows(3).caretAfterRemoving(from = 1, keepsNested = false),
        )
    }

    @Test
    fun backspaceOnTheFirstRowJoinsOntoTheFrontOfTheOneBelow() {
        assertEquals(
            "k1" to CaretLanding.TextStart,
            rows(3).caretAfterRemoving(from = 0, keepsNested = false),
        )
    }

    @Test
    fun backspaceStepsOverRowsOnTheirWayOut() {
        assertEquals(
            "k0" to CaretLanding.TextEnd,
            rows(4, 1).caretAfterRemoving(from = 2, keepsNested = false),
        )
    }

    @Test
    fun backspaceOnTheOnlyRowHandsTheCaretNowhere() {
        assertNull(rows(1).caretAfterRemoving(from = 0, keepsNested = false))
    }

    @Test
    fun backspaceStepsPastWhatGoesWithTheRowItRemoves() {
        assertEquals(
            "k2" to CaretLanding.TextStart,
            nested(listOf(0, 1, 0)).caretAfterRemoving(from = 0, keepsNested = false),
        )
    }

    @Test
    fun backspaceOnAnUnsavedRowHandsTheCaretToItsOwnChild() {
        assertEquals(
            "k1" to CaretLanding.TextStart,
            nested(listOf(0, 1, 0)).caretAfterRemoving(from = 0, keepsNested = true),
        )
    }

    @Test
    fun theArrowsLandOnTheLineTheyEnterOn() {
        assertEquals("k1" to CaretLanding.LineEnd, rows(3).caretAfterMoving(from = 0, step = 1))
        assertEquals("k1" to CaretLanding.TextEnd, rows(3).caretAfterMoving(from = 2, step = -1))
        assertNull(rows(3).caretAfterMoving(from = 2, step = 1))
    }
}
