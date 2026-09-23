package org.tasks.caldav

import org.tasks.caldav.extensions.preserving
import org.tasks.caldav.extensions.toVAlarms
import org.tasks.caldav.iCalendar.Companion.filtered
import org.tasks.data.entity.Alarm
import org.tasks.icalendar.ICalProperty
import org.tasks.icalendar.Trigger
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class VAlarmPreservationTest {
    private val absolute = DateTime(2018, 4, 16, 23, 0, 0, timeZone = DateTime.UTC).millis
    private val relative = -15 * 60_000L

    @Test
    fun keepsTriggerParametersOnAnUnchangedAbsoluteAlarm() {
        val alarms = preserve(Alarm(time = absolute, type = Alarm.TYPE_DATE_TIME))

        assertEquals(
            listOf("X-VENDOR-ID" to "xyz"),
            (alarms.single().trigger as Trigger.Absolute).parameters,
        )
    }

    @Test
    fun keepsPropertiesOnAnUnchangedAbsoluteAlarm() {
        val alarms = preserve(Alarm(time = absolute, type = Alarm.TYPE_DATE_TIME))

        assertEquals(
            listOf(
                ICalProperty("X-WR-ALARMUID", "31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5"),
                ICalProperty("UID", "31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5"),
            ),
            alarms.single().otherProperties,
        )
    }

    @Test
    fun keepsTriggerParametersOnAnUnchangedRelativeAlarm() {
        val alarms = preserve(Alarm(time = relative, type = Alarm.TYPE_REL_START))

        assertEquals(
            listOf("X-VENDOR-ID" to "abc"),
            (alarms.single().trigger as Trigger.Relative).parameters,
        )
    }

    @Test
    fun forgetsTheRemoteAlarmWhenTheReminderMoved() {
        val alarms = preserve(Alarm(time = absolute + 60_000, type = Alarm.TYPE_DATE_TIME))

        assertEquals(emptyList(), (alarms.single().trigger as Trigger.Absolute).parameters)
        assertEquals(emptyList(), alarms.single().otherProperties)
    }

    @Test
    fun forgetsTheRemotePropertiesWhenTheActionChanges() {
        val todo = parseVTodos(AUDIO_ALARM).single()
        val replaced = todo.alarms.filtered
        todo.alarms.removeAll(replaced)
        todo.alarms.addAll(
            listOf(Alarm(time = relative, type = Alarm.TYPE_REL_START))
                .toVAlarms()
                .preserving(replaced)
        )

        val serialized = todo.serialize().unfolded()

        assertContains(serialized, "ACTION:DISPLAY")
        assertEquals(emptyList(), serialized.filter { it.startsWith("ATTACH") })
        assertEquals(emptyList(), serialized.filter { it == "UID:audio-alarm" })
        assertContains(serialized.single { it.startsWith("TRIGGER;RELATED=START") }, "X-VENDOR-ID=abc")
    }

    @Test
    fun givesTheRemoteAlarmToOnlyOneReminder() {
        val alarms = listOf(
            Alarm(time = relative, type = Alarm.TYPE_REL_START),
            Alarm(time = relative, type = Alarm.TYPE_REL_START, repeat = 3, interval = 60_000),
        )
            .toVAlarms()
            .preserving(parseVTodos(ALARMS).single().alarms.filtered)

        assertEquals(2, alarms.size)
        assertEquals(
            1,
            alarms.count { (it.trigger as Trigger.Relative).parameters.isNotEmpty() },
        )
    }

    @Test
    fun writesThePreservedAlarmsBackOut() {
        val todo = parseVTodos(ALARMS).single()
        val replaced = todo.alarms.filtered
        todo.alarms.removeAll(replaced)
        todo.alarms.addAll(
            listOf(
                Alarm(time = absolute, type = Alarm.TYPE_DATE_TIME),
                Alarm(time = relative, type = Alarm.TYPE_REL_START),
            )
                .toVAlarms()
                .preserving(replaced)
        )

        val serialized = todo.serialize().unfolded()

        listOf(
            "X-WR-ALARMUID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5",
            "UID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5",
        ).forEach { assertContains(serialized, it) }
        assertContains(serialized.single { it.startsWith("TRIGGER;VALUE=DATE-TIME") }, "X-VENDOR-ID=xyz")
        assertContains(serialized.single { it.startsWith("TRIGGER;RELATED=START") }, "X-VENDOR-ID=abc")
    }

    private fun preserve(local: Alarm) = listOf(local)
        .toVAlarms()
        .preserving(parseVTodos(ALARMS).single().alarms.filtered)

    private fun String.unfolded(): List<String> =
        replace("\r\n ", "").replace("\n ", "").lines()

    companion object {
        private val AUDIO_ALARM = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//Mac OS X 10.13.4//EN
            BEGIN:VTODO
            UID:audio-task
            DTSTAMP:20180416T222656Z
            SUMMARY:Test title
            DUE:20180417T100000Z
            BEGIN:VALARM
            UID:audio-alarm
            TRIGGER;RELATED=START;X-VENDOR-ID=abc:-PT15M
            ACTION:AUDIO
            ATTACH;FMTTYPE=audio/basic:ftp://host/pub/beep.wav
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val ALARMS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Apple Inc.//Mac OS X 10.13.4//EN
            BEGIN:VTODO
            UID:preserve-alarms
            DTSTAMP:20180416T222656Z
            SUMMARY:Test title
            DUE:20180417T100000Z
            BEGIN:VALARM
            X-WR-ALARMUID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5
            UID:31AC3FC5-D6D1-47D1-9B8D-597BD5D097A5
            TRIGGER;VALUE=DATE-TIME;X-VENDOR-ID=xyz:20180416T230000Z
            DESCRIPTION:Event reminder
            ACTION:DISPLAY
            END:VALARM
            BEGIN:VALARM
            TRIGGER;RELATED=START;X-VENDOR-ID=abc:-PT15M
            DESCRIPTION:Event reminder
            ACTION:DISPLAY
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}
