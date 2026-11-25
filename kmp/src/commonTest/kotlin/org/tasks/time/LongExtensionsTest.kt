package org.tasks.time

import kotlin.test.Test
import kotlin.test.assertEquals

class LongExtensionsTest {
    @Test
    fun countDaysBetweenTimestamps() =
        assertEquals(1, day(2023, 7, 19).daysUntil(day(2023, 7, 20)))

    @Test
    fun countDaysBackwards() =
        assertEquals(-7, day(2023, 7, 19).daysUntil(day(2023, 7, 12)))

    @Test
    fun ignorePartialDays() =
        assertEquals(1, day(2023, 7, 19, 23, 59).daysUntil(day(2023, 7, 20, 0, 1)))

    @Test
    fun noDaysUntilUnsetTimestamp() =
        assertEquals(0, day(2023, 7, 19).daysUntil(0))

    @Test
    fun noDaysFromUnsetTimestamp() =
        assertEquals(0, 0L.daysUntil(day(2023, 7, 19)))

    private fun day(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ) = DateTime(year, month, day, hour, minute).millis
}
