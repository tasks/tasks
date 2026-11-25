package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
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
    fun startTheNextOccurrenceWithNothingAcknowledged() {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2017, 10, 4, 13, 30),
                with(COMPLETION_TIME, DateTime())
        ).apply {
            reminderLast = DateTime(2017, 10, 4, 13, 30).millis
            reminderDismissed = DateTime(2017, 10, 4, 13, 31).millis
        }

        calculateNextDueDate(task)

        assertEquals(0L, task.reminderLast)
        assertEquals(0L, task.reminderDismissed)
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

    @Test
    fun dueTimeThatDoesNotExistMovesForwardAndStaysThere() {
        withTZ("America/Chicago") {
            // 2024-03-10 02:00 -> 03:00 does not exist in Chicago
            val task = newFromDue("FREQ=DAILY", newDayTime(2024, 3, 9, 2, 30))

            assertEquals(newDayTime(2024, 3, 10, 3, 30), calculateNextDueDate(task))
            assertEquals(newDayTime(2024, 3, 11, 3, 30), calculateNextDueDate(task))
        }
    }

    @Test
    fun keepTheDueDayWhenTheNextOccurrenceLandsOnAnEveningThatDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val task = newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2025, 3, 28, 23, 30))

            assertEquals(newDayTime(2025, 3, 29, 22, 59), calculateNextDueDate(task))
            assertEquals(newDayTime(2025, 3, 30, 22, 59), calculateNextDueDate(task))
        }
    }

    @Test
    fun keepTheStartDayWhenRepeatingAfterCompletionIntoAnEveningThatDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2025, 3, 20, 23, 30),
                with(COMPLETION_TIME, DateTime(2025, 3, 29, 10, 0)),
                afterComplete = true,
            )

            assertEquals(newDayTime(2025, 3, 30, 22, 59), calculateNextDueDate(task))
        }
    }
}
