package com.todoroo.astrid.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.data.isHidden
import org.tasks.data.isOverdue
import org.tasks.date.DateTimeUtils
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.TreeSet

class TaskTest {
    @Before
    fun setUp() {
        freezeAt(now)
    }

    @After
    fun tearDown() {
        Freeze.thaw()
    }

    @Test
    fun testCreateDueDateNoUrgency() {
        assertEquals(0, createDueDate(Task.URGENCY_NONE, 1L))
    }

    @Test
    fun testCreateDueDateToday() {
        val expected = DateTime(2013, 12, 31, 12, 0, 0, 0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_TODAY, -1L))
    }

    @Test
    fun testCreateDueDateTomorrow() {
        val expected = DateTime(2014, 1, 1, 12, 0, 0, 0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_TOMORROW, -1L))
    }

    @Test
    fun testCreateDueDateDayAfter() {
        val expected = DateTime(2014, 1, 2, 12, 0, 0, 0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_DAY_AFTER, -1L))
    }

    @Test
    fun testCreateDueDateNextWeek() {
        val expected = DateTime(2014, 1, 7, 12, 0, 0, 0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_NEXT_WEEK, -1L))
    }

    @Test
    fun testCreateDueDateInTwoWeeks() {
        val expected = DateTime(2014, 1, 14, 12, 0, 0, 0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_IN_TWO_WEEKS, -1L))
    }

    @Test
    fun testRemoveTimeForSpecificDay() {
        val expected = specificDueDate
                .withHourOfDay(12)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)
                .millis
        assertEquals(expected, createDueDate(Task.URGENCY_SPECIFIC_DAY, specificDueDate.millis))
    }

    @Test
    fun testRemoveSecondsForSpecificTime() {
        val expected = specificDueDate.withSecondOfMinute(1).withMillisOfSecond(0).millis
        assertEquals(expected, createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, specificDueDate.millis))
    }

    @Test
    fun testTaskHasDueTime() {
        val task = Task()
        task.dueDate = 1388516076000L
        assertTrue(task.hasDueTime())
        assertTrue(task.hasDueDate())
    }

    @Test
    fun testTaskHasDueDate() {
        val task = Task()
        task.dueDate = 1388469600000L
        assertFalse(task.hasDueTime())
        assertTrue(task.hasDueDate())
    }

    @Test
    fun testDoesHaveDueTime() {
        assertTrue(Task.hasDueTime(1388516076000L))
    }

    @Test
    fun testNoDueTime() {
        assertFalse(Task.hasDueTime(DateTimeUtils.newDateTime().startOfDay().millis))
        assertFalse(Task.hasDueTime(DateTimeUtils.newDateTime().withMillisOfDay(60000).millis))
    }

    @Test
    fun testHasDueTime() {
        assertTrue(Task.hasDueTime(DateTimeUtils.newDateTime().withMillisOfDay(1).millis))
        assertTrue(Task.hasDueTime(DateTimeUtils.newDateTime().withMillisOfDay(1000).millis))
        assertTrue(Task.hasDueTime(DateTimeUtils.newDateTime().withMillisOfDay(59999).millis))
    }

    @Test
    fun testDoesNotHaveDueTime() {
        assertFalse(Task.hasDueTime(1388469600000L))
    }

    @Test
    fun testNewTaskIsNotCompleted() {
        assertFalse(Task().isCompleted)
    }

    @Test
    fun testNewTaskNotDeleted() {
        assertFalse(Task().isDeleted)
    }

    @Test
    fun testNewTaskNotHidden() {
        assertFalse(Task().isHidden)
    }

    @Test
    fun testNewTaskDoesNotHaveDueDateOrTime() {
        assertFalse(Task().hasDueDate())
        assertFalse(Task().hasDueTime())
    }

    @Test
    fun testTaskIsCompleted() {
        val task = Task()
        task.completionDate = 1L
        assertTrue(task.isCompleted)
    }

    @Test
    fun testTaskIsNotHiddenAtHideUntilTime() {
        val now = currentTimeMillis()
        freezeAt(now) {
            val task = Task()
            task.hideUntil = now
            assertFalse(task.isHidden)
        }
    }

    @Test
    fun testTaskIsHiddenBeforeHideUntilTime() {
        val now = currentTimeMillis()
        freezeAt(now) {
            val task = Task()
            task.hideUntil = now + 1
            assertTrue(task.isHidden)
        }
    }

    @Test
    fun testTaskIsDeleted() {
        val task = Task()
        task.deletionDate = 1L
        assertTrue(task.isDeleted)
    }

    @Test
    fun testTaskWithNoDueDateIsNotOverdue() {
        assertFalse(Task().isOverdue)
    }

    @Test
    fun testTaskNotOverdueAtDueTime() {
        val now = currentTimeMillis()
        freezeAt(now) {
            val task = Task()
            task.dueDate = now
            assertFalse(task.isOverdue)
        }
    }

    @Test
    fun testTaskIsOverduePastDueTime() {
        val dueDate = currentTimeMillis()
        freezeAt(dueDate + 1) {
            val task = Task()
            task.dueDate = dueDate
            assertTrue(task.isOverdue)
        }
    }

    @Test
    fun testTaskNotOverdueBeforeNoonOnDueDate() {
        val dueDate = DateTime().startOfDay()
        freezeAt(dueDate.plusHours(12).minusMillis(1)) {
            val task = Task()
            task.dueDate = dueDate.millis
            assertFalse(task.hasDueTime())
            assertFalse(task.isOverdue)
        }
    }

    @Test
    fun testTaskOverdueAtNoonOnDueDate() {
        val dueDate = DateTime().startOfDay()
        freezeAt(dueDate.plusHours(12)) {
            val task = Task()
            task.dueDate = dueDate.millis
            assertFalse(task.hasDueTime())
            assertFalse(task.isOverdue)
        }
    }

    @Test
    fun testTaskWithNoDueTimeIsOverdue() {
        val dueDate = DateTime().startOfDay()
        freezeAt(dueDate.plusDays(1)) {
            val task = Task()
            task.dueDate = dueDate.millis
            assertFalse(task.hasDueTime())
            assertTrue(task.isOverdue)
        }
    }

    @Test
    fun testSanity() {
        assertTrue(Task.Priority.HIGH < Task.Priority.MEDIUM)
        assertTrue(Task.Priority.MEDIUM < Task.Priority.LOW)
        assertTrue(Task.Priority.LOW < Task.Priority.NONE)
        val reminderFlags = ArrayList<Int>()
        reminderFlags.add(Task.NOTIFY_AFTER_DEADLINE)
        reminderFlags.add(Task.NOTIFY_AT_DEADLINE)
        reminderFlags.add(Task.NOTIFY_MODE_NONSTOP)

        // assert no duplicates
        assertEquals(TreeSet(reminderFlags).size, reminderFlags.size)
    }

    companion object {
        private val now = DateTime(2013, 12, 31, 16, 10, 53, 452)
        private val specificDueDate = DateTime(2014, 3, 17, 9, 54, 27, 959)
    }
}