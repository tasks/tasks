package org.tasks.googleapis

import org.junit.Assert.assertEquals
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.googleapis.GoogleTaskSynchronizer.Companion.mergeDates
import org.tasks.time.DateTime
import org.junit.Test
import java.util.TimeZone

class GoogleDateMergeTest {
    private fun withTZ(id: String, block: () -> Unit) {
        val default = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(default)
        }
    }

    private fun allDay(year: Int, month: Int, day: Int) =
        createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime(year, month, day).millis)

    private fun dueTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(year, month, day, hour, minute).millis)

    @Test
    fun keepLocalDueTimeWhenGoogleMovesTheDay() {
        withTZ("America/Chicago") {
            val task = Task(dueDate = dueTime(2023, 7, 18, 15, 0))

            mergeDates(allDay(2023, 7, 19), task)

            assertEquals(DateTime(2023, 7, 19, 15, 0, 1).millis, task.dueDate)
        }
    }

    @Test
    fun repeatedPullsDoNotMoveADueTimeThatDoesNotExist() {
        withTZ("Pacific/Chatham") {
            // 2019-09-29 02:45 -> 03:45 does not exist in Chatham
            val task = Task(dueDate = dueTime(2022, 4, 3, 2, 50))

            mergeDates(allDay(2019, 9, 29), task)
            assertEquals(DateTime(2019, 9, 29, 3, 50, 1).millis, task.dueDate)

            mergeDates(allDay(2019, 9, 29), task)
            assertEquals(DateTime(2019, 9, 29, 3, 50, 1).millis, task.dueDate)
        }
    }

    @Test
    fun remoteDayIsNotPushedForwardWhenTheLocalDueTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val task = Task(dueDate = dueTime(2025, 3, 30, 23, 30))

            mergeDates(allDay(2025, 3, 29), task)

            assertEquals(
                DateTime(2025, 3, 29).millis,
                DateTime(task.dueDate).startOfDay().millis
            )
        }
    }

}
