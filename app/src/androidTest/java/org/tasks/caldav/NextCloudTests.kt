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
class NextCloudTests {
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
        assertEquals("Test title", vtodo("nextcloud/basic_no_due_date.txt").getTitle())
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("nextcloud/basic_no_due_date.txt").getNotes())
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 17, 11, 32, 3).millis,
                vtodo("nextcloud/basic_no_due_date.txt").creationDate as Long)
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 17, 17, 0, 1).millis,
                vtodo("nextcloud/basic_due_date.txt").getDueDate() as Long)
    }

    @Test
    fun priorityNoStars() {
        assertEquals(Task.Priority.NONE, vtodo("nextcloud/priority_no_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityOneStar() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_1_star.txt").getPriority() as Int)
    }

    @Test
    fun priorityTwoStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_2_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityThreeStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_3_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityFourStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_4_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityFiveStars() {
        assertEquals(Task.Priority.MEDIUM, vtodo("nextcloud/priority_5_stars.txt").getPriority() as Int)
    }

    @Test
    fun prioritySixStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_6_stars.txt").getPriority() as Int)
    }

    @Test
    fun prioritySevenStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_7_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityEightStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_8_stars.txt").getPriority() as Int)
    }

    @Test
    fun priorityNineStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_9_stars.txt").getPriority() as Int)
    }
}