package com.todoroo.astrid.repeats

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatSecondlyTests : RepeatTests() {
    @Test
    fun testRepeatSecondlyAdvancesAtLeastOneMinute() {
        val task = newFromDue("FREQ=SECONDLY;INTERVAL=30", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 26, 12, 31), next)
    }

    @Test
    fun testRepeatSecondlyUsesIntervalsLongerThanAMinute() {
        val task = newFromDue("FREQ=SECONDLY;INTERVAL=180", newDayTime(2016, 8, 26, 12, 30))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 8, 26, 12, 33), next)
    }
}
