package org.tasks.compose.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.entity.Task

class CaretHandoffTest {
    private fun rows(count: Int, vararg deleted: Int): List<SubtaskRow> =
        List(count) { index ->
            SubtaskRow(
                node = SubtaskNode(
                    key = "k$index",
                    parentKey = "root",
                    sequence = index.toLong(),
                    task = Task(id = index + 1L, title = "t$index"),
                    deleted = index in deleted,
                ),
                indent = 0,
            )
        }

    private fun handoffTo(key: String, landing: CaretLanding = CaretLanding.TextEnd) =
        CaretHandoff().apply { handTo(key, landing) }

    @Test
    fun onlyTheRowItNamesIsOfferedTheCaret() {
        val handoff = handoffTo("k1", CaretLanding.LineEnd)

        assertEquals(CaretLanding.LineEnd, handoff.landingFor("k1"))
        assertNull(handoff.landingFor("k0"))
        assertNull(handoff.landingFor("k2"))
    }

    @Test
    fun theRowThatTookTheCaretEndsTheHandoff() {
        val handoff = handoffTo("k1")

        handoff.placed()

        assertNull(handoff.arriving)
        assertNull(handoff.landingFor("k1"))
    }

    @Test
    fun aHandoffToARowThatIsStillThereStands() {
        val handoff = handoffTo("k1")

        handoff.standDownIfGone(rows(3))

        assertEquals(CaretLanding.TextEnd, handoff.landingFor("k1"))
    }

    @Test
    fun aHandoffToARowThatHasGoneIsStoodDown() {
        val handoff = handoffTo("k9")

        handoff.standDownIfGone(rows(3))

        assertNull(handoff.arriving)
    }

    @Test
    fun aHandoffToARowOnItsWayOutIsStoodDown() {
        val handoff = handoffTo("k1")

        handoff.standDownIfGone(rows(3, 1))

        assertNull(handoff.arriving)
    }

    @Test
    fun aRowMarkedForDeletionCannotTakeTheCaret() {
        assertTrue(rows(3).canTakeCaret("k1"))
        assertFalse(rows(3, 1).canTakeCaret("k1"))
        assertFalse(rows(3).canTakeCaret("k9"))
    }
}
