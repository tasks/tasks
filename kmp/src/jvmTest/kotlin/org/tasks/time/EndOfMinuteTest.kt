package org.tasks.time

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EndOfMinuteTest {
    private lateinit var savedZone: java.util.TimeZone

    @Before
    fun setUp() {
        savedZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(ZONE))
    }

    @After
    fun tearDown() {
        java.util.TimeZone.setDefault(savedZone)
    }

    @Test
    fun windsForwardToTheEndOfTheMinute() {
        val firstPass = 1_762_061_430_000L

        assertEquals(firstPass + 29_999L, firstPass.endOfMinute())
    }

    @Test
    fun neverGoesBackwardsThroughADaylightSavingFallBack() {
        val secondPass = 1_762_065_030_000L

        assertTrue(
            "endOfMinute went backwards: ${secondPass.endOfMinute()} < $secondPass",
            secondPass.endOfMinute() >= secondPass,
        )
    }

    @Test
    fun holdsForEverySecondOfTheRepeatedHour() {
        val start = 1_762_059_600_000L
        var offset = 0L
        while (offset < 2 * 60 * 60_000L) {
            val timestamp = start + offset
            assertTrue(
                "endOfMinute went backwards at $timestamp",
                timestamp.endOfMinute() >= timestamp,
            )
            offset += 1_000L
        }
    }

    @Test
    fun leavesTheZeroSentinelAlone() {
        assertEquals(0L, 0L.endOfMinute())
    }

    companion object {
        private const val ZONE = "America/New_York"
    }
}
