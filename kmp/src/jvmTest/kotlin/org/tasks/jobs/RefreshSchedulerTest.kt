package org.tasks.jobs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.jobs.RefreshScheduler.Companion.MAX_WAIT
import org.tasks.jobs.RefreshScheduler.Companion.MIN_WAIT
import org.tasks.jobs.RefreshScheduler.Companion.NEVER
import org.tasks.jobs.RefreshScheduler.Companion.midnight
import org.tasks.jobs.RefreshScheduler.Companion.waitFor
import org.tasks.time.DateTimeUtils2

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshSchedulerTest {
    private lateinit var savedZone: java.util.TimeZone

    private var now = 0L

    private var refreshes = 0

    private var queries = 0

    private var dueDates = emptyList<Long>()

    private var failWith: Throwable? = null

    private val scheduler = RefreshScheduler(
        nextRefresh = { now ->
            queries++
            failWith?.let { throw it }
            dueDates.filter { it > now }.minOrNull() ?: NEVER
        },
        refresh = { refreshes++ },
    )

    @Before
    fun setUp() {
        savedZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(ZONE))
    }

    @After
    fun tearDown() {
        DateTimeUtils2.setCurrentMillisSystem()
        java.util.TimeZone.setDefault(savedZone)
    }

    @Test
    fun midnightIsTheStartOfTheNextDay() {
        assertEquals(at(day = 11, hour = 0), midnight(at(day = 10, hour = 0)))
        assertEquals(at(day = 11, hour = 0), midnight(at(day = 10, hour = 23, minute = 59, second = 59)))
        assertEquals(at(day = 12, hour = 0), midnight(at(day = 11, hour = 0)))
    }

    @Test
    fun waitsNoLongerThanTheCapAndNoShorterThanTheFloor() {
        assertEquals(MIN_WAIT, waitFor(due = 0, now = 1_000))
        assertEquals(MIN_WAIT, waitFor(due = 1_500, now = 1_000))
        assertEquals(30_000, waitFor(due = 31_000, now = 1_000))
        assertEquals(MAX_WAIT, waitFor(due = NEVER, now = 1_000))
    }

    @Test
    fun startingUpDoesNotRefresh() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        scheduler.start(backgroundScope)
        runCurrent()

        assertEquals(0, refreshes)
        assertEquals(1, queries)
    }

    @Test
    fun refreshesOnceMidnightPasses() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 23, minute = 59, second = 30))
        scheduler.start(backgroundScope)
        runCurrent()

        advance(29_999)
        assertEquals(0, refreshes)

        advance(1)
        assertEquals(1, refreshes)

        advance(10 * MAX_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun refreshesWhenTheNextDueDatePasses() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        dueDates = listOf(now + 90_000)
        scheduler.start(backgroundScope)
        runCurrent()

        advance(MAX_WAIT)
        assertEquals(0, refreshes)

        advance(29_999)
        assertEquals(0, refreshes)

        advance(1)
        assertEquals(1, refreshes)

        advance(10 * MAX_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun aScheduledRefreshBringsTheNextOneForward() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        scheduler.start(backgroundScope)
        runCurrent()

        scheduler.schedule(now + 5_000)
        advance(MIN_WAIT)
        assertEquals(0, refreshes)

        advance(3_999)
        assertEquals(0, refreshes)

        advance(1)
        assertEquals(1, refreshes)

        advance(10 * MAX_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun theEarliestScheduledRefreshWins() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        scheduler.start(backgroundScope)
        runCurrent()

        scheduler.schedule(now + 5_000)
        scheduler.schedule(now + 3_000)
        scheduler.schedule(now + 8_000)
        advance(3_000)
        assertEquals(1, refreshes)

        advance(10 * MAX_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun aRefreshScheduledInThePastFiresAtTheFloor() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        scheduler.start(backgroundScope)
        runCurrent()

        scheduler.schedule(0)
        runCurrent()
        assertEquals(0, refreshes)

        advance(MIN_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun sleepingThroughMidnightIsCaughtUpWithinTheCap() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 22))
        scheduler.start(backgroundScope)
        runCurrent()

        setTime(at(day = 11, hour = 8))
        advanceTimeBy(MAX_WAIT)
        runCurrent()
        assertEquals(1, refreshes)

        advance(10 * MAX_WAIT)
        assertEquals(1, refreshes)
    }

    @Test
    fun aSignalRechecksTheClockStraightAway() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 22))
        scheduler.start(backgroundScope)
        runCurrent()
        advance(MIN_WAIT)

        setTime(at(day = 11, hour = 8))
        scheduler.signal()
        runCurrent()
        assertEquals(1, refreshes)
    }

    @Test
    fun aFailingQueryStillRefreshesAtMidnight() = runTest(StandardTestDispatcher()) {
        failWith = RuntimeException("no database")
        setTime(at(day = 10, hour = 23, minute = 59, second = 30))
        scheduler.start(backgroundScope)
        runCurrent()

        advance(30_000)
        assertEquals(1, refreshes)
    }

    @Test
    fun startingTwiceDoesNotRunTwoLoops() = runTest(StandardTestDispatcher()) {
        setTime(at(day = 10, hour = 10))
        scheduler.start(backgroundScope)
        scheduler.start(backgroundScope)
        runCurrent()
        assertEquals(1, queries)

        advance(MAX_WAIT)
        assertEquals(2, queries)
    }

    private fun TestScope.advance(millis: Long) {
        var remaining = millis
        while (remaining > 0) {
            val step = minOf(remaining, MIN_WAIT)
            setTime(now + step)
            advanceTimeBy(step)
            runCurrent()
            remaining -= step
        }
    }

    private fun setTime(millis: Long) {
        now = millis
        DateTimeUtils2.setCurrentMillisFixed(millis)
    }

    companion object {
        private const val ZONE = "America/Chicago"

        private fun at(day: Int, hour: Int, minute: Int = 0, second: Int = 0): Long =
            LocalDateTime(2025, 6, day, hour, minute, second)
                .toInstant(TimeZone.of(ZONE))
                .toEpochMilliseconds()
    }
}
