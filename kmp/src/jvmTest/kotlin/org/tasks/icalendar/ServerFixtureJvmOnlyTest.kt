package org.tasks.icalendar

import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Real-world fixtures that still round-trip differently with libical on iOS.
// Move each one to ServerFixtureRoundTripTest once iOS matches.
class ServerFixtureJvmOnlyTest {
    // iOS reads the value of a VALUE=URI X- property as empty, and re-quotes quoted parameters.
    @Test
    fun appleStructuredLocationKeepsItsValueAndQuotedParameters() {
        val todo = assertRoundTrips(APPLE_STRUCTURED_LOCATION)

        assertEquals(
            ICalProperty(
                "X-APPLE-STRUCTURED-LOCATION",
                "geo:37.331741,-122.030333",
                listOf(
                    "VALUE" to "URI",
                    "X-ADDRESS" to "1 Main St, Springfield",
                    "X-APPLE-RADIUS" to "100",
                    "X-TITLE" to "Home",
                ),
            ),
            todo.alarms.single().otherProperties.last(),
        )
    }

    // iOS writes the quoted parameter back as X-LABEL="^'a:b;c^'" and drops the empty X- property.
    @Test
    fun keepsQuotedParametersAndEmptyVendorProperties() {
        val todo = assertRoundTrips(QUOTED_AND_EMPTY_X_PROPERTIES)

        assertEquals(
            listOf(
                ICalProperty("X-VENDOR-LINK", "https://example.com/a?b=c", listOf("X-LABEL" to "a:b;c")),
                ICalProperty("X-EMPTY", ""),
            ),
            todo.unknownProperties,
        )
    }

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
        private val APPLE_STRUCTURED_LOCATION = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//iOS 17.2//EN
            BEGIN:VTODO
            UID:7B2C9E44-3F1A-4C8D-9E2B-5A6F7D8C9B0A
            SUMMARY:Buy milk
            BEGIN:VALARM
            X-WR-ALARMUID:5C7D9E1F-2A3B-4C5D-8E9F-0A1B2C3D4E5F
            TRIGGER;VALUE=DATE-TIME:19760401T005545Z
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            X-APPLE-PROXIMITY:ARRIVE
            X-APPLE-STRUCTURED-LOCATION;VALUE=URI;X-ADDRESS="1 Main St, Springfield";X-APPLE-RADIUS=100;X-TITLE=Home:geo:37.331741,-122.030333
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val QUOTED_AND_EMPTY_X_PROPERTIES = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Other Client//EN
            BEGIN:VTODO
            UID:x-properties
            SUMMARY:Vendor data
            X-VENDOR-LINK;X-LABEL="a:b;c":https://example.com/a?b=c
            X-EMPTY:
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

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
