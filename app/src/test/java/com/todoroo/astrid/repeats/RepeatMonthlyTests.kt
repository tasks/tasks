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
}