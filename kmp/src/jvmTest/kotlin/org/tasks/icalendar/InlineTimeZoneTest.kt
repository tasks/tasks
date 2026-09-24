package org.tasks.icalendar

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class InlineTimeZoneTest {
    @Test
    fun readsATimeZoneDefinedInlineAsUtc() {
        val todo = parseVTodos(CUSTOM_TIME_ZONE).single()

        assertEquals(ICalDate.DateTime(TEN_AM_UTC), todo.due)
        assertContains(todo.serialize().unfolded(), "DUE:20260116T100000Z")
    }

    private fun String.unfolded(): List<String> = replace("\r\n ", "").replace("\n ", "").lines()

    companion object {
        private const val TEN_AM_UTC = 1768557600000L

        private val CUSTOM_TIME_ZONE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Some Exchange Server//EN
            BEGIN:VTIMEZONE
            TZID:Customized Time Zone
            BEGIN:STANDARD
            DTSTART:16010101T000000
            TZOFFSETFROM:-0100
            TZOFFSETTO:-0100
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VTODO
            UID:custom-zone
            SUMMARY:Water the plants
            DUE;TZID=Customized Time Zone:20260116T090000
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
