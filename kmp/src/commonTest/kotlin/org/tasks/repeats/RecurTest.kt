package org.tasks.repeats

import kotlinx.datetime.DayOfWeek
import org.tasks.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RecurTest {

    @Test
    fun keepsRulePartsItDoesNotModel() {
        val recur = Recur.parse("RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD")

        assertEquals(listOf("RSCALE" to "HEBREW", "SKIP" to "FORWARD"), recur.unknownParts)
        assertEquals("RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD", recur.toString())
    }

    @Test
    fun dropsACountOfZero() {
        val recur = Recur.parse("FREQ=DAILY;COUNT=0")

        assertNull(recur.count)
        assertEquals("FREQ=DAILY", recur.toString())
    }

    @Test
    fun parseFrequencyOnly() {
        assertEquals(Recur(Frequency.DAILY), Recur.parse("FREQ=DAILY"))
        assertEquals(Recur(Frequency.SECONDLY), Recur.parse("FREQ=SECONDLY"))
    }

    @Test
    fun parseIntervalCountAndUntil() {
        assertEquals(
            Recur(Frequency.WEEKLY, interval = 2, count = 5),
            Recur.parse("FREQ=WEEKLY;INTERVAL=2;COUNT=5"),
        )
        assertEquals(Recur(Frequency.DAILY, interval = 1), Recur.parse("FREQ=DAILY;INTERVAL=1"))
        assertEquals(
            Recur(Frequency.MONTHLY, until = Until.Date(2026, 2, 28)),
            Recur.parse("FREQ=MONTHLY;UNTIL=20260228"),
        )
        assertEquals(
            Recur(Frequency.DAILY, until = Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59, timeZone = DateTime.UTC).millis, utc = true)),
            Recur.parse("FREQ=DAILY;UNTIL=20260228T235959Z"),
        )
    }

    @Test
    fun parseFloatingUntil() {
        val recur = Recur.parse("FREQ=DAILY;UNTIL=20260228T235959")
        assertEquals(Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59).millis, utc = false), recur.until)
    }

    @Test
    fun parseByDay() {
        assertEquals(
            listOf(ByDay(Weekday.MO), ByDay(Weekday.WE), ByDay(Weekday.FR)),
            Recur.parse("FREQ=WEEKLY;BYDAY=MO,WE,FR").byDay,
        )
        assertEquals(listOf(ByDay(Weekday.FR, -1)), Recur.parse("FREQ=MONTHLY;BYDAY=-1FR").byDay)
        assertEquals(listOf(ByDay(Weekday.TU, 2)), Recur.parse("FREQ=MONTHLY;BYDAY=2TU").byDay)
    }

    @Test
    fun parseNumberLists() {
        assertEquals(listOf(-1), Recur.parse("FREQ=MONTHLY;BYMONTHDAY=-1").byMonthDay)
        assertEquals(listOf(1, 15), Recur.parse("FREQ=MONTHLY;BYMONTHDAY=1,15").byMonthDay)
        assertEquals(listOf(1, 3, 5), Recur.parse("FREQ=MONTHLY;BYMONTH=1,3,5").byMonth)
        val everything = Recur.parse("FREQ=YEARLY;BYSECOND=1;BYMINUTE=2;BYHOUR=3;BYYEARDAY=100;BYWEEKNO=20;BYSETPOS=-1;WKST=MO")
        assertEquals(listOf(1), everything.bySecond)
        assertEquals(listOf(2), everything.byMinute)
        assertEquals(listOf(3), everything.byHour)
        assertEquals(listOf(100), everything.byYearDay)
        assertEquals(listOf(20), everything.byWeekNo)
        assertEquals(listOf(-1), everything.bySetPos)
        assertEquals(Weekday.MO, everything.weekStart)
        assertNull(Recur.parse("FREQ=WEEKLY;BYDAY=MO").weekStart)
    }

    @Test
    fun parseStripsLegacyPrefixAndArtifacts() {
        assertEquals(Recur(Frequency.DAILY), Recur.parse("RRULE:FREQ=DAILY"))
        assertEquals(Recur(Frequency.WEEKLY), Recur.parse("FREQ=WEEKLY;BYDAY=;"))
        assertEquals(
            Recur(Frequency.WEEKLY, until = Until.Date(2026, 2, 28)),
            Recur.parse("FREQ=WEEKLY;UNTIL=20260228;COUNT=-1"),
        )
    }

    @Test
    fun parseRejectsGarbage() {
        assertFailsWith<IllegalArgumentException> { Recur.parse("FREQ=DAILY;UNTIL=notadate") }
        assertFailsWith<IllegalArgumentException> { Recur.parse("FREQ=FORTNIGHTLY") }
    }

    @Test
    fun serializeMatchesIcal4j() {
        assertEquals("FREQ=DAILY", Recur(Frequency.DAILY).toString())
        assertEquals("FREQ=DAILY;INTERVAL=1", Recur(Frequency.DAILY, interval = 1).toString())
        assertEquals("FREQ=WEEKLY;COUNT=5;INTERVAL=2", Recur(Frequency.WEEKLY, interval = 2, count = 5).toString())
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE,FR", Recur(Frequency.WEEKLY, byDay = listOf(ByDay(Weekday.MO), ByDay(Weekday.WE), ByDay(Weekday.FR))).toString())
        assertEquals("FREQ=MONTHLY;BYDAY=-1FR", Recur(Frequency.MONTHLY, byDay = listOf(ByDay(Weekday.FR, -1))).toString())
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", Recur(Frequency.MONTHLY, byMonthDay = listOf(-1)).toString())
        assertEquals("FREQ=MONTHLY;UNTIL=20260228", Recur(Frequency.MONTHLY, until = Until.Date(2026, 2, 28)).toString())
        assertEquals(
            "FREQ=DAILY;UNTIL=20260228T235959Z",
            Recur(Frequency.DAILY, until = Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59, timeZone = DateTime.UTC).millis, utc = true)).toString(),
        )
        assertEquals(
            "FREQ=YEARLY;WKST=MO;BYMONTH=6;BYWEEKNO=20;BYYEARDAY=100;BYHOUR=3;BYMINUTE=2;BYSECOND=1;BYSETPOS=-1",
            Recur(
                Frequency.YEARLY, bySecond = listOf(1), byMinute = listOf(2), byHour = listOf(3), byYearDay = listOf(100),
                byWeekNo = listOf(20), byMonth = listOf(6), bySetPos = listOf(-1), weekStart = Weekday.MO,
            ).toString(),
        )
    }

    @Test
    fun serializeFloatingUntil() {
        assertEquals(
            "FREQ=DAILY;UNTIL=20260228T235959",
            Recur(Frequency.DAILY, until = Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59).millis, utc = false)).toString(),
        )
    }

    @Test
    fun roundTrips() {
        listOf(
            "FREQ=DAILY",
            "FREQ=DAILY;INTERVAL=1",
            "FREQ=DAILY;INTERVAL=3",
            "FREQ=WEEKLY;COUNT=10;INTERVAL=2;BYDAY=MO,WE,FR",
            "FREQ=WEEKLY;WKST=SU;BYDAY=MO",
            "FREQ=MONTHLY;BYDAY=2TU",
            "FREQ=MONTHLY;BYDAY=-1FR",
            "FREQ=MONTHLY;BYMONTHDAY=-1",
            "FREQ=MONTHLY;BYMONTH=1,3,5,7,9,11",
            "FREQ=YEARLY;UNTIL=20301231",
            "FREQ=DAILY;UNTIL=20260228T235959Z",
            "FREQ=HOURLY;INTERVAL=6",
            "FREQ=MINUTELY;INTERVAL=30",
        ).forEach { assertEquals(it, Recur.parse(it).toString()) }
    }

    @Test
    fun nextOccurrenceDaily() {
        val start = DateTime(2026, 1, 30, 13, 30)
        assertEquals(DateTime(2026, 1, 31, 13, 30), Recur(Frequency.DAILY).nextOccurrence(start, hasTime = true))
        assertEquals(DateTime(2026, 2, 2, 13, 30), Recur(Frequency.DAILY, interval = 3).nextOccurrence(start, hasTime = true))
    }

    @Test
    fun nextOccurrenceWithoutTimeIsAtMidnight() {
        assertEquals(DateTime(2026, 1, 31), Recur(Frequency.DAILY).nextOccurrence(DateTime(2026, 1, 30, 13, 30), hasTime = false))
    }

    @Test
    fun nextOccurrenceKeepsWallClockTimeAcrossUsSpringForward() {
        val dayBeforeUsSpringForward = DateTime(2026, 3, 7, 9, 0)
        assertEquals(DateTime(2026, 3, 8, 9, 0), Recur(Frequency.DAILY).nextOccurrence(dayBeforeUsSpringForward, hasTime = true))
    }

    @Test
    fun nextOccurrenceMonthlySkipsMonthsWithoutThatDay() {
        assertEquals(DateTime(2026, 3, 31, 13, 30), Recur(Frequency.MONTHLY).nextOccurrence(DateTime(2026, 1, 31, 13, 30), hasTime = true))
    }

    @Test
    fun nextOccurrenceMonthlyByMonthSkipsMonths() {
        assertEquals(
            DateTime(2017, 3, 31, 13, 30),
            Recur.parse("FREQ=MONTHLY;BYMONTH=1,3,5,7,9,11").nextOccurrence(DateTime(2017, 1, 31, 13, 30), hasTime = true),
        )
    }

    @Test
    fun nextOccurrenceByDay() {
        val friday = DateTime(2026, 1, 30, 13, 30)
        assertEquals(DateTime(2026, 2, 2, 13, 30), Recur.parse("FREQ=WEEKLY;BYDAY=MO,WE,FR").nextOccurrence(friday, hasTime = true))
        assertEquals(DateTime(2026, 2, 27, 13, 30), Recur.parse("FREQ=MONTHLY;BYDAY=-1FR").nextOccurrence(friday, hasTime = true))
        assertEquals(DateTime(2026, 2, 10, 13, 30), Recur.parse("FREQ=MONTHLY;BYDAY=2TU").nextOccurrence(friday, hasTime = true))
    }

    @Test
    fun nextOccurrenceStopsAtUntilAndCount() {
        val start = DateTime(2026, 1, 30, 13, 30)
        val endOfStartDay = Until.DateTime(DateTime(2026, 1, 30, 23, 59, 59).millis, utc = true)
        val endOfNextDay = Until.DateTime(DateTime(2026, 1, 31, 23, 59, 59).millis, utc = true)
        assertNull(Recur(Frequency.DAILY, until = endOfStartDay).nextOccurrence(start, hasTime = true))
        assertEquals(DateTime(2026, 1, 31, 13, 30), Recur(Frequency.DAILY, until = endOfNextDay).nextOccurrence(start, hasTime = true))
        assertNull(Recur.parse("FREQ=DAILY;COUNT=1").nextOccurrence(start, hasTime = true))
        assertEquals(DateTime(2026, 1, 31, 13, 30), Recur.parse("FREQ=DAILY;COUNT=2").nextOccurrence(start, hasTime = true))
    }

    @Test
    fun nextOccurrenceStopsAtUntilDate() {
        val start = DateTime(2026, 1, 30)
        assertNull(Recur(Frequency.DAILY, until = Until.Date(2026, 1, 30)).nextOccurrence(start, hasTime = false))
        assertEquals(DateTime(2026, 1, 31), Recur(Frequency.DAILY, until = Until.Date(2026, 1, 31)).nextOccurrence(start, hasTime = false))
    }

    @Test
    fun weekdayNumbering() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), Weekday.entries.map { it.calendarDay })
        assertEquals(Weekday.SU, DateTime(2024, 12, 22).weekday)
        assertEquals(Weekday.SA, DateTime(2024, 12, 28).weekday)
    }

    @Test
    fun untilToDateTime() {
        assertEquals(DateTime(2026, 2, 28), Until.Date(2026, 2, 28).toDateTime())
        val utc = DateTime(2026, 2, 28, 23, 59, 59, timeZone = DateTime.UTC)
        assertEquals(utc, Until.DateTime(utc.millis, utc = true).toDateTime())
        assertEquals(DateTime(2026, 2, 28, 23, 59, 59), Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59).millis, utc = false).toDateTime())
    }

    @Test
    fun weekdaysRoundTripThroughDayOfWeek() {
        assertEquals(DayOfWeek.MONDAY, Weekday.MO.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, Weekday.SU.dayOfWeek)
        Weekday.entries.forEach { assertEquals(it, it.dayOfWeek.toWeekday()) }
        DayOfWeek.entries.forEach { assertEquals(it, it.toWeekday().dayOfWeek) }
    }
}
