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

    @Test
    fun endOfDayIsTheLastInstantWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 2025-03-30 00:00, so 23:00-23:59 never happens
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    lastInstantOf(2025, 3, 29).toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD)
                )
            }
        }
    }

    @Test
    fun endOfDayYesterdayEndsYesterdayWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 3, 28, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_YESTERDAY)
                )
            }
        }
    }

    @Test
    fun endOfDayTomorrowEndsTomorrowWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 3, 30, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_TOMORROW)
                )
            }
        }
    }

    @Test
    fun endOfDayDayAfterEndsTheDayAfterWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 3, 31, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_DAY_AFTER)
                )
            }
        }
    }

    @Test
    fun endOfDayNextWeekEndsThatDayWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 4, 5, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_NEXT_WEEK)
                )
            }
        }
    }

    @Test
    fun endOfDayNextMonthEndsThatDayWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 4, 28, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_NEXT_MONTH)
                )
            }
        }
    }

    @Test
    fun endOfDayTomorrowEndsTomorrowWhenTodayEndsWithARepeatedHour() {
        withTZ("Africa/Cairo") {
            // 2025-10-31 00:00 -> 2025-10-30 23:00, so 23:00-23:59 happens twice
            freezeAt(DateTime(2025, 10, 30, 12, 0)) {
                assertEquals(
                    DateTime(2025, 10, 31, 23, 59, 59).millis.toString(),
                    PermaSql.replacePlaceholdersForQuery(PermaSql.VALUE_EOD_TOMORROW)
                )
            }
        }
    }

    @Test
    fun newTaskTomorrowIsStillNoonWhenTodaysLastHourDoesNotExist() {
        withTZ("America/Nuuk") {
            freezeAt(DateTime(2025, 3, 29, 12, 0)) {
                assertEquals(
                    DateTime(2025, 3, 30, 12, 0).millis.toString(),
                    PermaSql.replacePlaceholdersForNewTask(PermaSql.VALUE_EOD_TOMORROW)
                )
            }
        }
    }

    private fun lastInstantOf(year: Int, month: Int, day: Int) =
        DateTime(year, month, day).plusDays(1).startOfDay().millis - 1
}
