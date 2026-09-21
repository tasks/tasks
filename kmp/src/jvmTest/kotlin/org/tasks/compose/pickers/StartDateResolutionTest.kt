package org.tasks.compose.pickers

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.time.hourOfDay
import org.tasks.time.minusDays
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay

class StartDateResolutionTest {

    private lateinit var savedZone: java.util.TimeZone
    private var today = 0L
    private var dueNoon = 0L
    private var dueWithTime = 0L
    private val nineAm = 9 * 60 * 60 * 1000 + 1000

    @Before
    fun setUp() {
        savedZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(FIXED_ZONE))
        today = FIXED_DAY.startOfDay()
        dueNoon = today.withMillisOfDay(12 * 60 * 60 * 1000)
        dueWithTime = today.withMillisOfDay(9 * 60 * 60 * 1000 + 1000)
    }

    @After
    fun tearDown() {
        java.util.TimeZone.setDefault(savedZone)
    }

    @Test
    fun noStartResolvesToZero() {
        assertEquals(0L, resolveStartDate(StartDate.None, NO_TIME, dueNoon))
    }

    @Test
    fun relativeStartWithoutDueResolvesToZero() {
        assertEquals(0L, resolveStartDate(StartDate.DueDate, NO_TIME, 0))
        assertEquals(0L, resolveStartDate(StartDate.DueTime, NO_TIME, 0))
        assertEquals(0L, resolveStartDate(StartDate.DayBeforeDue, NO_TIME, 0))
        assertEquals(0L, resolveStartDate(StartDate.WeekBeforeDue, NO_TIME, 0))
    }

    @Test
    fun dueDateStartResolvesToStartOfDueDay() {
        assertEquals(dueNoon.startOfDay(), resolveStartDate(StartDate.DueDate, NO_TIME, dueNoon))
    }

    @Test
    fun dayBeforeAndWeekBeforeResolveRelativeToDue() {
        assertEquals(dueNoon.startOfDay().minusDays(1), resolveStartDate(StartDate.DayBeforeDue, NO_TIME, dueNoon))
        assertEquals(dueNoon.startOfDay().minusDays(7), resolveStartDate(StartDate.WeekBeforeDue, NO_TIME, dueNoon))
    }

    @Test
    fun selectionCollapsesRelativeChoices() {
        assertEquals(
            StartDateSelection(StartDate.DueDate, NO_TIME),
            startDateSelection(resolveStartDate(StartDate.DueDate, NO_TIME, dueNoon), dueNoon),
        )
        assertEquals(
            StartDateSelection(StartDate.DayBeforeDue, NO_TIME),
            startDateSelection(resolveStartDate(StartDate.DayBeforeDue, NO_TIME, dueNoon), dueNoon),
        )
        assertEquals(
            StartDateSelection(StartDate.WeekBeforeDue, NO_TIME),
            startDateSelection(resolveStartDate(StartDate.WeekBeforeDue, NO_TIME, dueNoon), dueNoon),
        )
    }

    @Test
    fun dueTimeRoundTripsWhenDueHasTime() {
        val hideUntil = resolveStartDate(StartDate.DueTime, NO_TIME, dueWithTime)
        assertEquals(StartDateSelection(StartDate.DueTime, NO_TIME), startDateSelection(hideUntil, dueWithTime))
    }

    @Test
    fun dueTimeRoundTripsOnDateOnlyDue() {
        val hideUntil = resolveStartDate(StartDate.DueTime, NO_TIME, dueNoon)
        assertEquals(dueNoon, hideUntil)
        assertEquals(StartDateSelection(StartDate.DueTime, NO_TIME), startDateSelection(hideUntil, dueNoon))
    }

    @Test
    fun absoluteStartRoundTrips() {
        val abs = today.plusDays(3)
        val hideUntil = resolveStartDate(StartDate.Absolute(abs), NO_TIME, dueNoon)
        assertEquals(abs, hideUntil)
        assertEquals(StartDateSelection(StartDate.Absolute(abs), NO_TIME), startDateSelection(hideUntil, dueNoon))
    }

    @Test
    fun absoluteStartWithTimeRoundTrips() {
        val abs = today.plusDays(3)
        val hideUntil = resolveStartDate(StartDate.Absolute(abs), nineAm, dueNoon)
        assertEquals(abs.withMillisOfDay(nineAm), hideUntil)
        assertEquals(
            StartDateSelection(StartDate.Absolute(abs), nineAm),
            startDateSelection(hideUntil, dueNoon),
        )
    }

    @Test
    fun dueDateStartWithTimeResolvesAndRoundTrips() {
        val hideUntil = resolveStartDate(StartDate.DueDate, nineAm, dueNoon)
        assertEquals(dueNoon.startOfDay().withMillisOfDay(nineAm), hideUntil)
        assertEquals(StartDateSelection(StartDate.DueDate, nineAm), startDateSelection(hideUntil, dueNoon))
    }

    @Test
    fun dayBeforeAndWeekBeforeWithTimeResolveAndRoundTrip() {
        val dayBefore = resolveStartDate(StartDate.DayBeforeDue, nineAm, dueNoon)
        assertEquals(dueNoon.startOfDay().minusDays(1).withMillisOfDay(nineAm), dayBefore)
        assertEquals(StartDateSelection(StartDate.DayBeforeDue, nineAm), startDateSelection(dayBefore, dueNoon))

        val weekBefore = resolveStartDate(StartDate.WeekBeforeDue, nineAm, dueNoon)
        assertEquals(dueNoon.startOfDay().minusDays(7).withMillisOfDay(nineAm), weekBefore)
        assertEquals(StartDateSelection(StartDate.WeekBeforeDue, nineAm), startDateSelection(weekBefore, dueNoon))
    }

    @Test
    fun withTimeMarkerStripsSecondsBelowTheMinute() {
        val timed = today.withMillisOfDay(9 * 60 * 60 * 1000 + 45_000)
        assertEquals(
            today.withMillisOfDay(9 * 60 * 60 * 1000 + 1000),
            timed.withTimeMarkerOr { it.startOfDay() },
        )
    }

    @Test
    fun absoluteStartWithTimeIsDstCorrectAcrossSpringForward() {
        val springForwardDay = LocalDateTime(2027, 3, 14, 0, 0)
            .toInstant(TimeZone.of(FIXED_ZONE))
            .toEpochMilliseconds()
        val hideUntil = resolveStartDate(StartDate.Absolute(springForwardDay), nineAm, dueNoon)
        assertEquals(9, hideUntil.hourOfDay)
        assertEquals(10, (springForwardDay + nineAm).hourOfDay)
    }

    @Test
    fun relativeChoicesReTrackAMovedDueDate() {
        val dueA = today.plusDays(2).withMillisOfDay(12 * 60 * 60 * 1000)
        val dueB = today.plusDays(9).withMillisOfDay(12 * 60 * 60 * 1000)
        assertEquals(dueA.startOfDay(), resolveStartDate(StartDate.DueDate, NO_TIME, dueA))
        assertEquals(dueB.startOfDay(), resolveStartDate(StartDate.DueDate, NO_TIME, dueB))
        assertEquals(dueA.startOfDay().minusDays(1), resolveStartDate(StartDate.DayBeforeDue, NO_TIME, dueA))
        assertEquals(dueB.startOfDay().minusDays(1), resolveStartDate(StartDate.DayBeforeDue, NO_TIME, dueB))
    }

    @Test
    fun isRelativeIsTrueForDueAnchoredChoicesOnly() {
        assertTrue(StartDate.DueDate.isRelative)
        assertTrue(StartDate.DueTime.isRelative)
        assertTrue(StartDate.DayBeforeDue.isRelative)
        assertTrue(StartDate.WeekBeforeDue.isRelative)
        assertFalse(StartDate.None.isRelative)
        assertFalse(StartDate.Absolute(today).isRelative)
    }

    @Test
    fun startDayWireValuesRoundTrip() {
        listOf(NO_DAY, DUE_DATE, DUE_TIME, DAY_BEFORE_DUE, WEEK_BEFORE_DUE, today).forEach { wire ->
            assertEquals(wire, startDayOf(wire).toStartDay())
        }
        assertEquals(StartDate.DueDate, startDayOf(-1L))
        assertEquals(StartDate.Absolute(today), startDayOf(today))
    }

    @Test
    fun initialStartTimeKeepsRealTimeStripsDateOnly() {
        assertEquals(nineAm, initialStartTime(nineAm))
        assertEquals(NO_TIME, initialStartTime(12 * 60 * 60 * 1000))
        assertEquals(NO_TIME, initialStartTime(NO_TIME))
    }

    @Test
    fun negativeSentinelTimeResolvesToDateOnlyInsteadOfThrowing() {
        val abs = today.plusDays(3)
        assertEquals(abs, resolveStartDate(StartDate.Absolute(abs), MULTIPLE_TIMES, dueNoon))
        assertEquals(dueNoon.startOfDay(), resolveStartDate(StartDate.DueDate, MULTIPLE_TIMES, dueNoon))
        assertEquals(
            dueNoon.startOfDay().minusDays(1),
            resolveStartDate(StartDate.DayBeforeDue, MULTIPLE_TIMES, dueNoon),
        )
        assertEquals(
            dueNoon.startOfDay().minusDays(7),
            resolveStartDate(StartDate.WeekBeforeDue, MULTIPLE_TIMES, dueNoon),
        )
    }

    @Test
    fun resolvedStartDatesAreUnchangedByASecondNormalization() {
        listOf(
            resolveStartDate(StartDate.None, NO_TIME, dueNoon),
            resolveStartDate(StartDate.DueDate, NO_TIME, dueNoon),
            resolveStartDate(StartDate.DueDate, nineAm, dueNoon),
            resolveStartDate(StartDate.DueTime, NO_TIME, dueWithTime),
            resolveStartDate(StartDate.DayBeforeDue, nineAm, dueNoon),
            resolveStartDate(StartDate.Absolute(today.plusDays(3)), nineAm, dueNoon),
        ).forEach { resolved ->
            assertEquals(resolved, resolved.withTimeMarkerOr { it.startOfDay() })
        }
    }

    companion object {
        private const val FIXED_ZONE = "America/New_York"
        private const val FIXED_DAY = 1_800_000_000_000L
    }
}
