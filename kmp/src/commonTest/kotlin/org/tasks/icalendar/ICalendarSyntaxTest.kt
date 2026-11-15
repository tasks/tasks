package org.tasks.icalendar

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ICalendarSyntaxTest {
    private fun ical(vararg lines: String, separator: String = "\r\n") =
        (listOf("BEGIN:VCALENDAR", "BEGIN:VTODO") + lines + listOf("END:VTODO", "END:VCALENDAR"))
            .joinToString(separator, postfix = separator)

    @Test
    fun wellFormedWithCrlf() =
        assertTrue(ical("SUMMARY:a", "DESCRIPTION:long", " folded\u2028text").isWellFormedICalendar())

    @Test
    fun wellFormedWithLf() =
        assertTrue(ical("SUMMARY:a", "DESCRIPTION:long", "\tfolded", separator = "\n").isWellFormedICalendar())

    @Test
    fun parametersAndXPropertiesAreFine() =
        assertTrue(
            ical(
                "ATTENDEE;CN=\"Doe: Jane\";ROLE=REQ-PARTICIPANT:mailto:jane@example.com",
                "X-APPLE-SORT-ORDER:1",
                "SOME-FUTURE-PROPERTY;X=1:value",
            ).isWellFormedICalendar()
        )

    @Test
    fun lineWithoutColonIsDamaged() =
        assertFalse(ical("DESCRIPTION:Hardware store ", "Watering can ", "PRIORITY:9").isWellFormedICalendar())

    @Test
    fun illegalPropertyNameIsDamaged() =
        assertFalse(ical("DESCRIPTION:a", "Soil bags:12", "PRIORITY:9").isWellFormedICalendar())

    @Test
    fun emptyLineInsideIsDamaged() =
        assertFalse(ical("DESCRIPTION:a", "", "PRIORITY:9").isWellFormedICalendar())

    @Test
    fun blankIsNotWellFormed() = assertFalse("".isWellFormedICalendar())
}
