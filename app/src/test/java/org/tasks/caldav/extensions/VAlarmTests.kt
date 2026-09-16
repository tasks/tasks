package org.tasks.caldav.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.data.createDueDate
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.icalendar.VAlarm
import org.tasks.icalendar.VTodo
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import org.tasks.time.DateTime
import org.tasks.time.DateTime.Companion.UTC
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class VAlarmTests {
    private fun VAlarm.text(): String =
        VTodo(alarms = mutableListOf(this)).serialize().let { "BEGIN:VALARM" + it.substringAfter("BEGIN:VALARM").substringBefore("END:VALARM") + "END:VALARM\r\n" }

    @Test
    fun dateTimeToVAlarm() {
        assertEquals(
            "BEGIN:VALARM\r\nTRIGGER;VALUE=DATE-TIME:20220121T190000Z\r\nACTION:DISPLAY\r\nDESCRIPTION:Default Tasks.org description\r\nEND:VALARM\r\n",
            Alarm(
                time = DateTime(2022, 1, 21, 19, 0, 0, 0, UTC).millis,
                type = TYPE_DATE_TIME,
            ).toVAlarm()!!.text()
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
            Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_START).toVAlarm()!!.text()
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
            Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_START).toVAlarm()!!.text()
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
            Alarm(time = -MINUTES.toMillis(75), type = TYPE_REL_END).toVAlarm()!!.text()
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
            Alarm(time = MINUTES.toMillis(75), type = TYPE_REL_END).toVAlarm()!!.text()
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
            ).toVAlarm()!!.text()
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

    @Test
    fun serializeAlarms() {
        val remoteTask = VTodo()
        remoteTask.applyLocal(
            CaldavTask(
                calendar = "",
            ),
            Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2025, 9, 4, 18, 0, 0).millis
                ),
            ),
        )
        Alarm(time = 0, type = TYPE_REL_END).toVAlarm()?.let { remoteTask.alarms.add(it) }

        val tasks = parseVTodos(remoteTask.serialize())
        assertEquals(1, tasks.size)

        val task = tasks.first()
        assertEquals(1, task.alarms.size)

        val alarm = task.alarms.first().toAlarm()
        assertEquals(TYPE_REL_END, alarm?.type)
        assertEquals(0L, alarm?.time)
    }
}
