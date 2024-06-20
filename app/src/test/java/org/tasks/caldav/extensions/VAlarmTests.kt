package org.tasks.caldav.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.time.DateTime
import org.tasks.time.DateTime.Companion.UTC
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class VAlarmTests {
    @Test
    fun dateTimeToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;VALUE=DATE-TIME:20220121T190000Z\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(
                time = DateTime(2022, 1, 21, 19, 0, 0, 0, UTC).millis,
                type = TYPE_DATE_TIME,
            ).toVAlarm().toString()
        )
    }

    @Test
    fun dateTimeFromAlarm() {
        val alarm = Alarm(
            time = DateTime(2022, 1, 21, 19, 0, 0, 0, UTC).millis,
            type = TYPE_DATE_TIME,
        )
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun beforeStartToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:-PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_START).toVAlarm().toString()
        )
    }

    @Test
    fun beforeStartFromAlarm() {
        val alarm = Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_START)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun afterStartToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_START).toVAlarm().toString()
        )
    }

    @Test
    fun afterStartFromAlarm() {
        val alarm = Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_START)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun beforeEndToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=END:-PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_END).toVAlarm().toString()
        )
    }

    @Test
    fun beforeEndFromAlarm() {
        val alarm = Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_END)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun afterEndToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=END:PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_END).toVAlarm().toString()
        )
    }

    @Test
    fun afterEndFromAlarm() {
        val alarm = Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_END)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun repeatingAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:P1DT8H\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nREPEAT:15\r\nDURATION:PT15M\r\nEND:VALARM\r\n",
            Alarm(
                time = HOURS.toMillis(32),
                type = TYPE_REL_START,
                repeat = 15,
                interval = MINUTES.toMillis(15)
            ).toVAlarm().toString()
        )
    }

    @Test
    fun repeatingFromAlarm() {
        val alarm = Alarm(
            time = HOURS.toMillis(32),
            type = TYPE_REL_START,
            repeat = 15,
            interval = MINUTES.toMillis(15)
        )
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }
}