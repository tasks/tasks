package org.tasks.icalendar

import kotlin.test.Test
import kotlin.test.assertEquals

class ICalTextRoundTripTest {
    @Test
    fun keepsSeparatorsAndBackslashesInText() {
        val summary = """Buy milk, eggs; and \butter\"""

        assertEquals(summary, roundTrip(VTodo(uid = "u", summary = summary)).summary)
    }

    @Test
    fun keepsNewlinesInDescriptions() {
        val description = "first line\nsecond line\nthird"

        assertEquals(description, roundTrip(VTodo(uid = "u", description = description)).description)
    }

    @Test
    fun keepsCommasInsideASingleCategory() {
        val todo = VTodo(uid = "u").apply { categories.addAll(listOf("home, office", "errands")) }

        assertEquals(listOf("home, office", "errands"), roundTrip(todo).categories.toList())
    }

    @Test
    fun keepsNonAsciiAndAstralCharacters() {
        val summary = "Ich möchte 日本語 😀 テスト"

        assertEquals(summary, roundTrip(VTodo(uid = "u", summary = summary)).summary)
    }

    @Test
    fun keepsLongTextThatHasToBeFolded() {
        val summary = "😀 ".repeat(60).trim()

        assertEquals(summary, roundTrip(VTodo(uid = "u", summary = summary)).summary)
    }

    @Test
    fun keepsTextThatLooksLikeAnIcalendarStructure() {
        val description = "BEGIN:VTODO\nUID:not-really\nEND:VTODO"

        assertEquals(description, roundTrip(VTodo(uid = "u", description = description)).description)
    }

    private fun roundTrip(todo: VTodo): VTodo = parseVTodos(todo.serialize()).single()
}
