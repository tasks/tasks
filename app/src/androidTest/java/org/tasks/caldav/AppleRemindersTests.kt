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
        assertEquals("Test title", vtodo("apple/basic_no_due_date.txt").getTitle())
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("apple/basic_no_due_date.txt").getNotes())
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 16, 17, 24, 10).millis,
                vtodo("apple/basic_no_due_date.txt").creationDate as Long)
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 16, 18, 0, 1, 0).millis,
                vtodo("apple/basic_due_date.txt").getDueDate() as Long)
    }

    @Test
    fun completed() {
        assertEquals(
                DateTime(2018, 4, 17, 13, 43, 2).millis,
                vtodo("apple/basic_completed.txt").completionDate as Long)
    }

    @Test
    fun repeatDaily() {
        assertEquals("RRULE:FREQ=DAILY;INTERVAL=1", vtodo("apple/repeat_daily.txt").getRecurrence())
    }

    @Test
    fun noPriority() {
        assertEquals(Task.Priority.NONE, vtodo("apple/priority_none.txt").getPriority() as Int)
    }

    @Test
    fun lowPriority() {
        assertEquals(Task.Priority.LOW, vtodo("apple/priority_low.txt").getPriority() as Int)
    }

    @Test
    fun mediumPriority() {
        assertEquals(Task.Priority.MEDIUM, vtodo("apple/priority_medium.txt").getPriority() as Int)
    }

    @Test
    fun highPriority() {
        assertEquals(Task.Priority.HIGH, vtodo("apple/priority_high.txt").getPriority() as Int)
    }
}