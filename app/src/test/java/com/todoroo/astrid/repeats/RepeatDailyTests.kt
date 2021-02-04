package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.time.DateTime

class RepeatDailyTests : RepeatTests() {
    @Test
    fun testRepeatDailyFromDueDate() {
        val task = newFromDue("FREQ=DAILY;INTERVAL=3", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 12, 30), next)
    }

    @Test
    fun testRepeatDailyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 30, 0, 25), next)
    }

    @Test
    fun testRepeatDailyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 30, 0, 4), next)
    }
}