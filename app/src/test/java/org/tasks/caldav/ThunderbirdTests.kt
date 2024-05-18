package org.tasks.caldav

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.tasks.TestUtilities.alarms
import org.tasks.TestUtilities.setup
import org.tasks.TestUtilities.vtodo
import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Task
import org.tasks.time.DateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

class ThunderbirdTests {
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun before() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
    }

    @After
    fun after() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun readTitle() {
        assertEquals("Test title", vtodo("thunderbird/basic_no_due_date.txt").title)
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("thunderbird/basic_no_due_date.txt").notes)
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 17, 11, 31, 52).millis,
                vtodo("thunderbird/basic_no_due_date.txt").creationDate)
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 17, 14, 0, 1).millis,
                vtodo("thunderbird/basic_due_date.txt").dueDate)
    }

    @Test
    fun completed() {
        assertEquals(
                DateTime(2018, 4, 17, 16, 24, 29).millis,
                vtodo("thunderbird/basic_completed.txt").completionDate)
    }

    @Test
    fun repeatDaily() {
        assertEquals(
                "FREQ=DAILY", vtodo("thunderbird/repeat_daily.txt").recurrence)
    }

    @Test
    fun priorityNotSet() {
        assertEquals(Task.Priority.NONE, vtodo("thunderbird/basic_no_due_date.txt").priority)
    }

    @Test
    fun priorityNotSpecified() {
        assertEquals(Task.Priority.NONE, vtodo("thunderbird/priority_unspecified.txt").priority)
    }

    @Test
    fun lowPriority() {
        assertEquals(Task.Priority.LOW, vtodo("thunderbird/priority_low.txt").priority)
    }

    @Test
    fun normalPriority() {
        assertEquals(Task.Priority.MEDIUM, vtodo("thunderbird/priority_normal.txt").priority)
    }

    @Test
    fun highPriority() {
        assertEquals(Task.Priority.HIGH, vtodo("thunderbird/priority_high.txt").priority)
    }

    @Test
    fun dontTruncateTimeFromUntil() {
        val (task, caldavTask, remote) = setup("thunderbird/repeat_until_date_time.txt")
        remote.applyLocal(caldavTask, task)
        assertEquals(
                "FREQ=WEEKLY;UNTIL=20200731T160000Z;BYDAY=MO,TU,WE,TH,FR",
                remote.rRule!!.value)
    }

    @Test
    fun startDateTime() {
        assertEquals(
                DateTime(2021, 1, 12, 11, 0, 1).millis,
                vtodo("thunderbird/start_date_time.txt").hideUntil)
    }

    @Test
    fun dateTimeReminder() {
        assertEquals(
            listOf(Alarm(time = 1642791600000, type = TYPE_DATE_TIME)),
            "thunderbird/date_time_reminder.txt".alarms
        )
    }

    @Test
    fun reminderBeforeStart() {
        assertEquals(
            listOf(Alarm(time = -TimeUnit.HOURS.toMillis(1), type = TYPE_REL_START)),
            "thunderbird/reminder_before_start.txt".alarms
        )
    }

    @Test
    fun reminderAfterStart() {
        assertEquals(
            listOf(Alarm(time = MINUTES.toMillis(15), type = TYPE_REL_START)),
            "thunderbird/reminder_after_start.txt".alarms
        )
    }

    @Test
    fun reminderBeforeEnd() {
        assertEquals(
            listOf(Alarm(time = -MINUTES.toMillis(15), type = TYPE_REL_END)),
            "thunderbird/reminder_before_end.txt".alarms
        )
    }

    @Test
    fun reminderAfterEnd() {
        assertEquals(
            listOf(Alarm(time = MINUTES.toMillis(15), type = TYPE_REL_END)),
            "thunderbird/reminder_after_end.txt".alarms
        )
    }

    @Test
    fun reminderAtStart() {
        assertEquals(
            listOf(Alarm(type = TYPE_REL_START)),
            "thunderbird/reminder_at_start.txt".alarms
        )
    }

    @Test
    fun reminder50Days() {
        assertEquals(
            listOf(Alarm(time = -TimeUnit.DAYS.toMillis(50), type = TYPE_REL_START)),
            "thunderbird/reminder_50_days.txt".alarms
        )
    }

    @Test
    @Ignore
    fun dontCrashOnMultipleTasks() {
        vtodo("thunderbird/completed_repeating_task.txt")
    }
}