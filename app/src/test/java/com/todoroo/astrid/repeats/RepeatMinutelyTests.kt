package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.time.DateTime

class RepeatMinutelyTests : RepeatTests() {
    @Test
    fun testRepeatMinutelyFromDueDate() {
        val task = newFromDue("FREQ=MINUTELY;INTERVAL=30", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 26, 13, 0), next)
    }

    @Test
    fun testRepeatMinutelyFromDueDateNoInterval() {
        val task = newFromDue("FREQ=MINUTELY", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 26, 12, 31), next)
    }

    @Test
    fun testRepeatMinutelyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=MINUTELY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 0, 15), next)
    }

    @Test
    fun testRepeatMinutelyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=MINUTELY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 0, 15), next)
    }
}