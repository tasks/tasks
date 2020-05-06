package org.tasks.caldav

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.data.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.TestUtilities.vtodo
import org.tasks.time.DateTime
import java.util.*

@RunWith(AndroidJUnit4::class)
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
        assertEquals("Test title", vtodo("thunderbird/basic_no_due_date.txt").getTitle())
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("thunderbird/basic_no_due_date.txt").getNotes())
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 17, 11, 31, 52).millis,
                vtodo("thunderbird/basic_no_due_date.txt").creationDate as Long)
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 17, 14, 0, 1).millis,
                vtodo("thunderbird/basic_due_date.txt").getDueDate() as Long)
    }

    @Test
    fun completed() {
        assertEquals(
                DateTime(2018, 4, 17, 16, 24, 29).millis,
                vtodo("thunderbird/basic_completed.txt").completionDate as Long)
    }

    @Test
    fun repeatDaily() {
        assertEquals(
                "RRULE:FREQ=DAILY;INTERVAL=1", vtodo("thunderbird/repeat_daily.txt").getRecurrence())
    }

    @Test
    fun priorityNotSet() {
        assertEquals(Task.Priority.NONE, vtodo("thunderbird/basic_no_due_date.txt").getPriority() as Int)
    }

    @Test
    fun priorityNotSpecified() {
        assertEquals(Task.Priority.NONE, vtodo("thunderbird/priority_unspecified.txt").getPriority() as Int)
    }

    @Test
    fun lowPriority() {
        assertEquals(Task.Priority.LOW, vtodo("thunderbird/priority_low.txt").getPriority() as Int)
    }

    @Test
    fun normalPriority() {
        assertEquals(Task.Priority.MEDIUM, vtodo("thunderbird/priority_normal.txt").getPriority() as Int)
    }

    @Test
    fun highPriority() {
        assertEquals(Task.Priority.HIGH, vtodo("thunderbird/priority_high.txt").getPriority() as Int)
    }
}