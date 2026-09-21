package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.time.DateTime

class RepeatMonthlyTests : RepeatTests() {
    @Test
    fun testRepeatMonthlyFromDueDate() {
        val task = newFromDue("FREQ=MONTHLY;INTERVAL=3", newDayTime(2016, 8, 28, 1, 44))

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 11, 28, 1, 44), next)
    }

    @Test
    fun testRepeatMonthlyFromCompleteDateCompleteBefore() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2016, 8, 30, 0, 25),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 29, 0, 25), next)
    }

    @Test
    fun testRepeatMonthlyFromCompleteDateCompleteAfter() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2016, 8, 28, 0, 4),
                with(COMPLETION_TIME, DateTime(2016, 8, 29, 0, 14, 13, 451)),
                afterComplete = true
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2016, 9, 29, 0, 4), next)
    }

    @Test
    fun repeatAtEndOfJanuary() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2017, 1, 31, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 2, 28, 13, 30), next)
    }

    @Test
    fun repeatMonthlyNoInterval() {
        val task = newFromDue(
            "FREQ=MONTHLY",
            newDayTime(2017, 11, 1, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 12, 1, 13, 30), next)
    }

    @Test
    fun repeatMonthlyEndOfMonthNoInterval() {
        val task = newFromDue(
            "FREQ=MONTHLY",
            newDayTime(2017, 11, 30, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 12, 30, 13, 30), next)
    }

    @Test
    fun explicitLastDayOfMonthSnapsToEachMonthEnd() {
        val task = newFromDue(
            "FREQ=MONTHLY;BYMONTHDAY=-1",
            newDayTime(2017, 11, 30, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 12, 31, 13, 30), next)
    }

    @Test
    fun clampedSixMonthChainDriftsOffThe30th() {
        var due = newDayTime(2026, 8, 30, 18, 0)
        val chain = (1..3).map {
            due = calculateNextDueDate(newFromDue("FREQ=MONTHLY;INTERVAL=6", due))
            due
        }

        assertEquals(
            listOf(
                newDayTime(2027, 2, 28, 18, 0),
                newDayTime(2027, 8, 28, 18, 0),
                newDayTime(2028, 2, 28, 18, 0),
            ),
            chain
        )
    }

    /* https://tools.ietf.org/html/rfc5545#section-3.3.10
     * Recurrence rules may generate recurrence instances with an invalid
     * date (e.g., February 30) or nonexistent local time (e.g., 1:30 AM
     * on a day where the local time is moved forward by an hour at 1:00
     * AM). We ignore this rule and clamp instead (similar to SKIP=BACKWARD from RFC 7529)
     * You can't skip a bill just because February 30th doesn't exist
     */
    @Test
    fun repeatJanuary30th() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=1",
                newDayTime(2017, 1, 30, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 2, 28, 13, 30), next)
    }

    @Test
    fun everySixMonthsFrom30thClampsIntoFebruary() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=6",
                newDayTime(2026, 8, 30, 18, 0)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2027, 2, 28, 18, 0), next)
    }

    @Test
    fun everySixMonthsFrom29thClampsIntoNonLeapFebruary() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=6",
                newDayTime(2026, 8, 29, 18, 0)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2027, 2, 28, 18, 0), next)
    }

    @Test
    fun everySixMonthsFrom29thKeepsLeapFebruary() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=6",
                newDayTime(2027, 8, 29, 18, 0)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2028, 2, 29, 18, 0), next)
    }

    @Test
    fun byMonthIsLeftToIcal4jRatherThanClamped() {
        val task = newFromDue(
                "FREQ=MONTHLY;BYMONTH=1,3,5,7,9,11",
                newDayTime(2017, 1, 31, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 3, 31, 13, 30), next)
    }

    @Test
    fun explicitLastDayOfMonthIsHandledByIcal4j() {
        val task = newFromDue(
                "FREQ=MONTHLY;BYMONTHDAY=-1",
                newDayTime(2017, 1, 31, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 2, 28, 13, 30), next)
    }

    @Test
    fun explicitLastDayOfMonthEverySixMonths() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=6;BYMONTHDAY=-1",
                newDayTime(2026, 8, 31, 18, 0)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2027, 2, 28, 18, 0), next)
    }

    @Test
    fun everySixMonthsFrom28thIsUnaffected() {
        val task = newFromDue(
                "FREQ=MONTHLY;INTERVAL=6",
                newDayTime(2026, 8, 28, 18, 0)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2027, 2, 28, 18, 0), next)
    }

    @Test
    fun clampedOccurrenceOnUntilDayIsKept() {
        val task = newFromDue(
                "FREQ=MONTHLY;UNTIL=20260228",
                newDayTime(2026, 1, 30, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2026, 2, 28, 13, 30), next)
    }

    @Test
    fun clampedOccurrenceDoesNotOutliveUntil() {
        val task = newFromDue(
                "FREQ=MONTHLY;UNTIL=20260215",
                newDayTime(2026, 1, 30, 13, 30)
        )

        calculateNextDueDate(task)

        assertTrue(task.isCompleted)
    }

    @Test
    fun explicitDayOfMonthClampsInsteadOfSkippingFebruary() {
        val task = newFromDue(
                "FREQ=MONTHLY;BYMONTHDAY=31",
                newDayTime(2017, 1, 31, 13, 30)
        )

        val next = calculateNextDueDate(task)

        assertEquals(newDayTime(2017, 2, 28, 13, 30), next)
    }

    @Test
    fun explicitDayOfMonthReturnsToTheAnchorAfterClamping() {
        var due = newDayTime(2017, 1, 31, 13, 30)
        val chain = (1..4).map {
            due = calculateNextDueDate(newFromDue("FREQ=MONTHLY;BYMONTHDAY=31", due))
            due
        }

        assertEquals(
            listOf(
                newDayTime(2017, 2, 28, 13, 30),
                newDayTime(2017, 3, 31, 13, 30),
                newDayTime(2017, 4, 30, 13, 30),
                newDayTime(2017, 5, 31, 13, 30),
            ),
            chain
        )
    }

    @Test
    fun anchoredSixMonthChainStaysOnThe30th() {
        var due = newDayTime(2026, 8, 30, 18, 0)
        val chain = (1..3).map {
            due = calculateNextDueDate(newFromDue("FREQ=MONTHLY;INTERVAL=6;BYMONTHDAY=30", due))
            due
        }

        assertEquals(
            listOf(
                newDayTime(2027, 2, 28, 18, 0),
                newDayTime(2027, 8, 30, 18, 0),
                newDayTime(2028, 2, 29, 18, 0),
            ),
            chain
        )
    }
}