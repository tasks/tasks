package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME

class RepeatYearlyTests : RepeatTests() {
    @Test
    fun testRepeatYearlyFromDueDate() {
        val task = newFromDue("FREQ=YEARLY;INTERVAL=2", newDayTime(2016, 8, 28, 1, 44))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2018, 8, 28, 1, 44), next)
    }

    @Test
    fun testRepeatYearlyFromDueDateNoInterval() {
        val task = newFromDue("FREQ=YEARLY", newDayTime(2016, 8, 28, 1, 44))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 8, 28, 1, 44), next)
    }

    @Test
    fun testRepeatYearlyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=YEARLY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, newDayTime(2016, 8, 29, 0, 14)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 8, 29, 0, 25), next)
    }

    @Test
    fun testRepeatYearlyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=YEARLY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, newDayTime(2016, 8, 29, 0, 14)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 8, 29, 0, 4), next)
    }
}