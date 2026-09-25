package org.tasks.icalendar

import org.junit.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VTodoFieldFidelityTest {
    @Test
    fun keepsEveryFieldThroughAParseWriteParseCycle() {
        val t = regenerate(MOST_FIELDS_1)

        assertEquals(1, t.sequence)
        assertEquals("most-fields1@example.com", t.uid)
        assertEquals("Conference Room - F123, Bldg. 002", t.location)
        assertEquals(37.386013, t.geoPosition!!.latitude, 0.0000001)
        assertEquals(-122.082932, t.geoPosition!!.longitude, 0.0000001)
        assertEquals(
            "Meeting to provide technical review for \"Phoenix\" design.\n" +
                    "Happy Face Conference Room. Phoenix design team MUST attend this meeting.\n" +
                    "RSVP to team leader.",
            t.description,
        )
        assertEquals("http://example.com/principals/jsmith", t.organizer!!.value)
        assertEquals("http://example.com/pub/calendars/jsmith/mytime.ics", t.url)
        assertEquals(1, t.priority)
        assertEquals("CONFIDENTIAL", t.classification)
        assertEquals(TodoStatus.IN_PROCESS, t.status)
        assertEquals(25, t.percentComplete)
        assertEquals(ICalDate.Date(2010, 1, 1), t.dtStart)
        assertEquals(ICalDate.Date(2010, 10, 1), t.due)
        assertEquals("FREQ=YEARLY;INTERVAL=2", t.rRule?.toString())
        assertEquals(listOf("Test", "Sample"), t.categories.toList())
        assertEquals(828106200000L, t.createdAt)
        assertEquals(840288600000L, t.lastModified)
    }

    @Test
    fun keepsEveryRecurrenceDateProperty() {
        val t = regenerate(MOST_FIELDS_1)

        assertEquals(listOf("20120101", "20140101,20180101"), t.exDates.map { it.value })
        assertEquals(listOf("20100310,20100315", "20100810"), t.rDates.map { it.value })
    }

    @Test
    fun keepsRelationshipsAndUnknownProperties() {
        val t = regenerate(MOST_FIELDS_1)

        val sibling = t.relatedTo.single()
        assertEquals("most-fields2@example.com", sibling.uid)
        assertEquals("SIBLING", sibling.relType)

        val unknown = t.unknownProperties.single()
        assertEquals("X-UNKNOWN-PROP", unknown.name)
        assertEquals("Unknown Value", unknown.value)
        assertEquals(listOf("PARAM1" to "xxx"), unknown.parameters)
    }

    @Test
    fun keepsADurationAnchoredToAUtcStart() {
        val t = regenerate(MOST_FIELDS_2)

        assertEquals("most-fields2@example.com", t.uid)
        assertEquals(ICalDate.DateTime(1262340610000L), t.dtStart)
        assertEquals("P4DT3H2M1S", t.duration)
        assertEquals(emptyList(), t.unknownProperties.toList())
    }

    @Test
    fun keepsAFloatingDueAndAnAlarmWithExtraProperties() {
        val t = regenerate(RFC5545_SAMPLE_1)

        assertEquals(2, t.sequence)
        assertEquals("uid4@example.com", t.uid)
        assertEquals("mailto:unclesam@example.com", t.organizer!!.value)
        assertEquals("Submit Income Taxes", t.summary)
        assertEquals(TodoStatus.NEEDS_ACTION, t.status)
        assertEquals(ICalDate.DateTime(892598400000L, floating = true), t.due)

        val alarm = t.alarms.single()
        assertEquals("AUDIO", alarm.action)
        assertEquals(4, alarm.repeat)
        assertEquals(3_600_000L, alarm.duration)
    }

    @Test
    @Ignore("ical4j leaks the coerced TZID into later parses in the same process")
    fun rewritesADateStartWhenTheDueIsATime() {
        val t = parseVTodos(vtodo(
            "DTSTART;VALUE=DATE:20200731",
            "DUE;TZID=Europe/Vienna:20200731T234600",
        )).single()

        assertEquals(ICalDate.DateTime(1596146400000L, tzId = "Europe/Vienna"), t.dtStart)
        assertEquals(ICalDate.DateTime(1596231960000L, tzId = "Europe/Vienna"), t.due)
    }

    @Test
    @Ignore("ical4j leaks the coerced TZID into later parses in the same process")
    fun rewritesADateDueWhenTheStartIsATime() {
        val t = parseVTodos(vtodo(
            "DTSTART;TZID=Europe/Vienna:20200731T235510",
            "DUE;VALUE=DATE:20200801",
        )).single()

        assertEquals(ICalDate.DateTime(1596232510000L, tzId = "Europe/Vienna"), t.dtStart)
        assertEquals(ICalDate.DateTime(1596232800000L, tzId = "Europe/Vienna"), t.due)
    }

    @Test
    fun dropsAStartThatIsAfterTheDue() {
        val t = parseVTodos(vtodo(
            "DTSTART;TZID=Europe/Vienna:20200731T234600",
            "DUE;TZID=Europe/Vienna:20200731T123000",
        )).single()

        assertNull(t.dtStart)
        assertEquals(ICalDate.DateTime(1596191400000L, tzId = "Europe/Vienna"), t.due)
    }

    @Test
    fun dropsADurationWithoutAStart() {
        val t = parseVTodos(vtodo("DURATION:PT1H")).single()

        assertNull(t.dtStart)
        assertNull(t.duration)
    }

    @Test
    fun readsAnEmptyPriorityAsUnset() {
        val t = parseVTodos(vtodo("PRIORITY:")).single()

        assertEquals(0, t.priority)
    }

    private fun regenerate(ical: String): VTodo =
        parseVTodos(parseVTodos(ical).single().serialize()).single()

    private fun vtodo(vararg lines: String) =
        "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//E//EN\r\nBEGIN:VTODO\r\nUID:x\r\nSUMMARY:s\r\n" +
                lines.joinToString("") { "$it\r\n" } +
                "END:VTODO\r\nEND:VCALENDAR\r\n"

    companion object {
        private val MOST_FIELDS_1 =
            "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ABC Corporation//NONSGML My Product//EN\r\n" +
                    "BEGIN:VTODO\r\nSEQUENCE:1\r\nUID:most-fields1@example.com\r\n" +
                    "LOCATION;ALTREP=\"http://xyzcorp.com/conf-rooms/f123.vcf\":\r\n" +
                    " Conference Room - F123\\, Bldg. 002\r\n" +
                    "GEO:37.386013;-122.082932\r\n" +
                    "DESCRIPTION:Meeting to provide technical review for \"Phoenix\"\r\n" +
                    "  design.\\nHappy Face Conference Room. Phoenix design team\r\n" +
                    "  MUST attend this meeting.\\nRSVP to team leader.\r\n" +
                    "URL:http://example.com/pub/calendars/jsmith/mytime.ics\r\n" +
                    "ORGANIZER:http://example.com/principals/jsmith\r\n" +
                    "PRIORITY:1\r\nCLASS:CONFIDENTIAL\r\nSTATUS:IN-PROCESS\r\nPERCENT-COMPLETE:25\r\n" +
                    "DTSTART;VALUE=DATE:20100101\r\nDUE;VALUE=DATE:20101001\r\n" +
                    "CATEGORIES:Test,Sample\r\nRRULE:FREQ=YEARLY;INTERVAL=2\r\n" +
                    "EXDATE;VALUE=DATE:20120101\r\nEXDATE;VALUE=DATE:20140101,20180101\r\n" +
                    "RDATE;VALUE=DATE:20100310,20100315\r\nRDATE;VALUE=DATE:20100810\r\n" +
                    "RELATED-TO;RELTYPE=SIBLING:most-fields2@example.com\r\n" +
                    "X-UNKNOWN-PROP;param1=xxx:Unknown Value\r\n" +
                    "CREATED:19960329T133000Z\r\nLAST-MODIFIED:19960817T133000Z\r\n" +
                    "END:VTODO\r\nEND:VCALENDAR\r\n"

        private val MOST_FIELDS_2 =
            "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ABC Corporation//NONSGML My Product//EN\r\n" +
                    "BEGIN:VTODO\r\nUID:most-fields2@example.com\r\n" +
                    "DTSTART:20100101T101010Z\r\nDURATION:P4DT3H2M1S\r\n" +
                    "END:VTODO\r\nEND:VCALENDAR\r\n"

        private val RFC5545_SAMPLE_1 =
            "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//ABC Corporation//NONSGML My Product//EN\r\n" +
                    "BEGIN:VTODO\r\nDTSTAMP:19980130T134500Z\r\nSEQUENCE:2\r\nUID:uid4@example.com\r\n" +
                    "ORGANIZER:mailto:unclesam@example.com\r\n" +
                    "ATTENDEE;PARTSTAT=ACCEPTED:mailto:jqpublic@example.com\r\n" +
                    "DUE:19980415T000000\r\nSTATUS:NEEDS-ACTION\r\nSUMMARY:Submit Income Taxes\r\n" +
                    "BEGIN:VALARM\r\nACTION:AUDIO\r\nTRIGGER;VALUE=DATE-TIME:19980403T120000Z\r\n" +
                    "ATTACH;FMTTYPE=audio/basic:http://example.com/pub/audio-\r\n files/ssbanner.aud\r\n" +
                    "REPEAT:4\r\nDURATION:PT1H\r\nEND:VALARM\r\n" +
                    "END:VTODO\r\nEND:VCALENDAR\r\n"
    }
}
