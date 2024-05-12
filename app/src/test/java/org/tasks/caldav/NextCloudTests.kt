package org.tasks.caldav

import org.tasks.data.entity.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.TestUtilities.vtodo
import org.tasks.data.createDueDate
import org.tasks.time.DateTime
import java.util.TimeZone

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
        assertEquals("Test title", vtodo("nextcloud/basic_no_due_date.txt").title)
    }

    @Test
    fun readDescription() {
        assertEquals("Test description", vtodo("nextcloud/basic_no_due_date.txt").notes)
    }

    @Test
    fun readCreationDate() {
        assertEquals(
                DateTime(2018, 4, 17, 11, 32, 3).millis,
            vtodo("nextcloud/basic_no_due_date.txt").creationDate
        )
    }

    @Test
    fun readDueDate() {
        assertEquals(
                DateTime(2018, 4, 17, 17, 0, 1).millis,
                vtodo("nextcloud/basic_due_date.txt").dueDate)
    }

    @Test
    fun readAllDayTask() {
        assertEquals(
                createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2021, 2, 1).millis),
                vtodo("nextcloud/all_day_task.txt").dueDate
        )
    }

    @Test
    fun priorityNoStars() {
        assertEquals(Task.Priority.NONE, vtodo("nextcloud/priority_no_stars.txt").priority)
    }

    @Test
    fun priorityOneStar() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_1_star.txt").priority)
    }

    @Test
    fun priorityTwoStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_2_stars.txt").priority)
    }

    @Test
    fun priorityThreeStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_3_stars.txt").priority)
    }

    @Test
    fun priorityFourStars() {
        assertEquals(Task.Priority.LOW, vtodo("nextcloud/priority_4_stars.txt").priority)
    }

    @Test
    fun priorityFiveStars() {
        assertEquals(Task.Priority.MEDIUM, vtodo("nextcloud/priority_5_stars.txt").priority)
    }

    @Test
    fun prioritySixStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_6_stars.txt").priority)
    }

    @Test
    fun prioritySevenStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_7_stars.txt").priority)
    }

    @Test
    fun priorityEightStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_8_stars.txt").priority)
    }

    @Test
    fun priorityNineStars() {
        assertEquals(Task.Priority.HIGH, vtodo("nextcloud/priority_9_stars.txt").priority)
    }
}