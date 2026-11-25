package com.todoroo.astrid.api

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.setCurrentMillisFixed
import org.tasks.time.DateTimeUtils2.setCurrentMillisSystem
import java.util.TimeZone

class PermaSqlTest {
    private fun withTZ(id: String, block: () -> Unit) {
        val default = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(default)
        }
    }

    private fun freezeAt(dateTime: DateTime, block: () -> Unit) {
        try {
            setCurrentMillisFixed(dateTime.millis)
            block()
        } finally {
            setCurrentMillisSystem()
        }
    }

    @Test
    fun endOfDayTomorrowIsTheNextDayAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 12, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_TOMORROW)
                )
            }
        }
    }

    @Test
    fun endOfDayDayAfterIsTwoDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 13, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_DAY_AFTER)
                )
            }
        }
    }

    @Test
    fun endOfDayNextWeekIsSevenDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 3, 18, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_NEXT_WEEK)
                )
            }
        }
    }

    @Test
    fun endOfDayNextMonthIsThirtyDaysAcrossSpringForward() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    DateTime(2023, 4, 10, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_NEXT_MONTH)
                )
            }
        }
    }

    @Test
    fun endOfDayYesterdayIsThePreviousDayAcrossFallBack() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 11, 5, 0, 30)) {
                assertEquals(
                    DateTime(2023, 11, 4, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_YESTERDAY)
                )
            }
        }
    }

    @Test
    fun newTaskInTomorrowFilterGetsTheSameDueDateAsTheDatePicker() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 11, 23, 30)) {
                assertEquals(
                    createDueDate(Task.URGENCY_TOMORROW, 0).toString(),
                    PermaSql.replacePlaceholdersForNewTask(PermaSql.VALUE_EOD_TOMORROW)
                )
                assertEquals(
                    DateTime(2023, 3, 12, 12, 0).millis.toString(),
                    PermaSql.replacePlaceholdersForNewTask(PermaSql.VALUE_EOD_TOMORROW)
                )
            }
        }
    }
}
