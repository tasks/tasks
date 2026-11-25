package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
import org.tasks.data.createHideUntil
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.ONE_HOUR

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
    fun keepAlarmTimeWhenRepeatingAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2023, 3, 11, 9, 0))

            assertEquals(
                listOf(DateTime(2023, 3, 11, 9, 0).millis),
                repeatWithAlarm(task, DateTime(2023, 3, 10, 9, 0))
            )
        }
    }

    @Test
    fun moveAlarmTimeWhenRepeatingHourly() {
        withTZ("America/Chicago") {
            val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 7, 19, 9, 0))

            assertEquals(
                listOf(DateTime(2023, 7, 19, 9, 55).millis),
                repeatWithAlarm(task, DateTime(2023, 7, 19, 8, 55))
            )
        }
    }

    @Test
    fun moveAlarmTimeWhenHourlyRepeatCrossesMidnight() {
        withTZ("America/Chicago") {
            val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 7, 19, 23, 30))

            assertEquals(
                listOf(DateTime(2023, 7, 20, 0, 25).millis),
                repeatWithAlarm(task, DateTime(2023, 7, 19, 23, 25))
            )
        }
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
    fun keepStartDateOnDueDateWhenRepeatingAfterCompletionMovesItEarlier() {
        withTZ("America/Chicago") {
            val task = newFromDue(
                "FREQ=HOURLY;INTERVAL=1",
                newDayTime(2023, 7, 19, 23, 30),
                with(COMPLETION_TIME, DateTime(2023, 7, 19, 8, 0)),
                afterComplete = true,
            ).apply {
                hideUntil = dueDate
            }

            val next = calculateNextDueDate(task)

            assertEquals(newDayTime(2023, 7, 19, 9, 0), next)
            assertEquals(task.dueDate, task.hideUntil)
        }
    }

    @Test
    fun keepAlarmOnTheSameDayWhenRepeatingIntoAnEveningThatDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val task = newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2025, 3, 28, 9, 0))

            assertEquals(
                listOf(DateTime(2025, 3, 30, 0, 0).millis - 1),
                repeatWithAlarm(task, DateTime(2025, 3, 28, 23, 30))
            )
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

    @Test
    fun keepStartTimeAheadOfDueTimeWhenRepeatingHourly() {
        val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 7, 19, 9, 0))
            .startingAt(DateTime(2023, 7, 19, 8, 45))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2023, 7, 19, 10, 0), next)
        assertEquals(DateTime(2023, 7, 19, 9, 45, 1).millis, task.hideUntil)
    }

    @Test
    fun keepStartTimeAheadOfDueTimeWhenHourlyRepeatCrossesMidnight() {
        val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 7, 19, 23, 45))
            .startingAt(DateTime(2023, 7, 19, 23, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2023, 7, 20, 0, 45), next)
        assertEquals(DateTime(2023, 7, 20, 0, 30, 1).millis, task.hideUntil)
    }

    @Test
    fun startTimeKeepsItsLeadAcrossSeveralHourlyRepeats() {
        val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 7, 19, 22, 45))
            .startingAt(DateTime(2023, 7, 19, 22, 30))

        repeat(3) { calculateNextDueDate(task) }

        assertEquals(newDayTime(2023, 7, 20, 1, 45).millis, task.dueDate)
        assertEquals(DateTime(2023, 7, 20, 1, 30, 1).millis, task.hideUntil)
    }

    @Test
    fun keepStartTimeOnTheDayBeforeWhenRepeatingDailyAcrossDaylightSavings() {
        withTZ("America/Chicago") {
            val task = newFromDue("FREQ=DAILY;INTERVAL=1", newDayTime(2023, 3, 11, 9, 0))
                .startingAt(DateTime(2023, 3, 10, 9, 0))

            val next = calculateNextDueDate(task)

            assertEquals(newDayTime(2023, 3, 12, 9, 0), next)
            assertEquals(DateTime(2023, 3, 11, 9, 0, 1).millis, task.hideUntil)
        }
    }

    @Test
    fun restoreStartTimeWhenUndoingAnHourlyRepeat() {
        val due = newDayTime(2023, 7, 19, 9, 0)
        val task = newFromDue("FREQ=HOURLY;INTERVAL=1", due)
            .startingAt(DateTime(2023, 7, 19, 8, 45))

        calculateNextDueDate(task)
        undoRepeat(task, due.millis)

        assertEquals(due.millis, task.dueDate)
        assertEquals(DateTime(2023, 7, 19, 8, 45, 1).millis, task.hideUntil)
    }

    @Test
    fun keepAlarmInTheRepeatedHourWhenHourlyRepeatDoesNotMoveTheDueDate() {
        withTZ("America/Chicago") {
            // 2023-11-05 01:00 -> 02:00 happens twice, so the next hour is the same clock time
            val task = newFromDue("FREQ=HOURLY;INTERVAL=1", newDayTime(2023, 11, 5, 1, 30))
            val alarm = DateTime(DateTime(2023, 11, 5, 1, 0).millis + ONE_HOUR)

            assertEquals(listOf(alarm.millis), repeatWithAlarm(task, alarm))
        }
    }

    private fun Task.startingAt(start: DateTime) = apply {
        hideUntil = createHideUntil(Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, start.millis)
    }

    private fun repeatWithAlarm(task: Task, alarm: DateTime): List<Long> {
        val synchronized = mutableListOf<Alarm>()
        alarmService.stub {
            onBlocking { getAlarms(any<Long>()) } doReturn listOf(
                Alarm(
                    task = task.id,
                    time = alarm.millis,
                    type = Alarm.TYPE_DATE_TIME,
                )
            )
            onBlocking { synchronizeAlarms(any<Long>(), any()) } doAnswer {
                synchronized.addAll(it.getArgument<MutableSet<Alarm>>(1))
                false
            }
        }

        calculateNextDueDate(task)

        return synchronized.map { it.time }
    }
}
