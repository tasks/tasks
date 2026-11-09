package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StartOfDayUtcTest {
    @Test
    fun alignsToMidnightUtc() {
        assertEquals(utc("2026-11-10T00:00:00Z"), utc("2026-11-10T10:00:00Z").startOfDayUtc())
        assertEquals(utc("2026-11-10T00:00:00Z"), utc("2026-11-10T00:00:00Z").startOfDayUtc())
        assertEquals(utc("2026-11-10T00:00:00Z"), utc("2026-11-10T23:59:59Z").startOfDayUtc())
    }

    @Test
    fun isTheSameDayWhereverTheDeviceIs() {
        val instant = utc("2026-11-10T10:00:00Z")

        val auckland = withTimeZone("Pacific/Auckland") { instant.startOfDayUtc() }
        val chicago = withTimeZone("America/Chicago") { instant.startOfDayUtc() }

        assertEquals(utc("2026-11-10T00:00:00Z"), auckland)
        assertEquals(auckland, chicago)
    }

    @Test
    fun isNotTheLocalDay() {
        val instant = utc("2026-11-10T10:00:00Z")

        withTimeZone("Pacific/Auckland") {
            assertEquals(utc("2026-11-09T11:00:00Z"), instant.startOfDay())
            assertNotEquals(instant.startOfDay(), instant.startOfDayUtc())
        }
        withTimeZone("America/Chicago") {
            assertEquals(utc("2026-11-10T06:00:00Z"), instant.startOfDay())
            assertNotEquals(instant.startOfDay(), instant.startOfDayUtc())
        }
    }

    @Test
    fun twoLocalDaysCanShareOneUtcDay() {
        withTimeZone("Pacific/Auckland") {
            val before = utc("2026-11-10T10:00:00Z")
            val after = utc("2026-11-10T11:30:00Z")

            assertNotEquals(before.startOfDay(), after.startOfDay())
            assertEquals(before.startOfDayUtc(), after.startOfDayUtc())
        }
    }

    @Test
    fun treatsNonPositiveInstantsAsZero() {
        assertEquals(0L, 0L.startOfDayUtc())
        assertEquals(0L, (-1L).startOfDayUtc())
    }

    private fun utc(timestamp: String): Long = java.time.Instant.parse(timestamp).toEpochMilli()

    private fun <T> withTimeZone(id: String, block: () -> T): T {
        val default = java.util.TimeZone.getDefault()
        return try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(id))
            block()
        } finally {
            java.util.TimeZone.setDefault(default)
        }
    }
}
