package org.tasks.icalendar

import kotlinx.datetime.TimeZone
import org.tasks.kmp.PROD_ID
import org.tasks.repeats.Frequency
import org.tasks.repeats.Recur
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class VTodoTest {
    private val chicago = TimeZone.of("America/Chicago")

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) =
        DateTime(year, month, day, hour, minute, second, timeZone = DateTime.UTC).millis

    @Test
    fun parsesAppleTask() {
        val todo = parseVTodos(APPLE_DUE_DATE).single()

        assertEquals("CB8609CD-8345-46CF-8295-4FDA84050CDB", todo.uid)
        assertEquals("Test title", todo.summary)
        assertEquals(0, todo.sequence)
        assertEquals(utc(2018, 4, 16, 22, 26, 50), todo.createdAt)
        assertEquals(utc(2018, 4, 16, 22, 26, 56), todo.dtStamp)
        val six = DateTime(2018, 4, 16, 18, 0, timeZone = chicago).millis
        assertEquals(ICalDate.DateTime(six, "America/Chicago"), todo.due)
        assertEquals(ICalDate.DateTime(six, "America/Chicago"), todo.dtStart)
        assertNull(todo.completedAt)
        assertEquals(
            listOf(
                VAlarm(
                    trigger = Trigger.Absolute(utc(2018, 4, 16, 23, 0, 0)),
                    action = "DISPLAY",
                    description = "Event reminder",
                    otherProperties = listOf(
                        ICalProperty("X-WR-ALARMUID", "31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5"),
                        ICalProperty("UID", "31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5"),
                    ),
                ),
            ),
            todo.alarms,
        )
    }

    @Test
    fun parsesAllDayTaskAndReadsFloatingTimestampsAsUtc() {
        val todo = parseVTodos(NEXTCLOUD_ALL_DAY).single()

        assertEquals(ICalDate.Date(2021, 2, 1), todo.due)
        assertNull(todo.dtStart)
        assertEquals(utc(2021, 1, 27, 19, 51, 22), todo.createdAt)
        assertEquals(utc(2021, 1, 27, 19, 51, 28), todo.lastModified)
        assertEquals(utc(2021, 1, 27, 19, 51, 28), todo.dtStamp)
    }

    @Test
    fun parsesEveryTodoInTheCalendar() {
        val todos = parseVTodos(EMCLIENT_COMPLETED_RECURRING)

        assertEquals(2, todos.size)
        val (series, occurrence) = todos
        assertEquals(Recur(Frequency.DAILY), series.rRule)
        assertEquals(0, series.percentComplete)
        assertEquals(listOf(ICalProperty("X-APPLE-SORT-ORDER", "621354239")), series.unknownProperties)
        assertEquals(ICalDate.DateTime(DateTime(2020, 9, 10).millis), series.due)
        assertEquals(utc(2020, 9, 9, 14, 25, 4), occurrence.completedAt)
        assertEquals(TodoStatus.COMPLETED, occurrence.status)
        assertEquals(100, occurrence.percentComplete)
        assertEquals(listOf(ICalProperty("RECURRENCE-ID", "20200910T000000")), occurrence.unknownProperties)
    }

    @Test
    fun parsesCategoriesAndRelations() {
        val todo = parseVTodos(SUBTASK_WITH_TAGS).single()

        assertEquals(listOf("Home", "Garden, patio"), todo.categories)
        assertEquals(listOf(RelatedTo("parent-uid", "PARENT")), todo.relatedTo)
        assertEquals(Geo(37.386013, -122.082932), todo.geoPosition)
        assertEquals(TodoStatus.NEEDS_ACTION, todo.status)
        assertEquals(5, todo.priority)
        assertEquals("Line one\nLine two", todo.description)
        assertEquals(ICalProperty("ORGANIZER", "mailto:alex@example.com", listOf("CN" to "Alex")), todo.organizer)
    }

    @Test
    fun parsesRelativeAlarms() {
        val alarms = parseVTodos(RELATIVE_ALARMS).single().alarms

        assertEquals(
            listOf(
                VAlarm(Trigger.Relative(-15 * 60_000L), action = "DISPLAY", description = "Reminder"),
                VAlarm(Trigger.Relative(2 * 86_400_000L, relatedToEnd = true), action = "AUDIO", repeat = 2, duration = 5 * 60_000L),
            ),
            alarms,
        )
    }

    @Test
    fun serializesATask() {
        val nine = DateTime(2026, 1, 16, 9, 0, timeZone = chicago).millis
        val lines = task(nine).serialize().unfolded()

        assertContains(lines, "PRODID:$PROD_ID")
        assertContains(lines, "UID:task-uid")
        assertContains(lines, "SUMMARY:Water the plants")
        assertContains(lines, "DESCRIPTION:Line one\\nLine two")
        assertContains(lines, "DUE;TZID=America/Chicago:20260116T090000")
        assertContains(lines, "DTSTART;TZID=America/Chicago:20260116T090000")
        assertContains(lines, "CREATED:20260115T120000Z")
        assertContains(lines, "LAST-MODIFIED:20260115T120100Z")
        assertContains(lines, "COMPLETED:20260116T140000Z")
        assertContains(lines, "STATUS:COMPLETED")
        assertContains(lines, "PERCENT-COMPLETE:100")
        assertContains(lines, "PRIORITY:1")
        assertContains(lines, "RRULE:FREQ=DAILY")
        assertEquals("Home,Garden\\, patio", lines.filter { it.startsWith("CATEGORIES:") }.joinToString(",") { it.substringAfter(':') })
        assertContains(lines, "RELATED-TO;RELTYPE=PARENT:parent-uid")
        assertContains(lines, "GEO:37.386013;-122.082932")
        assertContains(lines, "X-APPLE-SORT-ORDER:5")
        assertContains(lines, "TRIGGER;RELATED=END:-PT15M")
        assertContains(lines, "ACTION:DISPLAY")
        assertContains(lines, "TZID:America/Chicago")
        assertFalse(lines.any { it.startsWith("SEQUENCE") })
    }

    @Test
    fun roundTrips() {
        val nine = DateTime(2026, 1, 16, 9, 0, timeZone = chicago).millis
        val original = task(nine)

        val parsed = parseVTodos(original.serialize()).single()

        assertEquals(original, parsed.copy(dtStamp = null, sequence = null))
    }

    @Test
    fun repairsInconsistentStartAndDue() {
        val dateStartTimedDue = parseVTodos(vtodo("DTSTART;VALUE=DATE:20260116", "DUE:20260116T090000Z")).single()
        assertEquals(ICalDate.DateTime(DateTime(2026, 1, 16).millis), dateStartTimedDue.dtStart)

        val startAfterDue = parseVTodos(vtodo("DTSTART:20260117T090000Z", "DUE:20260116T090000Z")).single()
        assertNull(startAfterDue.dtStart)
        assertEquals(ICalDate.DateTime(utc(2026, 1, 16, 9, 0, 0)), startAfterDue.due)

        val durationWithoutStart = parseVTodos(vtodo("DURATION:PT1H")).single()
        assertNull(durationWithoutStart.duration)
    }

    private fun vtodo(vararg lines: String) =
        "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Tasks.org//EN\nBEGIN:VTODO\nUID:repair\n${lines.joinToString("\n")}\nEND:VTODO\nEND:VCALENDAR"

    @Test
    fun formatsDurationsLikeIcal4j() {
        assertEquals("-PT15M", formatICalDuration(-900_000))
        assertEquals("P2D", formatICalDuration(2 * 86_400_000L))
        assertEquals("-P50D", formatICalDuration(-50 * 86_400_000L))
        assertEquals("PT1H1M", formatICalDuration(3_660_000))
        assertEquals("PT1M30S", formatICalDuration(90_000))
        assertEquals("P1DT1H", formatICalDuration(90_000_000))
        assertEquals("PT0S", formatICalDuration(0))
    }

    @Test
    fun repairsInvalidUtcOffsetsAndDayOffsets() {
        assertEquals("TZOFFSETFROM:+001900\n", "TZOFFSETFROM:+1900\n".repairICalendar())
        assertEquals("TZOFFSETTO:-0600\n", "TZOFFSETTO:-0600\n".repairICalendar())
        assertEquals("TRIGGER:-P1D\nDURATION:P2D\n", "TRIGGER:-PT1D\nDURATION:P2DT\n".repairICalendar())
    }

    private fun task(nine: Long) = VTodo(
        uid = "task-uid",
        createdAt = utc(2026, 1, 15, 12, 0, 0),
        lastModified = utc(2026, 1, 15, 12, 1, 0),
        summary = "Water the plants",
        description = "Line one\nLine two",
        geoPosition = Geo(37.386013, -122.082932),
        priority = 1,
        status = TodoStatus.COMPLETED,
        dtStart = ICalDate.DateTime(nine, "America/Chicago"),
        due = ICalDate.DateTime(nine, "America/Chicago"),
        completedAt = utc(2026, 1, 16, 14, 0, 0),
        percentComplete = 100,
        rRule = Recur(Frequency.DAILY),
        categories = mutableListOf("Home", "Garden, patio"),
        relatedTo = mutableListOf(RelatedTo("parent-uid", "PARENT")),
        unknownProperties = mutableListOf(ICalProperty("X-APPLE-SORT-ORDER", "5")),
        alarms = mutableListOf(
            VAlarm(Trigger.Relative(-15 * 60_000L, relatedToEnd = true), action = "DISPLAY", description = "Reminder"),
        ),
    )

    private fun String.unfolded(): List<String> = replace("\r\n ", "").replace("\n ", "").lines()

    companion object {
        private val APPLE_DUE_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//Mac OS X 10.13.4//EN
            CALSCALE:GREGORIAN
            BEGIN:VTODO
            CREATED:20180416T222650Z
            UID:CB8609CD-8345-46CF-8295-4FDA84050CDB
            SUMMARY:Test title
            DTSTART;TZID=America/Chicago:20180416T180000
            DTSTAMP:20180416T222656Z
            SEQUENCE:0
            DUE;TZID=America/Chicago:20180416T180000
            BEGIN:VALARM
            X-WR-ALARMUID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5
            UID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5
            TRIGGER;VALUE=DATE-TIME:20180416T230000Z
            DESCRIPTION:Event reminder
            ACTION:DISPLAY
            END:VALARM
            END:VTODO
            BEGIN:VTIMEZONE
            TZID:America/Chicago
            BEGIN:DAYLIGHT
            DTSTART:20070311T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU
            TZNAME:CDT
            TZOFFSETFROM:-0600
            TZOFFSETTO:-0500
            END:DAYLIGHT
            BEGIN:STANDARD
            DTSTART:20071104T020000
            RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU
            TZNAME:CST
            TZOFFSETFROM:-0500
            TZOFFSETTO:-0600
            END:STANDARD
            END:VTIMEZONE
            END:VCALENDAR
        """.trimIndent()

        private val NEXTCLOUD_ALL_DAY = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Nextcloud Tasks v0.13.6
            BEGIN:VTODO
            UID:838bdba9-f511-4dc7-8686-aaad8728e9bd
            CREATED:20210127T195122
            LAST-MODIFIED:20210127T195128
            DTSTAMP:20210127T195128
            SUMMARY:All day task
            DUE;VALUE=DATE:20210201
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val EMCLIENT_COMPLETED_RECURRING = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//MailClient.VObject/8.0.3385.0
            BEGIN:VTODO
            DTSTAMP:20200909T142504Z
            UID:3884083942925614120
            CREATED:20200909T142419Z
            LAST-MODIFIED:20200909T142504Z
            SUMMARY:Repeat
            RRULE:FREQ=DAILY
            X-APPLE-SORT-ORDER:621354239
            DUE:20200910T000000
            DTSTART:20200910T000000
            PERCENT-COMPLETE:0
            END:VTODO
            BEGIN:VTODO
            RECURRENCE-ID:20200910T000000
            UID:3884083942925614120
            DTSTART:20200910T000000
            DUE:20200910T000000
            COMPLETED:20200909T142504Z
            PERCENT-COMPLETE:100
            LAST-MODIFIED:20200909T142504Z
            DTSTAMP:20200909T142504Z
            CREATED:20200909T142504Z
            SUMMARY:Repeat
            STATUS:COMPLETED
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val SUBTASK_WITH_TAGS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Tasks.org//EN
            BEGIN:VTODO
            UID:child-uid
            SUMMARY:Child
            DESCRIPTION:Line one\nLine two
            CATEGORIES:Home,Garden\, patio
            RELATED-TO;RELTYPE=PARENT:parent-uid
            GEO:37.386013;-122.082932
            STATUS:NEEDS-ACTION
            PRIORITY:5
            ORGANIZER;CN=Alex:mailto:alex@example.com
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val RELATIVE_ALARMS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Tasks.org//EN
            BEGIN:VTODO
            UID:alarms-uid
            SUMMARY:Alarms
            DUE:20260116T090000Z
            BEGIN:VALARM
            TRIGGER:-PT15M
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            END:VALARM
            BEGIN:VALARM
            TRIGGER;RELATED=END:P2D
            ACTION:AUDIO
            REPEAT:2
            DURATION:PT5M
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
