package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import kotlinx.coroutines.runBlocking
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

    @Test
    fun advanceToNextOccurrenceWithoutCompleting() = runBlocking {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2017, 10, 4, 13, 30),
                with(COMPLETION_TIME, null as DateTime?)
        )
        task.id = 1L
        task.completionDate = 0L
        task.reminderLast = newDayTime(2017, 10, 4, 13, 30).millis

        val result = advanceToNextOccurrence(1L, task)

        assertTrue(result)
        assertEquals(newDayTime(2017, 10, 5, 13, 30).millis, task.dueDate)
        assertEquals(0L, task.reminderLast)
        assertEquals(0L, task.completionDate)
    }

    @Test
    fun advanceToNextOccurrenceReturnsFalseForNonRecurringTask() = runBlocking {
        val task = newFromDue(
                "FREQ=DAILY;INTERVAL=1",
                newDayTime(2017, 10, 4, 13, 30),
                with(COMPLETION_TIME, null as DateTime?)
        )
        task.id = 2L
        task.completionDate = 0L
        task.recurrence = null
        task.reminderLast = newDayTime(2017, 10, 4, 13, 30).millis

        val result = advanceToNextOccurrence(2L, task)

        assertFalse(result)
        assertEquals(newDayTime(2017, 10, 4, 13, 30).millis, task.dueDate)
    }
}