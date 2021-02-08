package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.time.DateTime

class RepeatMonthlyTests : RepeatTests() {
    @Test
    fun testRepeatMonthlyFromDueDate() {
        val task = newFromDue("FREQ=MONTHLY;INTERVAL=3", newDayTime(2016, 8, 28, 1, 44))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 11, 28, 1, 44), next)
    }

    @Test
    fun testRepeatMonthlyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 29, 0, 25), next)
    }

    @Test
    fun testRepeatMonthlyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 29, 0, 4), next)
    }

    @Test
    fun repeatAtEndOfJanuary() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2017, 1, 31, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 2, 28, 13, 30), next)
    }

    /* https://tools.ietf.org/html/rfc5545#section-3.3.10
     * Recurrence rules may generate recurrence instances with an invalid
     * date (e.g., February 30) or nonexistent local time (e.g., 1:30 AM
     * on a day where the local time is moved forward by an hour at 1:00
     * AM).  Such recurrence instances MUST be ignored and MUST NOT be
     * counted as part of the recurrence set.
     */
    @Test
    fun repeatJanuary30th() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2017, 1, 30, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 3, 30, 13, 30), next)
    }
}