package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.withTZ

class DatePickerConversionTest {

    private fun midnightUtc(year: Int, month: Int, day: Int): Long =
        DateTime(year, month, day, timeZone = DateTime.UTC).millis

    private fun startOfDay(year: Int, month: Int, day: Int): Long =
        DateTime(year, month, day).millis

    private fun assertRoundTrip(year: Int, month: Int, day: Int, msg: String = "") {
        val label = msg.ifEmpty { "$year-${"%02d".format(month)}-${"%02d".format(day)}" }
        val local = startOfDay(year, month, day)
        assertEquals(label, local, DateTime.toLocalDateMillis(DateTime.toUtcDateMillis(local)))
    }

    private fun assertForwardGivesMidnightUtc(year: Int, month: Int, day: Int) {
        val local = startOfDay(year, month, day)
        assertEquals(midnightUtc(year, month, day), DateTime.toUtcDateMillis(local))
    }

    // -- Asia/Beirut: midnight spring-forward, last Sunday of March --

    @Test
    fun beirut_roundTrip() = withTZ("Asia/Beirut") {
        assertRoundTrip(2026, 3, 29)
    }

    @Test
    fun beirut_forwardGivesMidnightUtc() = withTZ("Asia/Beirut") {
        assertForwardGivesMidnightUtc(2026, 3, 29)
    }

    @Test
    fun beirut_acrossYears() = withTZ("Asia/Beirut") {
        // Last Sundays of March
        for ((year, day) in mapOf(1993 to 28, 2000 to 26, 2024 to 31, 2025 to 30, 2026 to 29, 2027 to 28, 2028 to 26, 2099 to 29)) {
            assertRoundTrip(year, 3, day)
        }
    }

    @Test
    fun beirut_nonTransitionSundaysUnaffected() = withTZ("Asia/Beirut") {
        for (day in listOf(1, 8, 15, 22)) {
            assertForwardGivesMidnightUtc(2026, 3, day)
        }
    }

    // -- Atlantic/Azores: EU DST at 1:00 UTC = midnight local (UTC-1) --

    @Test
    fun azores_roundTrip() = withTZ("Atlantic/Azores") {
        assertRoundTrip(2026, 3, 29)
    }

    @Test
    fun azores_acrossYears() = withTZ("Atlantic/Azores") {
        for ((year, day) in mapOf(2024 to 31, 2025 to 30, 2026 to 29, 2027 to 28, 2028 to 26)) {
            assertRoundTrip(year, 3, day)
        }
    }

    // -- America/Havana: midnight spring-forward, 2nd Sunday of March --

    @Test
    fun havana_roundTrip() = withTZ("America/Havana") {
        assertRoundTrip(2026, 3, 8)
    }

    @Test
    fun havana_acrossYears() = withTZ("America/Havana") {
        for ((year, day) in mapOf(2024 to 10, 2025 to 9, 2026 to 8, 2027 to 14, 2028 to 12)) {
            assertRoundTrip(year, 3, day)
        }
    }

    // -- Africa/Cairo: midnight spring-forward in April --

    @Test
    fun cairo_roundTrip() = withTZ("Africa/Cairo") {
        assertRoundTrip(2026, 4, 24)
    }

    @Test
    fun cairo_acrossYears() = withTZ("Africa/Cairo") {
        for ((year, day) in mapOf(2024 to 26, 2025 to 25, 2026 to 24, 2027 to 30, 2028 to 28)) {
            assertRoundTrip(year, 4, day)
        }
    }

    // -- America/Santiago: midnight spring-forward in September --

    @Test
    fun santiago_roundTrip() = withTZ("America/Santiago") {
        assertRoundTrip(2026, 9, 6)
    }

    @Test
    fun santiago_acrossYears() = withTZ("America/Santiago") {
        for ((year, day) in mapOf(2024 to 8, 2025 to 7, 2026 to 6, 2027 to 5, 2028 to 3)) {
            assertRoundTrip(year, 9, day)
        }
    }

    // -- America/Asuncion: midnight spring-forward in October --

    @Test
    fun asuncion_roundTrip() = withTZ("America/Asuncion") {
        assertRoundTrip(2024, 10, 6)
    }

    // -- America/Coyhaique: midnight spring-forward in September --

    @Test
    fun coyhaique_roundTrip() = withTZ("America/Coyhaique") {
        assertRoundTrip(2024, 9, 8)
    }

    // -- Timezones with large offsets (UTC+12 and beyond) --

    @Test
    fun auckland_roundTrip() = withTZ("Pacific/Auckland") {
        // UTC+12/+13, DST at 2:00 AM
        assertRoundTrip(2026, 9, 27)  // NZ spring-forward
        assertRoundTrip(2026, 4, 5)   // NZ fall-back
        assertRoundTrip(2026, 1, 15)  // mid-summer
        assertRoundTrip(2026, 7, 15)  // mid-winter
    }

    @Test
    fun chatham_roundTrip() = withTZ("Pacific/Chatham") {
        // UTC+12:45/+13:45
        assertRoundTrip(2026, 9, 27)
        assertRoundTrip(2026, 4, 5)
    }

    @Test
    fun apia_roundTrip() = withTZ("Pacific/Apia") {
        // UTC+13/+14
        assertRoundTrip(2026, 9, 27)
        assertRoundTrip(2026, 4, 5)
    }

    @Test
    fun tongatapu_roundTrip() = withTZ("Pacific/Tongatapu") {
        // UTC+13
        assertRoundTrip(2026, 6, 15)
    }

    @Test
    fun kiritimati_roundTrip() = withTZ("Pacific/Kiritimati") {
        // UTC+14
        assertRoundTrip(2026, 6, 15)
    }

    @Test
    fun samoa_roundTrip() = withTZ("US/Samoa") {
        // UTC-11
        assertRoundTrip(2026, 6, 15)
    }

    // -- Timezones where DST does not transition at midnight (no old-code breakage) --

    @Test
    fun newYork_roundTrip() = withTZ("America/New_York") {
        assertRoundTrip(2026, 3, 8)
    }

    @Test
    fun berlin_roundTrip() = withTZ("Europe/Berlin") {
        assertRoundTrip(2026, 3, 29)
    }

    @Test
    fun utc_roundTrip() = withTZ("UTC") {
        assertRoundTrip(2026, 3, 29)
    }
}
