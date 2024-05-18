package org.tasks.caldav

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.TestUtilities.alarms
import org.tasks.TestUtilities.vtodo
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Task
import org.tasks.time.DateTime
import java.util.TimeZone

class AppleRemindersTests {
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
        assertEquals("Test title", vtodo("apple/basic_no_due_date.txt").title)
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("apple/basic_no_due_date.txt").notes)
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 16, 17, 24, 10).millis,
                vtodo("apple/basic_no_due_date.txt").creationDate)
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 16, 18, 0, 1, 0).millis,
                vtodo("apple/basic_due_date.txt").dueDate)
    }

    @Test
    fun completed() {
        assertEquals(
                DateTime(2018, 4, 17, 13, 43, 2).millis,
                vtodo("apple/basic_completed.txt").completionDate)
    }

    @Test
    fun repeatDaily() {
        assertEquals("FREQ=DAILY", vtodo("apple/repeat_daily.txt").recurrence)
    }

    @Test
    fun noPriority() {
        assertEquals(Task.Priority.NONE, vtodo("apple/priority_none.txt").priority)
    }

    @Test
    fun lowPriority() {
        assertEquals(Task.Priority.LOW, vtodo("apple/priority_low.txt").priority)
    }

    @Test
    fun mediumPriority() {
        assertEquals(Task.Priority.MEDIUM, vtodo("apple/priority_medium.txt").priority)
    }

    @Test
    fun highPriority() {
        assertEquals(Task.Priority.HIGH, vtodo("apple/priority_high.txt").priority)
    }

    @Test
    fun dateTimeReminder() {
        assertEquals(
            listOf(Alarm(time = 1642568400000, type = TYPE_DATE_TIME)),
            "apple/date_time_reminder.txt".alarms
        )
    }

    @Test
    fun ignoreLocationReminders() {
        assertTrue("apple/geofence_arrival.txt".alarms.isEmpty())
    }
}