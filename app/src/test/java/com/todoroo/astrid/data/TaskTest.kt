package com.todoroo.astrid.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
import org.tasks.data.createDueDate
import org.tasks.data.createHideUntil
import org.tasks.data.entity.Task
import org.tasks.data.isHidden
import org.tasks.data.isOverdue
import org.tasks.data.setDueDateAdjustingHideUntil
import org.tasks.date.DateTimeUtils
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_HOUR
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
    fun tomorrowIsTheNextDayAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 12, 12, 0).millis,
                    createDueDate(Task.URGENCY_TOMORROW, 0)
                )
            }
        }
    }

    @Test
    fun tomorrowIsTheNextDayAcrossFallBack() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 11, 5, 0, 30)) {
                assertEquals(
                    DateTime(2023, 11, 6, 12, 0).millis,
                    createDueDate(Task.URGENCY_TOMORROW, 0)
                )
            }
        }
    }

    @Test
    fun dayAfterTomorrowIsTwoDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 13, 12, 0).millis,
                    createDueDate(Task.URGENCY_DAY_AFTER, 0)
                )
            }
        }
    }

    @Test
    fun nextWeekIsSevenDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 18, 12, 0).millis,
                    createDueDate(Task.URGENCY_NEXT_WEEK, 0)
                )
            }
        }
    }

    @Test
    fun twoWeeksIsFourteenDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 25, 12, 0).millis,
                    createDueDate(Task.URGENCY_IN_TWO_WEEKS, 0)
                )
            }
        }
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
    fun allDayStartDateStaysAtMidnightWhenDueDateMoves() {
        val task = allDayTaskStartingDayBefore()
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 20, 0, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 0, 0).millis, task.hideUntil)
        assertFalse(task.hasStartTime())
    }

    @Test
    fun allDayStartDateStaysPutWhenMorningDueTimeIsAdded() {
        val task = allDayTaskStartingDayBefore()
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 19, 9, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 18, 0, 0).millis, task.hideUntil)
        assertFalse(task.hasStartTime())
    }

    @Test
    fun allDayStartDateStaysAtMidnightWhenDueTimeIsAdded() {
        val task = allDayTaskStartingDayBefore()
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 19, 15, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 18, 0, 0).millis, task.hideUntil)
        assertFalse(task.hasStartTime())
    }

    @Test
    fun allDayStartDateStaysAtMidnightWhenDueDateGetsTime() {
        val task = allDayTaskStartingDayBefore()
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 20, 15, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 0, 0).millis, task.hideUntil)
        assertFalse(task.hasStartTime())
    }

    @Test
    fun allDayStartDateStaysAtMidnightWhenDueDateIsRenormalized() {
        val task = Task(dueDate = DateTime(2023, 7, 19, 0, 0).millis)
        task.hideUntil = DateTime(2023, 7, 18, 0, 0).millis
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 19, 0, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 18, 0, 0).millis, task.hideUntil)
        assertFalse(task.hasStartTime())
    }

    @Test
    fun keepStartTimeWhenDueDateMoves() {
        val task = Task(
            dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 19, 0, 0).millis)
        )
        task.hideUntil = task.createHideUntil(
            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
            DateTime(2023, 7, 18, 9, 0).millis
        )
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 20, 0, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 9, 0, 1).millis, task.hideUntil)
        assertTrue(task.hasStartTime())
    }

    @Test
    fun keepStartTimeWhenDueTimeIsCleared() {
        val task = Task(
            dueDate = createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                DateTime(2023, 7, 19, 15, 0).millis
            )
        )
        task.hideUntil = task.createHideUntil(
            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
            DateTime(2023, 7, 18, 9, 0).millis
        )
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 19, 0, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 18, 9, 0, 1).millis, task.hideUntil)
        assertTrue(task.hasStartTime())
    }

    @Test
    fun keepStartTimeWhenDueDateMovesAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 3, 11, 9, 0).millis
                )
            )
            task.hideUntil = task.createHideUntil(
                Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
                DateTime(2023, 3, 10, 9, 0).millis
            )
            task.setDueDateAdjustingHideUntil(
                createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 3, 12, 9, 0).millis
                )
            )
            assertEquals(DateTime(2023, 3, 11, 9, 0, 1).millis, task.hideUntil)
            assertTrue(task.hasStartTime())
        }
    }

    @Test
    fun keepAllDayStartDateWhenDueDateMovesAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY,
                    DateTime(2023, 3, 11, 0, 0).millis
                )
            ).apply {
                hideUntil = createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
            }
            task.setDueDateAdjustingHideUntil(
                createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 3, 12, 0, 0).millis)
            )
            assertEquals(DateTime(2023, 3, 11, 0, 0).millis, task.hideUntil)
            assertFalse(task.hasStartTime())
        }
    }

    @Test
    fun dayBeforeStartDateDoesNotSkipADayAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 3, 13, 0, 30).millis
                )
            )
            assertEquals(
                DateTime(2023, 3, 12, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
            )
        }
    }

    @Test
    fun weekBeforeStartDateDoesNotSkipADayAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 3, 13, 0, 30).millis
                )
            )
            assertEquals(
                DateTime(2023, 3, 6, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_WEEK_BEFORE, 0)
            )
        }
    }

    @Test
    fun startDateFollowsDueTimeWhenHidingUntilDueTime() {
        val task = taskHidingUntilDueTime(DateTime(2023, 7, 19, 9, 0))
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 19, 15, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 15, 0, 1).millis, task.hideUntil)
        assertEquals(task.dueDate, task.hideUntil)
    }

    @Test
    fun startDateFollowsDueTimeWhenHidingUntilDueTimeOnAnotherDay() {
        val task = taskHidingUntilDueTime(DateTime(2023, 7, 19, 9, 0))
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 20, 15, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 20, 15, 0, 1).millis, task.hideUntil)
        assertEquals(task.dueDate, task.hideUntil)
    }

    @Test
    fun keepStartTimeWhenDueTimeChangesOnSameDay() {
        val task = Task(
            dueDate = createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                DateTime(2023, 7, 19, 9, 0).millis
            )
        )
        task.hideUntil = task.createHideUntil(
            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
            DateTime(2023, 7, 18, 8, 0).millis
        )
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 19, 15, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 18, 8, 0, 1).millis, task.hideUntil)
        assertTrue(task.hasStartTime())
    }

    @Test
    fun startDateDoesNotPassDueDateWhenDueTimeMovesEarlier() {
        val task = Task(
            dueDate = createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                DateTime(2023, 7, 19, 23, 30).millis
            )
        )
        task.hideUntil = task.createHideUntil(
            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
            DateTime(2023, 7, 19, 5, 26).millis
        )
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 7, 19, 1, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 1, 0, 1).millis, task.hideUntil)
        assertEquals(task.dueDate, task.hideUntil)
    }

    @Test
    fun dayBeforeStartDateStaysOnTheDayBeforeAcrossFallBack() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 11, 5, 23, 44).millis
                )
            )
            assertEquals(
                DateTime(2023, 11, 4, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
            )
        }
    }

    @Test
    fun weekBeforeStartDateDoesNotSkipADayAcrossFallBack() {
        withTZ("America/Chicago") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 11, 6, 23, 59).millis
                )
            )
            assertEquals(
                DateTime(2023, 10, 30, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_WEEK_BEFORE, 0)
            )
        }
    }

    @Test
    fun startDateUsesTheFirstValidTimeWhenMidnightDoesNotExist() {
        withTZ("America/Santiago") {
            // 2024-09-08 00:00 -> 01:00 does not exist in Santiago
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY,
                    DateTime(2024, 9, 9, 0, 0).millis
                )
            )
            assertEquals(
                DateTime(2024, 9, 8, 1, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
            )
        }
    }

    @Test
    fun dayBeforeStartDateStaysOnTheDayBeforeWhenLateEveningDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2025, 3, 30, 23, 30).millis
                )
            )
            assertEquals(
                DateTime(2025, 3, 29, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
            )
        }
    }

    @Test
    fun weekBeforeStartDateStaysAWeekBeforeWhenLateEveningDoesNotExist() {
        withTZ("America/Nuuk") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2025, 4, 5, 23, 30).millis
                )
            )
            assertEquals(
                DateTime(2025, 3, 29, 0, 0).millis,
                task.createHideUntil(Task.HIDE_UNTIL_WEEK_BEFORE, 0)
            )
        }
    }

    @Test
    fun startDateStaysOnTheDayBeforeWhenLateEveningDoesNotExist() {
        withTZ("America/Nuuk") {
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2025, 3, 28, 23, 30).millis
                )
            )
            task.hideUntil = task.createHideUntil(
                Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
                DateTime(2025, 3, 27, 23, 30).millis
            )

            task.setDueDateAdjustingHideUntil(
                createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2025, 3, 29, 23, 30).millis
                )
            )

            assertEquals(DateTime(2025, 3, 30, 0, 0).millis - 1, task.hideUntil)
            assertTrue(task.hasStartTime())
            assertTrue(task.hideUntil < task.dueDate)
        }
    }

    @Test
    fun keepStartTimeWhenDueTimeIsClearedOnTheSameDay() {
        val task = Task(
            dueDate = createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                DateTime(2023, 7, 19, 15, 52).millis
            )
        )
        task.hideUntil = task.createHideUntil(
            Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
            DateTime(2023, 7, 19, 15, 6).millis
        )
        task.setDueDateAdjustingHideUntil(
            createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 19, 0, 0).millis)
        )
        assertEquals(DateTime(2023, 7, 19, 15, 6, 1).millis, task.hideUntil)
        assertTrue(task.hasStartTime())
    }

    @Test
    fun keepStartTimeInTheRepeatedHourWhenDueTimeChangesOnTheSameDay() {
        withTZ("America/Chicago") {
            // 2023-11-05 01:00 -> 02:00 happens twice in Chicago
            val startInRepeatedHour = DateTime(2023, 11, 5, 1, 0, 1).millis + ONE_HOUR
            val task = Task(
                dueDate = createDueDate(
                    Task.URGENCY_SPECIFIC_DAY_TIME,
                    DateTime(2023, 11, 5, 9, 0).millis
                )
            ).apply { hideUntil = startInRepeatedHour }

            task.setDueDateAdjustingHideUntil(
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2023, 11, 5, 15, 0).millis)
            )

            assertEquals(startInRepeatedHour, task.hideUntil)
            assertTrue(task.hasStartTime())
        }
    }

    private fun taskHidingUntilDueTime(due: DateTime) = Task(
        dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, due.millis)
    ).apply {
        hideUntil = createHideUntil(Task.HIDE_UNTIL_DUE_TIME, 0)
    }

    private fun allDayTaskStartingDayBefore() = Task(
        dueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(2023, 7, 19, 0, 0).millis)
    ).apply {
        hideUntil = createHideUntil(Task.HIDE_UNTIL_DAY_BEFORE, 0)
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