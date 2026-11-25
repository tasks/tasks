package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class LongExtensionsJvmTest {
    private fun withTZ(id: String, block: () -> Unit) {
        val default = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(default)
        }
    }

    @Test
    fun twentyThreeHourDayIsStillOneDay() {
        withTZ("America/Chicago") {
            assertEquals(
                1,
                DateTime(2023, 3, 11, 9, 0).millis.daysUntil(DateTime(2023, 3, 12, 9, 0).millis)
            )
        }
    }

    @Test
    fun twentyFiveHourDayIsStillOneDay() {
        withTZ("America/Chicago") {
            assertEquals(
                1,
                DateTime(2023, 11, 4, 9, 0).millis.daysUntil(DateTime(2023, 11, 5, 9, 0).millis)
            )
        }
    }

    @Test
    fun plusDaysKeepsTheClockTimeAcrossSpringForward() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 3, 12, 9, 0).millis,
                DateTime(2023, 3, 11, 9, 0).millis.plusDays(1)
            )
        }
    }

    @Test
    fun plusDaysKeepsTheClockTimeAcrossFallBack() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 11, 5, 9, 0).millis,
                DateTime(2023, 11, 4, 9, 0).millis.plusDays(1)
            )
        }
    }

    @Test
    fun minusDaysStaysOnTheDayBeforeWhenTheClockTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            assertEquals(
                lastInstantOf(2025, 3, 29),
                DateTime(2025, 3, 30, 23, 30).millis.minusDays(1)
            )
        }
    }

    @Test
    fun minusDaysStaysAWeekBackWhenTheClockTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            assertEquals(
                lastInstantOf(2025, 3, 29),
                DateTime(2025, 4, 5, 23, 30).millis.minusDays(7)
            )
        }
    }

    @Test
    fun plusDaysStaysOnTheNextDayWhenTheClockTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            assertEquals(
                lastInstantOf(2025, 3, 29),
                DateTime(2025, 3, 28, 23, 30).millis.plusDays(1)
            )
        }
    }

    @Test
    fun withMillisOfDayStaysOnTheSameDayWhenTheClockTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            assertEquals(
                lastInstantOf(2025, 3, 29),
                DateTime(2025, 3, 29, 12, 0).millis.withMillisOfDay((23 * 60 + 30) * 60 * 1000)
            )
        }
    }

    @Test
    fun endOfDayStaysOnTheSameDayWhenTheLastSecondDoesNotExist() {
        withTZ("America/Nuuk") {
            assertEquals(
                lastInstantOf(2025, 3, 29),
                DateTime(2025, 3, 29, 12, 0).millis.endOfDay()
            )
        }
    }

    @Test
    fun endOfDayIsTheLastSecondOfAnOrdinaryDay() {
        withTZ("America/Nuuk") {
            assertEquals(
                DateTime(2025, 3, 27, 23, 59, 59).millis,
                DateTime(2025, 3, 27, 12, 0).millis.endOfDay()
            )
        }
    }

    @Test
    fun startOfDayStaysOnTheSameDayWhenMidnightDoesNotExist() {
        withTZ("America/Santiago") {
            // 2024-09-08 00:00 -> 01:00 does not exist in Santiago
            assertEquals(
                DateTime(2024, 9, 8, 1, 0).millis,
                DateTime(2024, 9, 8, 12, 0).millis.startOfDay()
            )
        }
    }

    @Test
    fun plusDaysAdvancesTheDateWhenTheWholeDayDoesNotExist() {
        withTZ("Pacific/Apia") {
            // 2011-12-30 did not exist in Apia
            assertEquals(
                DateTime(2011, 12, 31, 10, 0).millis,
                DateTime(2011, 12, 29, 10, 0).millis.plusDays(1)
            )
        }
    }

    @Test
    fun endOfDayIsTheLastInstantWhenTheClockGoesBackAtMidnight() {
        withTZ("Africa/Cairo") {
            // 2025-10-31 00:00 -> 2025-10-30 23:00, so 23:00-23:59 happens twice
            assertEquals(
                lastInstantOf(2025, 10, 30),
                DateTime(2025, 10, 30, 12, 0).millis.endOfDay()
            )
        }
    }

    @Test
    fun endOfDayIsTheLastSecondOfAnOrdinaryDayInAZoneThatEndsDstAtMidnight() {
        withTZ("Africa/Cairo") {
            assertEquals(
                DateTime(2025, 10, 29, 23, 59, 59).millis,
                DateTime(2025, 10, 29, 12, 0).millis.endOfDay()
            )
        }
    }

    @Test
    fun plusZeroDaysKeepsTheInstantInsideTheRepeatedHour() {
        withTZ("America/Chicago") {
            // 2023-11-05 01:00 -> 02:00 happens twice in Chicago
            val secondOneAm = DateTime(2023, 11, 5, 1, 0).millis + ONE_HOUR
            assertEquals(secondOneAm, secondOneAm.plusDays(0))
        }
    }

    @Test
    fun minusZeroDaysKeepsTheInstantInsideTheRepeatedHour() {
        withTZ("America/Chicago") {
            val secondOneAm = DateTime(2023, 11, 5, 1, 0).millis + ONE_HOUR
            assertEquals(secondOneAm, secondOneAm.minusDays(0))
        }
    }

    @Test
    fun shiftKeepsTheClockTimeWhenTheDueDateMovesWholeDays() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 3, 11, 9, 0).millis,
                DateTime(2023, 3, 10, 9, 0).millis.shiftFollowingDueDate(
                    DateTime(2023, 3, 11, 9, 0).millis,
                    DateTime(2023, 3, 12, 9, 0).millis,
                )
            )
        }
    }

    @Test
    fun shiftMovesByElapsedTimeWhenTheDueDateMovesWithinTheDay() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 9, 55).millis,
                DateTime(2023, 7, 19, 8, 55).millis.shiftFollowingDueDate(
                    DateTime(2023, 7, 19, 9, 0).millis,
                    DateTime(2023, 7, 19, 10, 0).millis,
                )
            )
        }
    }

    @Test
    fun shiftMovesByElapsedTimeWhenTheDueDateCrossesMidnight() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 20, 0, 25).millis,
                DateTime(2023, 7, 19, 23, 25).millis.shiftFollowingDueDate(
                    DateTime(2023, 7, 19, 23, 30).millis,
                    DateTime(2023, 7, 20, 0, 30).millis,
                )
            )
        }
    }

    @Test
    fun shiftAdvancesAnHourWhenTheDueDateMovesIntoTheRepeatedHour() {
        withTZ("America/Chicago") {
            val dueBeforeFallBack = DateTime(2023, 11, 5, 1, 30).millis
            val dueAfterFallBack = dueBeforeFallBack + ONE_HOUR
            val alarm = DateTime(2023, 11, 5, 1, 0).millis

            assertEquals(
                alarm + ONE_HOUR,
                alarm.shiftFollowingDueDate(dueBeforeFallBack, dueAfterFallBack)
            )
        }
    }

    @Test
    fun shiftLeavesAnAlarmInTheRepeatedHourAloneWhenTheDueDateDoesNotMove() {
        withTZ("America/Chicago") {
            val due = DateTime(2023, 11, 5, 1, 30).millis
            val alarm = DateTime(2023, 11, 5, 1, 0).millis + ONE_HOUR

            assertEquals(alarm, alarm.shiftFollowingDueDate(due, due))
        }
    }

    private fun lastInstantOf(year: Int, month: Int, day: Int) =
        DateTime(year, month, day).plusDays(1).startOfDay().millis - 1
}
