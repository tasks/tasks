package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime

class RepeatTaskHelperTests : RepeatTests() {
    @Test
    fun shouldDecrementCount() {
        val task = newFromDue(
                "RRULE:FREQ=MINUTELY;COUNT=2;INTERVAL=30",
                newDayTime(2017, 10, 4, 13, 30)
        )

        calculateNextDueDate(task)

        assertEquals(1, newRecur(task.recurrence!!).count)
    }

    @Test
    fun shouldUncompleteTask() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2017, 10, 4, 13, 30),
                with(COMPLETION_TIME, DateTime())
        )

        calculateNextDueDate(task)

        assertFalse(task.isCompleted)
    }

    @Test
    fun dontAdjustOnLastInstance() {
        val task = newFromDue(
                "FREQ=MINUTELY;COUNT=1;INTERVAL=30",
                newDayTime(2017, 10, 4, 13, 30),
                with(COMPLETION_TIME, DateTime())
        )

        val next = calculateNextDueDate(task)

        assertEquals(1, newRecur(task.recurrence!!).count)
        assertEquals(newDayTime(2017, 10, 4, 13, 30), next)
        assertTrue(task.isCompleted)
    }

    @Test
    fun useCompletionWhenNoDue() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                DateTime(0)
        )

        val next = freezeAt(DateTime(2021, 2, 1, 16, 54, 32, 451)) {
            calculateNextDueDate(task)
        }

        assertEquals(newDay(2021, 2, 2), next)
    }
}