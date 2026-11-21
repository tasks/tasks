package org.tasks.icalendar

import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Real-world fixtures that still round-trip differently with libical on iOS.
// Move each one to ServerFixtureRoundTripTest once iOS matches.
class ServerFixtureJvmOnlyTest {
    // iOS keeps TZID "W. Europe Standard Time", writes the time in UTC and loses the zone on the next read.
    @Test
    fun mapsAnExchangeTimeZoneToAnOlsonId() {
        val todo = assertRoundTrips(EXCHANGE_WINDOWS_TIME_ZONE)

        val eightUtc = DateTime(2026, 1, 16, 8, 0, timeZone = DateTime.UTC).millis
        assertEquals(ICalDate.DateTime(eightUtc, "Europe/Vienna"), todo.due)
        assertTrue("DUE;TZID=Europe/Vienna:20260116T090000" in todo.serialize().replace("\r\n ", "").lines())
    }

    private fun assertRoundTrips(iCalendar: String): VTodo {
        val parsed = parseVTodos(iCalendar).single()

        val again = parseVTodos(parsed.serialize()).single()

        assertEquals(parsed.copy(dtStamp = null), again.copy(dtStamp = null))
        return parsed
    }

    companion object {
        private val EXCHANGE_WINDOWS_TIME_ZONE = """
            BEGIN:VCALENDAR
            METHOD:PUBLISH
            PRODID:Microsoft Exchange Server 2010
            VERSION:2.0
            BEGIN:VTIMEZONE
            TZID:W. Europe Standard Time
            BEGIN:STANDARD
            DTSTART:16010101T030000
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10
            END:STANDARD
            BEGIN:DAYLIGHT
            DTSTART:16010101T020000
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3
            END:DAYLIGHT
            END:VTIMEZONE
            BEGIN:VTODO
            UID:040000008200E00074C5B7101A82E008
            SUMMARY:Submit expenses
            DUE;TZID=W. Europe Standard Time:20260116T090000
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
