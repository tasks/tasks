package org.tasks.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.tasks.Freeze
import org.tasks.notifications.Throttle
import org.tasks.time.DateTimeUtils

@RunWith(AndroidJUnit4::class)
class ThrottleTest {
    private lateinit var sleeper: Throttle.Sleeper
    private lateinit var throttle: Throttle

    @Before
    fun setUp() {
        sleeper = Mockito.mock(Throttle.Sleeper::class.java)
        throttle = Throttle(3, sleeper)
    }

    @After
    fun tearDown() {
        Mockito.verifyNoMoreInteractions(sleeper)
    }

    @Test
    fun dontThrottle() {
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 1000)
    }

    @Test
    fun throttleForOneMillisecond() {
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now + 999)
        Mockito.verify(sleeper).sleep(1)
    }

    @Test
    fun throttleForOneSecond() {
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now)
        runAt(now)
        runAt(now)
        Mockito.verify(sleeper).sleep(1000)
    }

    @Test
    fun throttleMultiple() {
        val now = DateTimeUtils.currentTimeMillis()
        runAt(now)
        runAt(now + 200)
        runAt(now + 600)
        runAt(now + 700)
        Mockito.verify(sleeper).sleep(300)
        runAt(now + 750)
        Mockito.verify(sleeper).sleep(450)
    }

    private fun runAt(millis: Long) {
        Freeze.freezeAt(millis) { throttle.run {} }
    }
}