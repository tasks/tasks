package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import kotlinx.datetime.TimeZone
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
    fun ambiguousWallClockTimeAtFallBackTakesTheDstInstant() {
        withTZ("America/Chicago") {
            val repeatedHalfPastOne = DateTime(2017, 11, 5, 1, 30)
            assertEquals(DateTime(2017, 11, 5, 6, 30, timeZone = DateTime.UTC).millis, repeatedHalfPastOne.millis)
            assertEquals(TimeUnit.HOURS.toMillis(-5), repeatedHalfPastOne.offset)
        }
    }

    @Test
    fun plusDaysIntoTheSpringForwardGapShiftsForward() {
        withTZ("America/Chicago") {
            assertEquals(DateTime(2017, 3, 12, 3, 30), DateTime(2017, 3, 11, 2, 30).plusDays(1))
        }
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
            val berlinTz = TimeZone.of("Europe/Berlin") // UTC+1
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
        val berlinDateTime = DateTime(instant.millis, TimeZone.of("Europe/Berlin")) // UTC+1
        assertEquals(12 * 3600000, instant.millisOfDay)
        assertEquals(13 * 3600000, berlinDateTime.millisOfDay)
    }

    @Test
    fun defaultConstructorIsNow() {
        Freeze.freezeAt(DateTime(2015, 10, 6, 16, 15, 27, 123)) {
            assertEquals(DateTime(2015, 10, 6, 16, 15, 27, 123), DateTime())
        }
    }

    @Test
    fun timestampConstructorPreservesMillis() {
        assertEquals(1444166127123L, DateTime(1444166127123L).millis)
        assertEquals(0L, DateTime(0).millis)
        assertEquals(-1L, DateTime(-1).millis)
    }

    @Test
    fun fieldConstructorDefaultsTimeToMidnight() {
        withTZ("America/Chicago") {
            val dateTime = DateTime(2015, 10, 6)
            assertEquals(0, dateTime.hourOfDay)
            assertEquals(0, dateTime.minuteOfHour)
            assertEquals(0, dateTime.secondOfMinute)
            assertEquals(0, dateTime.millisOfDay)
            assertEquals(DateTime(2015, 10, 6, 5, 0, timeZone = DateTime.UTC).millis, dateTime.millis)
        }
    }

    @Test
    fun fieldConstructorUsesGivenTimeZone() {
        val utc = DateTime(2015, 1, 15, 12, 0, timeZone = DateTime.UTC)
        val berlin = DateTime(2015, 1, 15, 12, 0, timeZone = TimeZone.of("Europe/Berlin"))
        val kolkata = DateTime(2015, 1, 15, 12, 0, timeZone = TimeZone.of("Asia/Kolkata"))
        assertEquals(TimeUnit.HOURS.toMillis(1), utc.millis - berlin.millis)
        assertEquals(TimeUnit.MINUTES.toMillis(330), utc.millis - kolkata.millis)
    }

    @Test
    fun epochBoundary() {
        val beforeEpoch = DateTime(1969, 12, 31, 23, 59, 59, 999, DateTime.UTC)
        assertEquals(-1L, beforeEpoch.millis)
        assertEquals(1969, beforeEpoch.year)
        assertEquals(31, beforeEpoch.dayOfMonth)
        assertEquals(23, beforeEpoch.hourOfDay)
        assertEquals(DateTime.MAX_MILLIS_PER_DAY, beforeEpoch.millisOfDay)
        assertEquals(DateTime(1970, 1, 1, timeZone = DateTime.UTC), DateTime(0, DateTime.UTC))
    }

    @Test
    fun constructorRollsOverflowingMonth() {
        assertEquals(DateTime(2016, 1, 1), DateTime(2015, 13, 1))
        assertEquals(DateTime(2014, 12, 1), DateTime(2015, 0, 1))
    }

    @Test
    fun constructorRollsOverflowingDay() {
        assertEquals(DateTime(2015, 3, 2), DateTime(2015, 2, 30))
        assertEquals(DateTime(2014, 12, 31), DateTime(2015, 1, 0))
    }

    @Test
    fun constructorRollsOverflowingTime() {
        assertEquals(DateTime(2015, 1, 2, 0, 0), DateTime(2015, 1, 1, 24, 0))
        assertEquals(DateTime(2015, 1, 1, 1, 15), DateTime(2015, 1, 1, 0, 75))
        assertEquals(DateTime(2015, 1, 1, 0, 0, 1, 500), DateTime(2015, 1, 1, 0, 0, 0, 1500))
        assertEquals(DateTime(2014, 12, 31, 23, 0), DateTime(2015, 1, 1, -1, 0))
    }

    @Test
    fun withDayOfMonthRollsIntoNextMonth() {
        assertEquals(DateTime(2015, 5, 1, 10, 30), DateTime(2015, 4, 15, 10, 30).withDayOfMonth(31))
    }

    @Test
    fun withMonthOfYearRollsOverflowingDay() {
        assertEquals(DateTime(2015, 3, 3, 10, 30), DateTime(2015, 1, 31, 10, 30).withMonthOfYear(2))
    }

    @Test
    fun withHourOfDayRollsIntoNextDay() {
        assertEquals(DateTime(2015, 2, 1, 1, 30), DateTime(2015, 1, 31, 10, 30).withHourOfDay(25))
    }

    @Test
    fun withMinuteOfHourRollsIntoNextHour() {
        assertEquals(DateTime(2015, 1, 31, 11, 0), DateTime(2015, 1, 31, 10, 30).withMinuteOfHour(60))
    }

    @Test
    fun withYearKeepsOtherFields() {
        assertEquals(DateTime(2016, 1, 31, 10, 30, 15, 250), DateTime(2015, 1, 31, 10, 30, 15, 250).withYear(2016))
    }

    @Test
    fun plusMonthsClampsToEndOfShorterMonth() {
        assertEquals(DateTime(2015, 2, 28, 10, 30), DateTime(2015, 1, 31, 10, 30).plusMonths(1))
        assertEquals(DateTime(2016, 2, 29, 10, 30), DateTime(2016, 1, 31, 10, 30).plusMonths(1))
        assertEquals(DateTime(2015, 4, 30, 10, 30), DateTime(2015, 3, 31, 10, 30).plusMonths(1))
        assertEquals(DateTime(2015, 2, 28, 10, 30), DateTime(2015, 3, 31, 10, 30).plusMonths(-1))
    }

    @Test
    fun plusMonthsAcrossYears() {
        assertEquals(DateTime(2016, 1, 31, 10, 30), DateTime(2015, 1, 31, 10, 30).plusMonths(12))
        assertEquals(DateTime(2013, 12, 31, 10, 30), DateTime(2015, 1, 31, 10, 30).plusMonths(-13))
    }

    @Test
    fun plusDaysKeepsWallClockTimeIntoDST() {
        withTZ("America/Chicago") {
            val saturday = DateTime(2015, 3, 7, 9, 0)
            val sunday = saturday.plusDays(1)
            assertEquals(DateTime(2015, 3, 8, 9, 0), sunday)
            assertEquals(TimeUnit.HOURS.toMillis(23), sunday.millis - saturday.millis)
            assertEquals(DateTime(2015, 3, 14, 9, 0), saturday.plusWeeks(1))
            assertEquals(DateTime(2015, 4, 7, 9, 0), saturday.plusMonths(1))
        }
    }

    @Test
    fun plusDaysKeepsWallClockTimeOutOfDST() {
        withTZ("America/Chicago") {
            val saturday = DateTime(2015, 10, 31, 9, 0)
            val sunday = saturday.plusDays(1)
            assertEquals(DateTime(2015, 11, 1, 9, 0), sunday)
            assertEquals(TimeUnit.HOURS.toMillis(25), sunday.millis - saturday.millis)
            assertEquals(saturday, sunday.minusDays(1))
        }
    }

    @Test
    fun plusHoursCountsElapsedTimeAcrossDST() {
        withTZ("America/Chicago") {
            assertEquals(DateTime(2015, 3, 8, 10, 0), DateTime(2015, 3, 7, 9, 0).plusHours(24))
            assertEquals(DateTime(2015, 3, 8, 10, 0), DateTime(2015, 3, 7, 9, 0).plusMinutes(1440))
            assertEquals(DateTime(2015, 11, 1, 8, 0), DateTime(2015, 10, 31, 9, 0).plusHours(24))
        }
    }

    @Test
    fun secondsAndMillisAcrossDST() {
        withTZ("America/Chicago") {
            assertEquals(DateTime(2015, 3, 8, 3, 0, 0), DateTime(2015, 3, 8, 1, 59, 59).plusSeconds(1))
            assertEquals(DateTime(2015, 3, 8, 3, 0, 0), DateTime(2015, 3, 8, 1, 59, 59, 999).plusMillis(1))
            assertEquals(DateTime(2015, 3, 8, 1, 59, 30), DateTime(2015, 3, 8, 3, 0, 0).minusSeconds(30))
        }
    }

    @Test
    fun plusDaysZeroAndNegative() {
        val dateTime = DateTime(2015, 1, 2, 3, 4, 5, 6)
        assertEquals(dateTime, dateTime.plusDays(0))
        assertEquals(DateTime(2014, 12, 31, 3, 4, 5, 6), dateTime.plusDays(-2))
        assertEquals(DateTime(2014, 12, 19, 3, 4, 5, 6), dateTime.plusWeeks(-2))
    }

    @Test
    fun offsetFollowsDST() {
        withTZ("America/Chicago") {
            assertEquals(TimeUnit.HOURS.toMillis(-6), DateTime(2015, 1, 15, 12, 0).offset)
            assertEquals(TimeUnit.HOURS.toMillis(-5), DateTime(2015, 7, 15, 12, 0).offset)
            assertEquals(TimeUnit.HOURS.toMillis(-6), DateTime(2015, 3, 8, 1, 59, 59, 999).offset)
            assertEquals(TimeUnit.HOURS.toMillis(-5), DateTime(2015, 3, 8, 3, 0).offset)
        }
    }

    @Test
    fun offsetUsesOwnTimeZone() {
        withTZ("America/Chicago") {
            assertEquals(0L, DateTime(2015, 1, 15, 12, 0, timeZone = DateTime.UTC).offset)
            assertEquals(
                TimeUnit.MINUTES.toMillis(330),
                DateTime(2015, 1, 15, 12, 0, timeZone = TimeZone.of("Asia/Kolkata")).offset
            )
        }
    }

    @Test
    fun fieldsAtEndOfYear() {
        val dateTime = DateTime(2015, 12, 31, 23, 59, 59, 999)
        assertEquals(2015, dateTime.year)
        assertEquals(12, dateTime.monthOfYear)
        assertEquals(31, dateTime.dayOfMonth)
        assertEquals(5, dateTime.dayOfWeek)
        assertEquals(23, dateTime.hourOfDay)
        assertEquals(59, dateTime.minuteOfHour)
        assertEquals(59, dateTime.secondOfMinute)
        assertEquals(DateTime.MAX_MILLIS_PER_DAY, dateTime.millisOfDay)
        assertEquals(DateTime(2016, 1, 1, 0, 0, 0, 0), dateTime.plusMillis(1))
    }

    @Test
    fun fieldsInHalfHourZone() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        val dateTime = DateTime(DateTime(2015, 1, 15, 12, 0, timeZone = DateTime.UTC).millis, kolkata)
        assertEquals(17, dateTime.hourOfDay)
        assertEquals(30, dateTime.minuteOfHour)
        assertEquals(DateTime(2015, 1, 15, timeZone = kolkata), dateTime.startOfDay())
    }

    @Test
    fun dayOfWeekCountsFromSundayAsOne() {
        val sunday = DateTime(2024, 12, 22)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), (0..6).map { sunday.plusDays(it).dayOfWeek })
    }

    @Test
    fun dayOfWeekInMonthCountsFromOne() {
        assertEquals(listOf(1, 1, 1, 1, 1, 1, 1, 2), (1..8).map { DateTime(2024, 12, it).dayOfWeekInMonth })
        assertEquals(5, DateTime(2024, 12, 29).dayOfWeekInMonth)
        assertEquals(5, DateTime(2024, 12, 31).dayOfWeekInMonth)
        assertEquals(5, DateTime(2024, 2, 29).dayOfWeekInMonth)
    }

    @Test
    fun weekdaysOfTheDaysPastFourWeeksOccurFiveTimes() {
        fun firstWeek(year: Int, month: Int) = (1..7).map { DateTime(year, month, it).maxDayOfWeekInMonth }
        assertEquals(listOf(5, 5, 5, 4, 4, 4, 4), firstWeek(2024, 12))
        assertEquals(listOf(5, 5, 4, 4, 4, 4, 4), firstWeek(2024, 9))
        assertEquals(listOf(5, 4, 4, 4, 4, 4, 4), firstWeek(2024, 2))
        assertEquals(listOf(4, 4, 4, 4, 4, 4, 4), firstWeek(2025, 2))
        assertEquals(5, DateTime(2024, 12, 31).maxDayOfWeekInMonth)
        assertEquals(4, DateTime(2025, 2, 14).maxDayOfWeekInMonth)
    }

    @Test
    fun numberOfDaysInFebruary() {
        assertEquals(29, DateTime(2024, 2, 10).numberOfDaysInMonth)
        assertEquals(29, DateTime(2000, 2, 10).numberOfDaysInMonth)
        assertEquals(28, DateTime(2100, 2, 10).numberOfDaysInMonth)
        assertEquals(28, DateTime(2023, 2, 10).numberOfDaysInMonth)
    }

    @Test
    fun startOfSecond() {
        assertEquals(DateTime(2015, 10, 6, 2, 0, 48, 0), DateTime(2015, 10, 6, 2, 0, 48, 412).startOfSecond())
    }

    @Test
    fun startOfDayKeepsTimeZone() {
        val kolkata = TimeZone.of("Asia/Kolkata")
        assertEquals(kolkata, DateTime(2015, 1, 15, 12, 0, timeZone = kolkata).startOfDay().timeZone)
        assertEquals(kolkata, DateTime(2015, 1, 15, 12, 0, timeZone = kolkata).plusDays(1).timeZone)
        assertEquals(kolkata, DateTime(2015, 1, 15, 12, 0, timeZone = kolkata).plusHours(1).timeZone)
        assertEquals(kolkata, DateTime(2015, 1, 15, 12, 0, timeZone = kolkata).withHourOfDay(1).timeZone)
    }

    @Test
    fun withMillisOfDayBounds() {
        val dateTime = DateTime(2015, 10, 6, 2, 0, 48, 412)
        assertEquals(DateTime(2015, 10, 6), dateTime.withMillisOfDay(0))
        assertEquals(DateTime(2015, 10, 6, 23, 59, 59, 999), dateTime.withMillisOfDay(DateTime.MAX_MILLIS_PER_DAY))
    }

    @Test
    fun maxMillisPerDay() {
        assertEquals(86399999, DateTime.MAX_MILLIS_PER_DAY)
    }

    @Test
    fun comparisonsAreStrict() {
        val a = DateTime(2015, 10, 6, 16, 15, 27)
        val b = DateTime(2015, 10, 6, 16, 15, 27)
        assertFalse(a.isAfter(b))
        assertFalse(a.isBefore(b))
        Freeze.freezeAt(a) {
            assertFalse(a.isAfterNow)
            assertFalse(a.isBeforeNow)
            assertTrue(a.plusMillis(1).isAfterNow)
            assertTrue(a.minusMillis(1).isBeforeNow)
        }
    }

    @Test
    fun comparisonsIgnoreTimeZone() {
        val utc = DateTime(2015, 10, 6, 16, 15, 27, timeZone = DateTime.UTC)
        val berlin = DateTime(utc.millis, TimeZone.of("Europe/Berlin"))
        assertFalse(utc.isAfter(berlin))
        assertFalse(berlin.isAfter(utc))
        assertTrue(berlin.plusMillis(1).isAfter(utc))
    }

    @Test
    fun equalityIncludesTimeZone() {
        val utc = DateTime(2015, 10, 6, 16, 15, 27, timeZone = DateTime.UTC)
        val sameInstantInBerlin = DateTime(utc.millis, TimeZone.of("Europe/Berlin"))
        assertEquals(utc, DateTime(utc.millis, DateTime.UTC))
        assertEquals(utc.hashCode(), DateTime(utc.millis, DateTime.UTC).hashCode())
        assertNotEquals(utc, sameInstantInBerlin)
        assertNotEquals(utc, utc.plusMillis(1))
        assertNotEquals(utc, null)
        assertNotEquals(utc, utc.millis)
    }

    @Test
    fun toUTCAndToLocalKeepTheInstant() {
        withTZ("America/Chicago") {
            val local = DateTime(2015, 7, 6, 14, 0, 48, 412)
            val utc = local.toUTC()
            assertEquals(local.millis, utc.millis)
            assertEquals(19, utc.hourOfDay)
            assertEquals(DateTime.UTC, utc.timeZone)
            assertEquals(local, utc.toLocal())
        }
    }

    @Test
    fun defaultToStringIncludesOffset() {
        withTZ("America/Chicago") {
            assertEquals("2015-01-06T02:00:48.005-0600", DateTime(2015, 1, 6, 2, 0, 48, 5).toString())
            assertEquals("2015-07-06T14:00:48.412-0500", DateTime(2015, 7, 6, 14, 0, 48, 412).toString())
            assertEquals(
                "2015-07-06T14:00:48.412+0000",
                DateTime(2015, 7, 6, 14, 0, 48, 412, DateTime.UTC).toString()
            )
            assertEquals(
                "2015-01-15T12:00:00.000+0530",
                DateTime(2015, 1, 15, 12, 0, timeZone = TimeZone.of("Asia/Kolkata")).toString()
            )
        }
    }

    @Test
    fun toStringWithPattern() {
        val dateTime = DateTime(2015, 1, 6, 2, 0, 48, 5)
        assertEquals("20150106T0200", dateTime.toString("yyyyMMdd'T'HHmm"))
        assertEquals("201501060200", dateTime.toString("yyyyMMddHHmm"))
    }

    @Test
    fun toStringKeepsLiteralDigitsInThePattern() {
        assertEquals("2023-07-21T00:00:00.0000000", DateTime(2023, 7, 21).toString("yyyy-MM-dd'T'HH:mm:ss.SSS0000"))
    }

    @Test
    fun appleEpoch() {
        assertEquals(0L, DateTime(2001, 1, 1, timeZone = DateTime.UTC).toAppleEpoch())
        assertEquals(TimeUnit.DAYS.toSeconds(1), DateTime(2001, 1, 2, timeZone = DateTime.UTC).toAppleEpoch())
    }
}
