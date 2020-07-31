package org.tasks.notifications

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.SuspendFreeze
import org.tasks.time.DateTimeUtils

@ExperimentalCoroutinesApi
class ThrottleTest {
    private lateinit var sleep: ArrayList<Long>
    private lateinit var throttle: Throttle

    @Before
    fun setUp() {
        sleep = ArrayList()
    }

    @Test
    fun dontThrottle() = runBlockingTest {
        throttle = Throttle(3, scope = this) { sleep.add(it) }
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 1000)
        assertTrue(sleep.isEmpty())
    }

    @Test
    fun throttleForOneMillisecond() = runBlockingTest {
        throttle = Throttle(3, scope = this) { sleep.add(it) }
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 999)
        assertEquals(arrayListOf(1L), sleep)
    }

    @Test
    fun throttleForOneSecond() = runBlockingTest {
        throttle = Throttle(3, scope = this) { sleep.add(it) }
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now)
        assertEquals(arrayListOf(1000L), sleep)
    }

    @Test
    fun throttleMultiple() = runBlockingTest {
        throttle = Throttle(3, scope = this) { sleep.add(it) }
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now + 200)
        runAt(now + 600)
        runAt(now + 700)
        assertEquals(arrayListOf(300L), sleep)
        runAt(now + 750)
        assertEquals(arrayListOf(300L, 450L), sleep)
    }

    private suspend fun runAt(millis: Long) {
        SuspendFreeze.freezeAt(millis) { throttle.run {} }
    }
}