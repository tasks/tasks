package org.tasks.icalendar

import kotlin.test.Test
import org.tasks.repeats.Recur
import kotlin.test.assertEquals

class RecurrenceRoundTripTest {
    @Test
    fun readsRulePartsTheAppDoesNotModel() {
        val todo = parseVTodos(vtodo("RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD")).single()

        assertEquals(
            listOf("RSCALE" to "HEBREW", "SKIP" to "FORWARD"),
            todo.rRule?.unknownParts,
        )
    }

    @Test
    fun writesRulePartsTheAppDoesNotModelBackToTheServer() {
        val todo = parseVTodos(vtodo("RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD")).single()

        assertEquals(
            Recur.parse("RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD"),
            parseVTodos(todo.serialize()).single().rRule,
        )
    }

    @Test
    fun roundTripsAnOrdinaryRule() {
        val todo = parseVTodos(vtodo("FREQ=WEEKLY;INTERVAL=2;WKST=SU;BYDAY=MO,WE")).single()

        assertEquals("FREQ=WEEKLY;WKST=SU;INTERVAL=2;BYDAY=MO,WE", todo.rRule?.toString())
        assertEquals(todo.rRule, parseVTodos(todo.serialize()).single().rRule)
    }

    companion object {
        private fun vtodo(rrule: String) = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Some Server//EN
            BEGIN:VTODO
            UID:repeating
            SUMMARY:Light the candles
            DUE:20260116T100000Z
            RRULE:$rrule
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
