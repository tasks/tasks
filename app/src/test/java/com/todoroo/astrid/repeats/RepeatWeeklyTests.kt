package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME

class RepeatWeeklyTests : RepeatTests() {
    @Test
    fun testRepeatWeeklyFromDueDate() {
        val task = newFromDue("FREQ=WEEKLY;INTERVAL=1", newDayTime(2016, 8, 28, 1, 34))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 4, 1, 34), next)
    }

    @Test
    fun testRepeatWeeklyFromDueDateNoInterval() {
        val task = newFromDue("FREQ=WEEKLY", newDayTime(2016, 8, 28, 1, 34))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 4, 1, 34), next)
    }

    @Test
    fun testRepeatBiWeekly() {
        val task = newFromDue("FREQ=WEEKLY;INTERVAL=2", newDayTime(2016, 8, 28, 1, 34))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 11, 1, 34), next)
    }

    @Test
    fun testRepeatWeeklyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, newDayTime(2016, 8, 29, 0, 14)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun testRepeatWeeklyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, newDayTime(2016, 8, 29, 0, 14)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 4), next)
    }

    @Test
    fun testWeeklyBySingleDayBefore() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO",
                newDayTime(2016, 8, 28, 0, 25) // Sunday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 0, 25), next)
    }

    @Test
    fun testWeeklyBySingleDayOf() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO",
                newDayTime(2016, 8, 29, 0, 25) // Monday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun testWeeklyBySingleDayAfter() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO",
                newDayTime(2016, 8, 30, 0, 25) // Sunday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun testByDayBeforeFirstDate() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
                newDayTime(2016, 8, 28, 0, 25) // Sunday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 0, 25), next)
    }

    @Test
    fun testAdvancedRepeatWeeklyOnFirstDate() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
                newDayTime(2016, 8, 29, 0, 25) // Monday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 31, 0, 25), next)
    }

    @Test
    fun testAdvancedRepeatWeeklyOnLastDate() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
                newDayTime(2016, 8, 31, 0, 25) // Wednesday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun testAdvancedRepeatWeeklyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
                newDayTime(2016, 8, 29, 0, 25),
                with(COMPLETION_TIME, newDayTime(2016, 8, 28, 1, 9)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 0, 25), next)
    }

    @Test
    fun testAdvancedRepeatWeeklyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
                newDayTime(2016, 8, 29, 0, 25),
                with(COMPLETION_TIME, newDayTime(2016, 9, 1, 1, 9)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun biweeklyOnBeforeFirstDay() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE",
                newDayTime(2016, 8, 28, 0, 25), // Sunday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 5, 0, 25), next)
    }

    @Test
    fun biweeklyOnFirstDay() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE",
                newDayTime(2016, 8, 29, 0, 25), // Monday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 31, 0, 25), next)
    }

    @Test
    fun biweeklyOnLastDay() {
        val task = newFromDue(
                "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE",
                newDayTime(2016, 8, 31, 0, 25), // Wednesday
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 12, 0, 25), next)
    }
}