package org.tasks.icalendar

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class VAlarmParsingTest {
    @Test
    fun dropsAnAlarmWhoseTriggerLibicalCannotParse() {
        listOf(
            "TRIGGER;RELATED=START:-P1M",
            "TRIGGER:not-a-duration",
            "TRIGGER:",
        ).forEach { trigger ->
            val todo = parseVTodos(alarmStartingWith(trigger)).single()

            assertEquals(emptyList(), todo.alarms, trigger)
            assertFalse(todo.serialize().contains("BEGIN:VALARM"), trigger)
        }
    }

    @Test
    fun keepsAnAlarmThatSimplyHasNoTrigger() {
        val todo = parseVTodos(alarmStartingWith("X-WR-ALARMUID:keepme")).single()

        assertNull(todo.alarms.single().trigger)
        assertContains(todo.serialize(), "X-WR-ALARMUID:keepme")
    }

    private fun alarmStartingWith(property: String) = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Other Client//EN
        BEGIN:VTODO
        UID:alarm-parsing
        SUMMARY:Task
        DUE:20260116T090000Z
        BEGIN:VALARM
        $property
        ACTION:DISPLAY
        DESCRIPTION:Reminder
        END:VALARM
        END:VTODO
        END:VCALENDAR
    """.trimIndent()
}
