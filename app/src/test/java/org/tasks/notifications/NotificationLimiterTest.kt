package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.notifications.NotificationManager.Companion.SUMMARY_NOTIFICATION_ID

class NotificationLimiterTest {
    @Test
    fun nothingIsEvictedUntilTheCapIsReached() {
        val limiter = NotificationLimiter(3)

        assertEquals(emptyList<Long>(), limiter.add(1))
        assertEquals(emptyList<Long>(), limiter.add(2))
        assertEquals(emptyList<Long>(), limiter.add(3))
    }

    @Test
    fun theOldestGoesWhenTheCapIsExceeded() {
        val limiter = NotificationLimiter(3)
        limiter.add(1)
        limiter.add(2)
        limiter.add(3)

        assertEquals(listOf(1L), limiter.add(4))
        assertEquals(listOf(2L), limiter.add(5))
    }

    @Test
    fun aBatchIsEvictedOnePostAtATime() {
        val limiter = NotificationLimiter(2)
        limiter.add(1)
        limiter.add(2)

        assertEquals(listOf(1L), limiter.add(3))
        assertEquals(listOf(2L), limiter.add(4))
    }

    @Test
    fun rePostingMovesANotificationToTheBackOfTheQueue() {
        val limiter = NotificationLimiter(3)
        limiter.add(1)
        limiter.add(2)
        limiter.add(3)

        assertEquals(emptyList<Long>(), limiter.add(1))

        assertEquals(listOf(2L), limiter.add(4))
    }

    @Test
    fun removingMakesRoom() {
        val limiter = NotificationLimiter(3)
        limiter.add(1)
        limiter.add(2)
        limiter.add(3)

        limiter.remove(2)

        assertEquals(emptyList<Long>(), limiter.add(4))
        assertEquals(listOf(1L), limiter.add(5))
    }

    @Test
    fun removingABatchMakesRoomForAllOfIt() {
        val limiter = NotificationLimiter(3)
        limiter.add(1)
        limiter.add(2)
        limiter.add(3)

        limiter.remove(listOf(1L, 2L))

        assertEquals(emptyList<Long>(), limiter.add(4))
        assertEquals(emptyList<Long>(), limiter.add(5))
        assertEquals(listOf(3L), limiter.add(6))
    }

    @Test
    fun theSummaryTakesUpRoomButIsNeverEvicted() {
        val limiter = NotificationLimiter(3)
        limiter.add(SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1)
        limiter.add(2)

        assertEquals(listOf(1L), limiter.add(3))

        assertEquals(listOf(2L), limiter.add(4))
    }

    @Test
    fun removingTheSummaryMakesRoom() {
        val limiter = NotificationLimiter(3)
        limiter.add(SUMMARY_NOTIFICATION_ID.toLong())
        limiter.add(1)
        limiter.add(2)

        limiter.remove(SUMMARY_NOTIFICATION_ID.toLong())

        assertEquals(emptyList<Long>(), limiter.add(3))
    }
}
