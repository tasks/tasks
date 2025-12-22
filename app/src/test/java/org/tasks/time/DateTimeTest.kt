package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DateTimeTest {
    @Test
    fun testGetMillisOfDay() {
        assertEquals(7248412, DateTime(2015, 10, 6, 2, 0, 48, 412).millisOfDay)
    }

    @Test
    fun testWithMillisOfDay() {
        assertEquals(
                DateTime(2015, 10, 6, 2, 0, 48, 412),
                DateTime(2015, 10, 6, 0, 0, 0, 0).withMillisOfDay(7248412))
    }

    @Test
    fun testWithMillisOfDayDuringDST() {
        withTZ("America/Chicago") {
            assertEquals(
                    2,
                    DateTime(2015, 10, 31, 2, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(2).toInt())
                            .hourOfDay)
        }
    }

    @Test
    fun testWithMillisOfDayAfterDST() {
        withTZ("America/Chicago") {
            assertEquals(
                    2,
                    DateTime(2015, 11, 2, 2, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(2).toInt())
                            .hourOfDay)
        }
    }

    @Test
    fun testWithMillisOfDayStartDST() {
        withTZ("America/Chicago") {
            assertEquals(
                    1,
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(1).toInt())
                            .hourOfDay)
            assertEquals(
                    3,
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(2).toInt())
                            .hourOfDay)
            assertEquals(
                    3,
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(3).toInt())
                            .hourOfDay)
            assertEquals(
                    4,
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(4).toInt())
                            .hourOfDay)
            assertEquals(
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(2).toInt())
                            .millis,
                    DateTime(2015, 3, 8, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(3).toInt())
                            .millis)
        }
    }

    @Test
    fun testWithMillisOfDayEndDST() {
        withTZ("America/Chicago") {
            assertEquals(
                    1,
                    DateTime(2015, 11, 1, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(1).toInt())
                            .hourOfDay)
            assertEquals(
                    2,
                    DateTime(2015, 11, 1, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(2).toInt())
                            .hourOfDay)
            assertEquals(
                    3,
                    DateTime(2015, 11, 1, 0, 0, 0)
                            .withMillisOfDay(TimeUnit.HOURS.toMillis(3).toInt())
                            .hourOfDay)
        }
    }

    @Test
    fun testPlusMonths() {
        assertEquals(
                DateTime(2015, 11, 6, 2, 0, 48, 412),
                DateTime(2015, 10, 6, 2, 0, 48, 412).plusMonths(1))
    }

    @Test
    fun testPlusMonthsWrapYear() {
        assertEquals(
                DateTime(2016, 1, 6, 2, 0, 48, 412),
                DateTime(2015, 10, 6, 2, 0, 48, 412).plusMonths(3))
    }

    @Test
    fun testGetDayOfMonth() {
        assertEquals(5, DateTime(2015, 10, 5, 0, 0, 0).dayOfMonth)
    }

    @Test
    fun testPlusDays() {
        assertEquals(
                DateTime(2015, 10, 6, 2, 0, 48, 412),
                DateTime(2015, 10, 5, 2, 0, 48, 412).plusDays(1))
    }

    @Test
    fun testPlusDaysWrapMonth() {
        assertEquals(
                DateTime(2015, 11, 1, 2, 0, 48, 412),
                DateTime(2015, 10, 31, 2, 0, 48, 412).plusDays(1))
    }

    @Test
    fun testMinuteOfHour() {
        assertEquals(43, DateTime(2015, 10, 5, 2, 43, 48).minuteOfHour)
    }

    @Test
    fun testIsEndOfMonth() {
        assertTrue(DateTime(2014, 1, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 2, 28, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 3, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 4, 30, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 5, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 6, 30, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 7, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 8, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 9, 30, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 10, 31, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 11, 30, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2014, 12, 31, 0, 0, 0).isLastDayOfMonth)
    }

    @Test
    fun testNotTheEndOfTheMonth() {
        for (month in 1..12) {
            val lastDay = DateTime(2014, month, 1, 0, 0, 0, 0).numberOfDaysInMonth
            for (day in 1 until lastDay) {
                assertFalse(DateTime(2014, month, day, 0, 0, 0).isLastDayOfMonth)
            }
        }
    }

    @Test
    fun testCheckEndOfMonthDuringLeapYear() {
        assertFalse(DateTime(2016, 2, 28, 0, 0, 0).isLastDayOfMonth)
        assertTrue(DateTime(2016, 2, 29, 0, 0, 0).isLastDayOfMonth)
    }

    @Test
    fun testNumberOfDaysInMonth() {
        assertEquals(31, DateTime(2015, 1, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(28, DateTime(2015, 2, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 3, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(30, DateTime(2015, 4, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 5, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(30, DateTime(2015, 6, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 7, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 8, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(30, DateTime(2015, 9, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 10, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(30, DateTime(2015, 11, 5, 9, 45, 34).numberOfDaysInMonth)
        assertEquals(31, DateTime(2015, 12, 5, 9, 45, 34).numberOfDaysInMonth)
    }

    @Test
    fun testWithMillisOfSecond() {
        assertEquals(
                DateTime(2015, 11, 6, 13, 34, 56, 453),
                DateTime(2015, 11, 6, 13, 34, 56, 0).withMillisOfSecond(453))
    }

    @Test
    fun testWithHourOfDay() {
        assertEquals(
                DateTime(2015, 11, 6, 23, 0, 0), DateTime(2015, 11, 6, 1, 0, 0).withHourOfDay(23))
    }

    @Test
    fun testWithMinuteOfHour() {
        assertEquals(
                DateTime(2015, 11, 6, 23, 13, 0),
                DateTime(2015, 11, 6, 23, 1, 0).withMinuteOfHour(13))
    }

    @Test
    fun testWithSecondOfMinute() {
        assertEquals(
                DateTime(2015, 11, 6, 23, 13, 56),
                DateTime(2015, 11, 6, 23, 13, 1).withSecondOfMinute(56))
    }

    @Test
    fun testGetYear() {
        assertEquals(2015, DateTime(2015, 1, 1, 1, 1, 1).year)
    }

    @Test
    fun testMinusMinutes() {
        assertEquals(
            DateTime(2015, 11, 4, 23, 59, 0, timeZone = DateTime.UTC),
            DateTime(2015, 11, 5, 0, 1, 0, timeZone = DateTime.UTC).minusMinutes(2)
        )
    }

    @Test
    fun testIsBefore() {
        assertTrue(DateTime(2015, 11, 4, 23, 59, 0).isBefore(DateTime(2015, 11, 4, 23, 59, 1)))
        assertFalse(
                DateTime(2015, 11, 4, 23, 59, 0).isBefore(DateTime(2015, 11, 4, 23, 59, 0)))
    }

    @Test
    fun testGetMonthOfYear() {
        assertEquals(1, DateTime(2015, 1, 2, 3, 4, 5).monthOfYear)
    }

    @Test
    fun testIsAfter() {
        assertTrue(DateTime(2015, 11, 4, 23, 59, 1).isAfter(DateTime(2015, 11, 4, 23, 59, 0)))
        assertFalse(DateTime(2015, 11, 4, 23, 59, 0).isAfter(DateTime(2015, 11, 4, 23, 59, 0)))
    }

    @Test
    fun testWithYear() {
        assertEquals(
                DateTime(2016, 1, 1, 1, 1, 1), DateTime(2015, 1, 1, 1, 1, 1).withYear(2016))
    }

    @Test
    fun testWithMonthOfYear() {
        assertEquals(
                DateTime(2015, 1, 2, 3, 4, 5), DateTime(2015, 2, 2, 3, 4, 5).withMonthOfYear(1))
    }

    @Test
    fun testGetHourOfDay() {
        assertEquals(3, DateTime(2015, 1, 2, 3, 4, 5).hourOfDay)
    }

    @Test
    fun testWithDayOfMonth() {
        assertEquals(
                DateTime(2015, 1, 2, 3, 4, 5), DateTime(2015, 1, 1, 3, 4, 5).withDayOfMonth(2))
    }

    @Test
    fun testPlusMinutes() {
        assertEquals(
                DateTime(2015, 1, 2, 3, 4, 5), DateTime(2015, 1, 2, 2, 59, 5).plusMinutes(5))
    }

    @Test
    fun testPlusHours() {
        assertEquals(
                DateTime(2015, 1, 2, 3, 4, 5), DateTime(2015, 1, 1, 3, 4, 5).plusHours(24))
    }

    @Test
    fun testPlusWeeks() {
        assertEquals(
                DateTime(2015, 1, 2, 3, 4, 5), DateTime(2014, 12, 12, 3, 4, 5).plusWeeks(3))
    }

    @Test
    fun testIsBeforeNow() {
        Freeze.freezeAt(DateTime(2015, 10, 6, 16, 15, 27)) {
            assertFalse(DateTime(2015, 10, 6, 16, 15, 27).isBeforeNow)
            assertTrue(DateTime(2015, 10, 6, 16, 15, 26).isBeforeNow)
        }
    }

    @Test
    fun testMinusMillis() {
        assertEquals(
            DateTime(2015, 11, 6, 16, 18, 20, 452, DateTime.UTC),
            DateTime(2015, 11, 6, 16, 18, 21, 374, DateTime.UTC).minusMillis(922)
        )
    }

    @Test
    fun testMinusDays() {
        assertEquals(
            DateTime(2015, 11, 6, 16, 19, 16, timeZone = DateTime.UTC),
            DateTime(2015, 12, 4, 16, 19, 16, timeZone = DateTime.UTC).minusDays(28)
        )
        assertEquals(
            DateTime(2015, 11, 6, 16, 19, 16, timeZone = DateTime.UTC),
            DateTime(2015, 11, 7, 16, 19, 16, timeZone = DateTime.UTC).minusDays(1)
        )
    }

    @Test
    fun testGetSecondOfMinute() {
        assertEquals(32, DateTime(2015, 11, 6, 16, 19, 32).secondOfMinute)
    }

    @Test
    fun toUTC() {
        withTZ("America/Chicago") {
            assertEquals(
                    DateTime(2015, 10, 6, 14, 45, 15, 0, DateTime.UTC),
                    DateTime(2015, 10, 6, 9, 45, 15).toUTC())
        }
    }

    @Test
    fun fromUTC() {
        withTZ("America/Chicago") {
            assertEquals(
                    DateTime(2021, 1, 27, 10, 56, 15, 423),
                    DateTime(2021, 1, 27, 16, 56, 15, 423, DateTime.UTC).toLocal()
            )
        }
    }

    @Test
    fun dontAdjustLocal() {
        assertEquals(
                DateTime(2021, 1, 27, 10, 56, 15, 423),
                DateTime(2021, 1, 27, 10, 56, 15, 423).toLocal()
        )
    }

    @Test
    fun dontAdjustUTC() {
        assertEquals(
                DateTime(2021, 1, 27, 16, 56, 15, 423, DateTime.UTC),
                DateTime(2021, 1, 27, 16, 56, 15, 423, DateTime.UTC).toUTC()
        )
    }

    @Test
    fun testStartOfMinute() {
        assertEquals(
            DateTime(2017, 9, 3, 0, 51, 0, 0, DateTime.UTC),
            DateTime(2017, 9, 3, 0, 51, 13, 427, DateTime.UTC).startOfMinute()
        )
    }

    @Test
    fun testEndOfMinute() {
        assertEquals(
            DateTime(2017, 9, 22, 14, 47, 59, 999, DateTime.UTC),
            DateTime(2017, 9, 22, 14, 47, 14, 453, DateTime.UTC).endOfMinute()
        )
    }

    @Test
    fun startOfDayPreservesTimezone() {
        val utcDateTime = DateTime(2024, 12, 20, 14, 30, timeZone = DateTime.UTC)
        val result = utcDateTime.startOfDay()
        assertEquals(DateTime(2024, 12, 20, timeZone = DateTime.UTC), result)
    }

    @Test
    fun startOfDayInDefaultTimezone() {
        val dateTime = DateTime(2024, 12, 20, 14, 30)
        val result = dateTime.startOfDay()
        assertEquals(DateTime(2024, 12, 20), result)
    }

    @Test
    fun startOfDayWithUTCTimezone() {
        withTZ("America/Chicago") { // UTC-6
            val utcDateTime = DateTime(2024, 12, 20, 15, timeZone = DateTime.UTC)
            val result = utcDateTime.startOfDay()
            assertEquals(DateTime(2024, 12, 20, timeZone = DateTime.UTC), result)
        }
    }

    @Test
    fun startOfDayBeforeUTC() {
        withTZ("America/New_York") { // UTC-5
            val nyDateTime = DateTime(2024, 12, 20, 15)
            val result = nyDateTime.startOfDay()
            assertEquals(DateTime(2024, 12, 20), result)
        }
    }

    @Test
    fun startOfDayAfterUTC() {
        withTZ("Europe/Berlin") { // UTC+1
            val berlinDateTime = DateTime(2024, 12, 20, 15)
            val result = berlinDateTime.startOfDay()
            assertEquals(DateTime(2024, 12, 20), result)
        }
    }

    @Test
    fun startOfDayWithDateBoundaryWrap() {
        withTZ("Pacific/Auckland") { // UTC+13
            val aucklandDateTime = DateTime(2024, 12, 20, 12)
            val result = aucklandDateTime.startOfDay()
            assertEquals(DateTime(2024, 12, 20), result)
        }
    }

    @Test
    fun startOfDayRespectsTimezoneNotSystemDefault() {
        withTZ("America/New_York") { // UTC-5
            val berlinTz = TimeZone.getTimeZone("Europe/Berlin") // UTC+1
            val berlinDateTime = DateTime(2024, 12, 20, 1, timeZone = berlinTz)
            val result = berlinDateTime.startOfDay()
            assertEquals(DateTime(2024, 12, 20, timeZone = berlinTz), result)
        }
    }

    @Test
    fun startOfDayWithExplicitTimezone() {
        withTZ("Europe/Berlin") { // UTC+1
            val localMidnight = DateTime(2024, 12, 20)
            val utcMidnight = localMidnight.startOfDay(DateTime.UTC)
            assertEquals(DateTime(2024, 12, 20, timeZone = DateTime.UTC), utcMidnight)
        }
    }

    @Test
    fun startOfDayWithExplicitTimezoneFromAuckland() {
        withTZ("Pacific/Auckland") { // UTC+13
            val localMidnight = DateTime(2024, 12, 20)
            val utcMidnight = localMidnight.startOfDay(DateTime.UTC)
            assertEquals(DateTime(2024, 12, 20, timeZone = DateTime.UTC), utcMidnight)
        }
    }

    @Test
    fun startOfDayWithExplicitTimezoneFromHonolulu() {
        withTZ("Pacific/Honolulu") { // UTC-10
            val localMidnight = DateTime(2024, 12, 20)
            val utcMidnight = localMidnight.startOfDay(DateTime.UTC)
            assertEquals(DateTime(2024, 12, 20, timeZone = DateTime.UTC), utcMidnight)
        }
    }

    @Test
    fun noon() {
        assertEquals(
            DateTime(2024, 12, 20, 12, timeZone = DateTime.UTC),
            DateTime(2024, 12, 20, 8, 30, 45, 123, DateTime.UTC).noon()
        )
    }

    @Test
    fun endOfDay() {
        assertEquals(
            DateTime(2024, 12, 20, 23, 59, 59, timeZone = DateTime.UTC),
            DateTime(2024, 12, 20, 8, 30, 45, 123, DateTime.UTC).endOfDay()
        )
    }

    @Test
    fun millisOfDayRespectsTimezone() {
        val instant = DateTime(2024, 12, 20, 12, timeZone = DateTime.UTC)
        val berlinDateTime = DateTime(instant.millis, TimeZone.getTimeZone("Europe/Berlin")) // UTC+1
        assertEquals(12 * 3600000, instant.millisOfDay)
        assertEquals(13 * 3600000, berlinDateTime.millisOfDay)
    }
}
