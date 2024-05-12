package org.tasks.caldav.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.time.DateTime
import org.tasks.time.DateTime.UTC
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class VAlarmTests {
    @Test
    fun dateTimeToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;VALUE=DATE-TIME:20220121T190000Z\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(
                0,
                DateTime(2022, 1, 21, 19, 0, 0, 0, UTC).millis,
                TYPE_DATE_TIME,
                0,
                0
            ).toVAlarm().toString()
        )
    }

    @Test
    fun dateTimeFromAlarm() {
        val alarm = Alarm(
            0,
            DateTime(2022, 1, 21, 19, 0, 0, 0, UTC).millis,
            TYPE_DATE_TIME,
            0,
            0
        )
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun beforeStartToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:-PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(0, -MINUTES.toMillis(75), TYPE_REL_START, 0, 0).toVAlarm().toString()
        )
    }

    @Test
    fun beforeStartFromAlarm() {
        val alarm = Alarm(0, -MINUTES.toMillis(75), TYPE_REL_START, 0, 0)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun afterStartToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(0, MINUTES.toMillis(75), TYPE_REL_START, 0, 0).toVAlarm().toString()
        )
    }

    @Test
    fun afterStartFromAlarm() {
        val alarm = Alarm(0, MINUTES.toMillis(75), TYPE_REL_START, 0, 0)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun beforeEndToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=END:-PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(0, -MINUTES.toMillis(75), TYPE_REL_END, 0, 0).toVAlarm().toString()
        )
    }

    @Test
    fun beforeEndFromAlarm() {
        val alarm = Alarm(0, -MINUTES.toMillis(75), TYPE_REL_END, 0, 0)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun afterEndToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=END:PT1H15M\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(0, MINUTES.toMillis(75), TYPE_REL_END, 0, 0).toVAlarm().toString()
        )
    }

    @Test
    fun afterEndFromAlarm() {
        val alarm = Alarm(0, MINUTES.toMillis(75), TYPE_REL_END, 0, 0)
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }

    @Test
    fun repeatingAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;RELATED=START:P1DT8H\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nREPEAT:15\r\nDURATION:PT15M\r\nEND:VALARM\r\n",
            Alarm(0, HOURS.toMillis(32), TYPE_REL_START, 15, MINUTES.toMillis(15)).toVAlarm().toString()
        )
    }

    @Test
    fun repeatingFromAlarm() {
        val alarm = Alarm(0, HOURS.toMillis(32), TYPE_REL_START, 15, MINUTES.toMillis(15))
        assertEquals(alarm, alarm.toVAlarm()?.toAlarm())
    }
}