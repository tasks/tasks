package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.time.DateTime

class RepeatHourlyTests : RepeatTests() {
    @Test
    fun testRepeatHourlyFromDueDate() {
        val task = newFromDue("FREQ=HOURLY;INTERVAL=6", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 26, 18, 30), next)
    }

    @Test
    fun testRepeatHourlyNoInterval() {
        val task = newFromDue(
            "FREQ=HOURLY",
            newDayTime(2016, 8, 30, 0, 25),
            with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
            afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 1, 14), next)
    }

    @Test
    fun testRepeatHourlyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=HOURLY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 29, 1, 14), next)
    }

    @Test
    fun testRepeatHourlyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=HOURLY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = freezeAt(newDayTime(2016, 8, 29, 0, 14)) {
            calculateNextDueDate(task)
        }

        assertEquals(newDayTime(2016, 8, 29, 1, 14), next)
    }
}