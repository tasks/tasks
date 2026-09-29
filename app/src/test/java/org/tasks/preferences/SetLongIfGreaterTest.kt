package org.tasks.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class SetLongIfGreaterTest {
    private val preferences = Preferences(RuntimeEnvironment.getApplication(), "test_preferences")
    private val today = currentTimeMillis().startOfDay()

    @Before
    fun setUp() = preferences.clear()

    @Test
    fun theFirstCallOfTheDayWins() {
        assertTrue(preferences.setLongIfGreater(KEY, today))

        assertEquals(today, preferences.getLong(KEY, 0L))
    }

    @Test
    fun laterCallsOnTheSameDayDoNot() {
        preferences.setLongIfGreater(KEY, today)

        assertFalse(preferences.setLongIfGreater(KEY, today))
    }

    @Test
    fun theNextDayWinsAgain() {
        preferences.setLongIfGreater(KEY, today)

        assertTrue(preferences.setLongIfGreater(KEY, today + TimeUnit.DAYS.toMillis(1)))
    }

    @Test
    fun callersArrivingTogetherProduceOneWinner() {
        val winners = AtomicInteger()
        val barrier = CyclicBarrier(THREADS)
        val callers = (1..THREADS).map {
            Thread {
                barrier.await()
                if (preferences.setLongIfGreater(KEY, today)) {
                    winners.incrementAndGet()
                }
            }
        }

        callers.forEach { it.start() }
        callers.forEach { it.join() }

        assertEquals(1, winners.get())
    }

    companion object {
        private const val KEY = "last_logged_cp_api"
        private const val THREADS = 16
    }
}
