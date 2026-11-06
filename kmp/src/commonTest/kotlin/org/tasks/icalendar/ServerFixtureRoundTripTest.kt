package org.tasks.icalendar

import kotlinx.datetime.TimeZone
import org.tasks.repeats.ByDay
import org.tasks.repeats.Frequency
import org.tasks.repeats.Recur
import org.tasks.repeats.Until
import org.tasks.repeats.Weekday
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// RRULE text is compared as a model only: libical writes rule parts in its own order (FREQ;INTERVAL;COUNT).
class ServerFixtureRoundTripTest {
    @Test
    fun nextcloudZonedSubtask() {
        val todo = assertRoundTrips(NEXTCLOUD_ZONED_SUBTASK)

        val berlin = TimeZone.of("Europe/Berlin")
        assertEquals(ICalDate.DateTime(DateTime(2026, 1, 16, 8, 0, timeZone = berlin).millis, "Europe/Berlin"), todo.dtStart)
        assertEquals(ICalDate.DateTime(DateTime(2026, 1, 16, 17, 0, timeZone = berlin).millis, "Europe/Berlin"), todo.due)
        assertEquals(listOf(RelatedTo("1b3c6d7e-2f4a-4c5b-8d9e-0a1b2c3d4e5f")), todo.relatedTo)
        assertEquals(TodoStatus.IN_PROCESS, todo.status)
        assertEquals(40, todo.percentComplete)
        assertEquals(listOf(ICalProperty("X-OC-HIDESUBTASKS", "0")), todo.unknownProperties)
        val lines = todo.serialize().unfolded()
        assertLine(lines, "DUE;TZID=Europe/Berlin:20260116T170000")
        assertLine(lines, "TZID:Europe/Berlin")
        assertLine(lines, "RELATED-TO:1b3c6d7e-2f4a-4c5b-8d9e-0a1b2c3d4e5f")
    }

    @Test
    fun appleRemindersRepeatingUntilWithAlarms() {
        val todo = assertRoundTrips(APPLE_REPEATING_UNTIL)

        val losAngeles = TimeZone.of("America/Los_Angeles")
        val seven = DateTime(2026, 1, 12, 19, 0, timeZone = losAngeles).millis
        assertEquals(1, todo.sequence)
        assertEquals(ICalDate.DateTime(seven, "America/Los_Angeles"), todo.due)
        assertEquals(
            Recur(
                Frequency.WEEKLY,
                until = Until.DateTime(utc(2026, 3, 30, 6, 59, 59), utc = true),
                byDay = listOf(ByDay(Weekday.MO)),
            ),
            todo.rRule,
        )
        assertEquals(listOf(RelatedTo("0D4E5F6A-7B8C-4D9E-A0B1-C2D3E4F5A6B7", "PARENT")), todo.relatedTo)
        assertEquals(listOf(ICalProperty("X-APPLE-SORT-ORDER", "726245812")), todo.unknownProperties)
        assertEquals(
            listOf(Trigger.Absolute(utc(2026, 1, 13, 3, 0, 0)), Trigger.Absolute(utc(1976, 4, 1, 0, 55, 45))),
            todo.alarms.map { it.trigger },
        )
        assertEquals(
            listOf(
                ICalProperty("X-WR-ALARMUID", "5C7D9E1F-2A3B-4C5D-8E9F-0A1B2C3D4E5F"),
                ICalProperty("UID", "5C7D9E1F-2A3B-4C5D-8E9F-0A1B2C3D4E5F"),
                ICalProperty("X-APPLE-PROXIMITY", "ARRIVE"),
            ),
            todo.alarms[1].otherProperties,
        )
        val lines = todo.serialize().unfolded()
        assertLine(lines, "RELATED-TO;RELTYPE=PARENT:0D4E5F6A-7B8C-4D9E-A0B1-C2D3E4F5A6B7")
        assertLine(lines, "TZID:America/Los_Angeles")
    }

    @Test
    fun fastmailAllDayRepeatingUntilDate() {
        val todo = assertRoundTrips(FASTMAIL_ALL_DAY_UNTIL_DATE)

        assertEquals(ICalDate.Date(2026, 1, 2), todo.dtStart)
        assertEquals(ICalDate.Date(2026, 1, 2), todo.due)
        assertEquals(Recur(Frequency.DAILY, until = Until.Date(2026, 2, 15)), todo.rRule)
        assertEquals(
            listOf(VAlarm(Trigger.Relative(-86_400_000L, value = "-P1D"), action = "DISPLAY", description = "Pay rent")),
            todo.alarms.map { it.copy(propertyOrder = emptyList()) },
        )
        val lines = todo.serialize().unfolded()
        assertLine(lines, "DUE;VALUE=DATE:20260102")
    }

    @Test
    fun radicaleFloatingTaskFromThunderbird() {
        val todo = assertRoundTrips(RADICALE_THUNDERBIRD_FLOATING)

        assertEquals(ICalDate.DateTime(DateTime(2026, 1, 16, 9, 0).millis, floating = true), todo.dtStart)
        assertEquals(ICalDate.DateTime(DateTime(2026, 1, 16, 10, 0).millis, floating = true), todo.due)
        assertEquals(Recur(Frequency.DAILY, interval = 2, count = 10), todo.rRule)
        assertEquals(
            listOf(RelatedTo("sibling-uid", "SIBLING"), RelatedTo("child-uid", "CHILD")),
            todo.relatedTo,
        )
        assertEquals(
            listOf(
                ICalProperty("X-MOZ-GENERATION", "2"),
                ICalProperty("X-MOZ-LASTACK", "20260108T101700Z"),
            ),
            todo.unknownProperties,
        )
        assertEquals(
            VAlarm(
                Trigger.Relative(-30 * 60_000L, relatedToEnd = true, value = "-PT30M"),
                action = "DISPLAY",
                description = "Default Mozilla Description",
                propertyOrder = listOf("ACTION", "TRIGGER", "DESCRIPTION"),
            ),
            todo.alarms.single(),
        )
        val lines = todo.serialize().unfolded()
        assertLine(lines, "DUE:20260116T100000")
        assertLine(lines, "TRIGGER;RELATED=END:-PT30M")
    }

    @Test
    fun unknownVendorPropertiesKeepTheirParameters() {
        val todo = assertRoundTrips(UNKNOWN_X_PROPERTIES)

        assertEquals(
            listOf(
                ICalProperty("X-VENDOR-NOTE", "plain value", listOf("X-LANG" to "de")),
                ICalProperty("X-VENDOR-ID", "42", listOf("X-SCOPE" to "team", "X-KIND" to "internal")),
            ),
            todo.unknownProperties,
        )
    }

    private fun assertRoundTrips(iCalendar: String): VTodo {
        val parsed = parseVTodos(iCalendar).single()

        val again = parseVTodos(parsed.serialize()).single()

        assertEquals(parsed.copy(dtStamp = null), again.copy(dtStamp = null))
        return parsed
    }

    private fun assertLine(lines: List<String>, line: String) = assertTrue(line in lines, "missing <$line> in $lines")

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) =
        DateTime(year, month, day, hour, minute, second, timeZone = DateTime.UTC).millis

    private fun String.unfolded(): List<String> = replace("\r\n ", "").replace("\n ", "").lines()

    companion object {
        private val NEXTCLOUD_ZONED_SUBTASK = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Nextcloud Tasks v0.16.1
            BEGIN:VTODO
            UID:5e8f1f3a-0b9f-4d0f-9a9c-2f6a3c1d7e01
            CREATED:20260110T081500Z
            LAST-MODIFIED:20260110T082000Z
            DTSTAMP:20260110T082000Z
            SUMMARY:Pack for the trip
            RELATED-TO:1b3c6d7e-2f4a-4c5b-8d9e-0a1b2c3d4e5f
            PERCENT-COMPLETE:40
            STATUS:IN-PROCESS
            X-OC-HIDESUBTASKS:0
            DTSTART;TZID=Europe/Berlin:20260116T080000
            DUE;TZID=Europe/Berlin:20260116T170000
            END:VTODO
            BEGIN:VTIMEZONE
            TZID:Europe/Berlin
            BEGIN:DAYLIGHT
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            TZNAME:CEST
            DTSTART:19700329T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
            END:DAYLIGHT
            BEGIN:STANDARD
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            TZNAME:CET
            DTSTART:19701025T030000
            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU
            END:STANDARD
            END:VTIMEZONE
            END:VCALENDAR
        """.trimIndent()

        private val APPLE_REPEATING_UNTIL = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//iOS 17.2//EN
            CALSCALE:GREGORIAN
            BEGIN:VTODO
            CREATED:20260105T193012Z
            DTSTAMP:20260105T193045Z
            LAST-MODIFIED:20260105T193045Z
            SEQUENCE:1
            UID:7B2C9E44-3F1A-4C8D-9E2B-5A6F7D8C9B0A
            SUMMARY:Take out the bins
            RELATED-TO;RELTYPE=PARENT:0D4E5F6A-7B8C-4D9E-A0B1-C2D3E4F5A6B7
            X-APPLE-SORT-ORDER:726245812
            DTSTART;TZID=America/Los_Angeles:20260112T190000
            DUE;TZID=America/Los_Angeles:20260112T190000
            RRULE:FREQ=WEEKLY;UNTIL=20260330T065959Z;BYDAY=MO
            BEGIN:VALARM
            X-WR-ALARMUID:E1F2A3B4-C5D6-4E7F-8091-A2B3C4D5E6F7
            UID:E1F2A3B4-C5D6-4E7F-8091-A2B3C4D5E6F7
            TRIGGER;VALUE=DATE-TIME:20260113T030000Z
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            END:VALARM
            BEGIN:VALARM
            X-WR-ALARMUID:5C7D9E1F-2A3B-4C5D-8E9F-0A1B2C3D4E5F
            UID:5C7D9E1F-2A3B-4C5D-8E9F-0A1B2C3D4E5F
            TRIGGER;VALUE=DATE-TIME:19760401T005545Z
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            X-APPLE-PROXIMITY:ARRIVE
            END:VALARM
            END:VTODO
            BEGIN:VTIMEZONE
            TZID:America/Los_Angeles
            BEGIN:DAYLIGHT
            TZOFFSETFROM:-0800
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU
            DTSTART:20070311T020000
            TZNAME:PDT
            TZOFFSETTO:-0700
            END:DAYLIGHT
            BEGIN:STANDARD
            TZOFFSETFROM:-0700
            RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU
            DTSTART:20071104T020000
            TZNAME:PST
            TZOFFSETTO:-0800
            END:STANDARD
            END:VTIMEZONE
            END:VCALENDAR
        """.trimIndent()

        private val FASTMAIL_ALL_DAY_UNTIL_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//CyrusIMAP.org/Cyrus 3.9.0-alpha0-1076-g6e0ea7c1c9-fm-20240107.001-g6e0ea7c1//EN
            BEGIN:VTODO
            UID:c4f6a2b8-9d1e-4f3a-8b7c-6d5e4f3a2b1c
            DTSTAMP:20260101T120000Z
            CREATED:20260101T115900Z
            LAST-MODIFIED:20260101T120000Z
            SUMMARY:Pay rent
            DTSTART;VALUE=DATE:20260102
            DUE;VALUE=DATE:20260102
            RRULE:FREQ=DAILY;UNTIL=20260215
            BEGIN:VALARM
            TRIGGER:-P1D
            ACTION:DISPLAY
            DESCRIPTION:Pay rent
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val RADICALE_THUNDERBIRD_FLOATING = """
            BEGIN:VCALENDAR
            PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN
            VERSION:2.0
            BEGIN:VTODO
            CREATED:20260108T101500Z
            LAST-MODIFIED:20260108T101700Z
            DTSTAMP:20260108T101700Z
            UID:a7d1c0e2-4b5f-4e8a-9c3d-1f2e3d4c5b6a
            SUMMARY:Water the plants
            RRULE:FREQ=DAILY;COUNT=10;INTERVAL=2
            DTSTART:20260116T090000
            DUE:20260116T100000
            X-MOZ-GENERATION:2
            X-MOZ-LASTACK:20260108T101700Z
            RELATED-TO;RELTYPE=SIBLING:sibling-uid
            RELATED-TO;RELTYPE=CHILD:child-uid
            BEGIN:VALARM
            ACTION:DISPLAY
            TRIGGER;VALUE=DURATION;RELATED=END:-PT30M
            DESCRIPTION:Default Mozilla Description
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val UNKNOWN_X_PROPERTIES = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Other Client//EN
            BEGIN:VTODO
            UID:x-properties
            SUMMARY:Vendor data
            X-VENDOR-NOTE;X-LANG=de:plain value
            X-VENDOR-ID;X-SCOPE=team;X-KIND=internal:42
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
