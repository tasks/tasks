package org.tasks.icalendar

import org.tasks.caldav.Task
import java.io.ByteArrayOutputStream
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class VTodoPreservationTest {
    @Test
    fun theVTodoLayerLosesNothingTheIcalModelHolds() {
        listOf(RICH, MOST_FIELDS_1, MOST_FIELDS_2, RFC5545_SAMPLE_1).forEach { ical ->
            val direct = Task.tasksFromReader(StringReader(ical)).single()

            val viaVTodo = direct.toVTodo().toTask()

            assertEquals(direct.render().toSet(), viaVTodo.render().toSet())
        }
    }

    @Test
    fun aCachedVtodoKeepsWhatTheAppDoesNotModel() {
        val lines = parseVTodos(RICH).single().serialize().unfolded()

        listOf(
            "ORGANIZER;CN=Someone:mailto:someone@example.com",
            "DTSTART;TZID=America/Chicago:20260201T090000",
            "DUE;TZID=America/Chicago:20260301T090000",
            "RDATE;TZID=America/Chicago:20260401T090000",
            "EXDATE;TZID=America/Chicago:20260501T090000",
            "RRULE:RSCALE=HEBREW;FREQ=MONTHLY;INTERVAL=2;SKIP=FORWARD",
            "RELATED-TO;RELTYPE=PARENT;X-CUSTOM=keepme:parent-uid",
            "ATTENDEE;PARTSTAT=ACCEPTED;CN=Someone Else:mailto:else@example.com",
            "X-APPLE-SORT-ORDER:12",
            "ATTACH:https://example.com/file.pdf",
            "TRIGGER;RELATED=START:-PT15M",
            "X-WR-ALARMUID:custom-alarm-id",
            "ATTENDEE:mailto:else@example.com",
        ).forEach { assertEquals(1, lines.count { line -> line == it }, "missing $it") }
    }

    private fun Task.render(): List<String> =
        ByteArrayOutputStream().also(::write).toString("UTF-8")
            .unfolded()
            .filterNot { it.startsWith("DTSTAMP") || it.startsWith("PRODID") }

    private fun String.unfolded(): List<String> =
        replace("\r\n ", "").replace("\n ", "").lines()

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
        private val RICH = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Example Corp//Server 1.0//EN
            BEGIN:VTODO
            UID:rich-task
            DTSTAMP:20260101T000000Z
            CREATED:20260101T000000Z
            LAST-MODIFIED:20260102T000000Z
            SEQUENCE:4
            SUMMARY:Rich task
            DESCRIPTION:Multi line\ndescription
            LOCATION:Somewhere
            GEO:37.386013;-122.082932
            PRIORITY:3
            STATUS:IN-PROCESS
            PERCENT-COMPLETE:40
            CLASS:CONFIDENTIAL
            URL:https://example.com/x
            ORGANIZER;CN=Someone:mailto:someone@example.com
            CATEGORIES:home,errands
            COMMENT:a comment
            DUE;TZID=America/Chicago:20260301T090000
            DTSTART;TZID=America/Chicago:20260201T090000
            RRULE:RSCALE=HEBREW;FREQ=MONTHLY;INTERVAL=2;SKIP=FORWARD
            RDATE;TZID=America/Chicago:20260401T090000
            EXDATE;TZID=America/Chicago:20260501T090000
            RELATED-TO;RELTYPE=PARENT;X-CUSTOM=keepme:parent-uid
            ATTENDEE;PARTSTAT=ACCEPTED;CN=Someone Else:mailto:else@example.com
            X-APPLE-SORT-ORDER:12
            X-MOZ-LASTACK:20260116T100000Z
            ATTACH:https://example.com/file.pdf
            BEGIN:VALARM
            UID:alarm-1
            TRIGGER;RELATED=START:-PT15M
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            X-WR-ALARMUID:custom-alarm-id
            ATTENDEE:mailto:else@example.com
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
