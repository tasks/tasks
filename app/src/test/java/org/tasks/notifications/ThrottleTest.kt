package org.tasks.notifications

import androidx.work.impl.utils.SynchronousExecutor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@ExperimentalCoroutinesApi
class ThrottleTest {
    private lateinit var sleep: ArrayList<Long>
    private lateinit var throttle: Throttle

    @Before
    fun setUp() {
        sleep = ArrayList()
    }

    @Test
    fun dontThrottle() {
        throttle = Throttle(3, executor = SynchronousExecutor()) { sleep.add(it) }
        val now = currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 1000)
        assertTrue(sleep.isEmpty())
    }

    @Test
    fun throttleForOneMillisecond() {
        throttle = Throttle(3, executor = SynchronousExecutor()) { sleep.add(it) }
        val now = currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 999)
        assertEquals(arrayListOf(1L), sleep)
    }

    @Test
    fun throttleForOneSecond() {
        throttle = Throttle(3, executor = SynchronousExecutor()) { sleep.add(it) }
        val now = currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now)
        assertEquals(arrayListOf(1000L), sleep)
    }

    @Test
    fun throttleMultiple() {
        throttle = Throttle(3, executor = SynchronousExecutor()) { sleep.add(it) }
        val now = currentTimeMillis()
        runAt(now)
        runAt(now + 200)
        runAt(now + 600)
        runAt(now + 700)
        assertEquals(arrayListOf(300L), sleep)
        runAt(now + 750)
        assertEquals(arrayListOf(300L, 450L), sleep)
    }

    private fun runAt(millis: Long) {
        Freeze.freezeAt(millis) { throttle.run {} }
    }
}