package org.tasks.repeats

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.time.DateTime

class RecurIcal4jTest {
    @Test
    fun serializerMatchesIcal4j() {
        listOf(
            Recur(Frequency.DAILY),
            Recur(Frequency.DAILY, interval = 1),
            Recur(Frequency.WEEKLY, interval = 2, count = 5),
            Recur(Frequency.WEEKLY, byDay = listOf(ByDay(Weekday.MO), ByDay(Weekday.WE), ByDay(Weekday.FR)), weekStart = Weekday.SU),
            Recur(Frequency.MONTHLY, byDay = listOf(ByDay(Weekday.FR, -1), ByDay(Weekday.TU, 2))),
            Recur(Frequency.MONTHLY, byMonthDay = listOf(1, 15, -1)),
            Recur(Frequency.MONTHLY, until = Until.Date(2026, 2, 28)),
            Recur(Frequency.DAILY, until = Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59, timeZone = DateTime.UTC).millis, utc = true)),
            Recur(Frequency.DAILY, until = Until.DateTime(DateTime(2026, 2, 28, 23, 59, 59).millis, utc = false)),
            Recur(
                Frequency.YEARLY, bySecond = listOf(1), byMinute = listOf(2), byHour = listOf(3), byYearDay = listOf(100),
                byWeekNo = listOf(20), byMonth = listOf(6), bySetPos = listOf(-1), weekStart = Weekday.MO,
            ),
        ).forEach { assertEquals(it.toIcal4j().toString(), serializeRecur(it)) }
    }
}
