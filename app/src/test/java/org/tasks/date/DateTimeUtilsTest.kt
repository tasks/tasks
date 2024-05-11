package org.tasks.date

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.TimeZone

class DateTimeUtilsTest {
    private val now = DateTime(2014, 1, 1, 15, 17, 53, 0)

    @Test
    fun testGetCurrentTime() {
        freezeAt(now) {
            assertEquals(now.millis, currentTimeMillis())
        }
    }

    @Test
    fun testCreateNewUtcDate() {
        val utc = now.toUTC()
        val actual = DateTimeUtils.newDateUtc(
                utc.year,
                utc.monthOfYear,
                utc.dayOfMonth,
                utc.hourOfDay,
                utc.minuteOfHour,
                utc.secondOfMinute)
        assertEquals(utc.millis, actual.millis)
    }

    @Test
    fun testIllegalInstant() {
        DateTime(2015, 7, 24, 0, 0, 0, 0, TimeZone.getTimeZone("Africa/Cairo"))
        DateTime(2015, 10, 18, 0, 0, 0, 0, TimeZone.getTimeZone("America/Sao_Paulo"))
        DateTime(2015, 10, 4, 0, 0, 0, 0, TimeZone.getTimeZone("America/Asuncion"))
    }
}