package org.tasks.analytics

import androidx.datastore.preferences.core.longPreferencesKey
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDayUtc

class SetIfNewUtcDayTest {
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val today = currentTimeMillis().startOfDayUtc()

    @Test
    fun theFirstCallOfTheDayWins() = runBlocking {
        assertTrue(preferences.setIfNewUtcDay(KEY, today))

        assertEquals(today, preferences.get(KEY, 0L))
    }

    @Test
    fun laterCallsOnTheSameDayDoNot() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today)

        assertFalse(preferences.setIfNewUtcDay(KEY, today))
    }

    @Test
    fun theNextDayWinsAgain() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today)

        assertTrue(preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(1)))
    }

    @Test
    fun theClaimStoresTheStartOfTheUtcDay() = runBlocking {
        assertTrue(preferences.setIfNewUtcDay(KEY, today + TimeUnit.HOURS.toMillis(17)))

        assertEquals(today, preferences.get(KEY, 0L))
    }

    @Test
    fun aClockSetFarInTheFutureDoesNotSilenceTheEventForever() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(1500))

        assertTrue(preferences.setIfNewUtcDay(KEY, today))

        assertEquals(today, preferences.get(KEY, 0L))
    }

    @Test
    fun aClockOneDayAheadIsToleratedRatherThanReset() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(1))

        assertFalse(preferences.setIfNewUtcDay(KEY, today))
    }

    @Test
    fun aClockOneDayAheadResumesTheDayAfter() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(1))

        assertTrue(preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(2)))
    }

    @Test
    fun aClockDaysAheadDoesNotSuppressTheEventForThatManyDays() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today + TimeUnit.DAYS.toMillis(3))

        assertTrue(preferences.setIfNewUtcDay(KEY, today))

        assertEquals(today, preferences.get(KEY, 0L))
    }

    @Test
    fun aClockSetBackWithinTheDayDoesNotLogTheEventAgain() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today)

        assertFalse(preferences.setIfNewUtcDay(KEY, today - TimeUnit.HOURS.toMillis(20)))

        assertEquals(today, preferences.get(KEY, 0L))
    }

    @Test
    fun aClockSetBackByDaysClaimsTheDayAgainRatherThanGoingQuiet() = runBlocking {
        preferences.setIfNewUtcDay(KEY, today)
        val earlier = today - TimeUnit.DAYS.toMillis(3)

        assertTrue(preferences.setIfNewUtcDay(KEY, earlier))

        assertEquals(earlier.startOfDayUtc(), preferences.get(KEY, 0L))
    }

    @Test
    fun callersArrivingTogetherProduceOneWinner() = runBlocking(Dispatchers.Default) {
        val winners = AtomicInteger()
        val start = CompletableDeferred<Unit>()
        val callers = (1..16).map {
            async {
                start.await()
                if (preferences.setIfNewUtcDay(KEY, today)) {
                    winners.incrementAndGet()
                }
            }
        }

        start.complete(Unit)
        callers.awaitAll()

        assertEquals(1, winners.get())
    }

    companion object {
        private val KEY = longPreferencesKey(utcDayKey("test_event"))
    }
}
