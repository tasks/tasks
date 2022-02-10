package org.tasks.jobs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.AdditionalAnswers
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils
import java.util.concurrent.TimeUnit

class NotificationQueueTest {
    private lateinit var queue: NotificationQueue
    private lateinit var workManager: WorkManager
    private lateinit var preferences: Preferences

    @Before
    fun before() {
        preferences = Mockito.mock(Preferences::class.java)
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong()))
            .then(AdditionalAnswers.returnsFirstArg<Any>())
        workManager = Mockito.mock(WorkManager::class.java)
        queue = NotificationQueue(preferences, workManager)
    }

    @After
    fun after() {
        Mockito.verifyNoMoreInteractions(workManager)
    }

    @Test
    fun removeAlarmDoesntAffectOtherAlarm() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        queue.add(AlarmEntry(2, 2, now, TYPE_DATE_TIME))
        queue.remove(listOf(AlarmEntry(1, 1, now, TYPE_DATE_TIME)))
        freezeAt(now) {
            assertEquals(
                listOf(AlarmEntry(2, 2, now, TYPE_DATE_TIME)),
                queue.overdueJobs
            )
        }
    }

    @Test
    fun removeByTaskDoesntAffectOtherAlarm() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        queue.add(AlarmEntry(2, 2, now, TYPE_DATE_TIME))
        queue.cancelForTask(1)
        freezeAt(now) {
            assertEquals(
                listOf(AlarmEntry(2, 2, now, TYPE_DATE_TIME)),
                queue.overdueJobs
            )
        }
    }

    @Test
    fun rescheduleForFirstJob() {
        queue.add(AlarmEntry(1, 2, 3, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(3)
    }

    @Test
    fun dontRescheduleForLaterJobs() {
        queue.add(AlarmEntry(1, 2, 3, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 3, 4, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(3)
    }

    @Test
    fun rescheduleForNewerJob() {
        queue.add(AlarmEntry(1, 1, 2, TYPE_DATE_TIME))
        queue.add(AlarmEntry(1, 1, 1, TYPE_DATE_TIME))
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(2)
        order.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun rescheduleWhenCancelingOnlyJob() {
        queue.add(AlarmEntry(1, 1, 2, TYPE_DATE_TIME))
        queue.cancelForTask(1)
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(2)
        order.verify(workManager).cancelNotifications()
    }

    @Test
    fun rescheduleWhenCancelingFirstJob() {
        queue.add(AlarmEntry(1, 1, 1, 0))
        queue.add(AlarmEntry(2, 2, 2, 0))
        queue.cancelForTask(1)
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(1)
        order.verify(workManager).scheduleNotification(2)
    }

    @Test
    fun dontRescheduleWhenCancelingLaterJob() {
        queue.add(AlarmEntry(1, 1, 1, 0))
        queue.add(AlarmEntry(2, 2, 2, 0))
        queue.cancelForTask(2)
        Mockito.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun nextScheduledTimeIsZeroWhenQueueIsEmpty() {
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong()))
            .thenReturn(1234L)
        assertEquals(0, queue.nextScheduledTime())
    }

    @Test
    fun adjustNextScheduledTimeForQuietHours() {
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong()))
            .thenReturn(1234L)
        queue.add(AlarmEntry(1, 1, 1, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(1234)
    }

    @Test
    fun overdueJobsAreReturned() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 1, now + ONE_MINUTE, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                listOf(AlarmEntry(1, 1, now, TYPE_DATE_TIME)), queue.overdueJobs
            )
        }
    }

    @Test
    fun twoOverdueJobsAtSameTimeReturned() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 2, now, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                setOf(
                    AlarmEntry(1, 1, now, TYPE_DATE_TIME),
                    AlarmEntry(2, 2, now, TYPE_DATE_TIME)
                ),
                queue.overdueJobs.toSet()
            )
        }
    }

    @Test
    fun twoOverdueJobsAtDifferentTimes() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 2, now + ONE_MINUTE, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now + 2 * ONE_MINUTE) {
            assertEquals(
                listOf(
                    AlarmEntry(1, 1, now, TYPE_DATE_TIME),
                    AlarmEntry(2, 2, now + ONE_MINUTE, TYPE_DATE_TIME)
                ),
                queue.overdueJobs
            )
        }
    }

    @Test
    fun overdueJobsAreRemoved() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 2, now + ONE_MINUTE, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            queue.remove(queue.overdueJobs)
        }
        assertEquals(
            listOf(AlarmEntry(2, 2, now + ONE_MINUTE, TYPE_DATE_TIME)), queue.getJobs()
        )
    }

    @Test
    fun multipleOverduePeriodsLapsed() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 2, now + ONE_MINUTE, TYPE_DATE_TIME))
        queue.add(AlarmEntry(3, 3, now + 2 * ONE_MINUTE, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now + ONE_MINUTE) {
            queue.remove(queue.overdueJobs)
        }
        assertEquals(
            listOf(AlarmEntry(3, 3, now + 2 * ONE_MINUTE, TYPE_DATE_TIME)), queue.getJobs()
        )
    }


    @Test
    fun clearShouldCancelExisting() {
        queue.add(AlarmEntry(1, 1, 1, 0))
        queue.clear()
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(1)
        order.verify(workManager).cancelNotifications()
        assertEquals(0, queue.size())
    }

    @Test
    fun ignoreInvalidCancelForByAlarm() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.remove(listOf(AlarmEntry(2, 2, now, TYPE_DATE_TIME)))
        Mockito.verify(workManager).scheduleNotification(now)
    }

    @Test
    fun ignoreInvalidCancelForTask() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 1, now, TYPE_DATE_TIME))
        queue.cancelForTask(2)
        Mockito.verify(workManager).scheduleNotification(now)
    }

    @Test
    fun allDuringSameMinuteAreOverdue() {
        val now = DateTime(2017, 9, 3, 0, 14, 6, 455)
        val due = DateTime(2017, 9, 3, 0, 14, 0, 0)
        val snooze = DateTime(2017, 9, 3, 0, 14, 59, 999)
        queue.add(AlarmEntry(1, 1, due.millis, TYPE_DATE_TIME))
        queue.add(AlarmEntry(2, 2, snooze.millis, TYPE_SNOOZE))
        queue.add(AlarmEntry(3, 3, due.plusMinutes(1).millis, TYPE_DATE_TIME))
        Mockito.verify(workManager).scheduleNotification(due.millis)
        freezeAt(now) {
            val overdueJobs = queue.overdueJobs
            assertEquals(
                listOf(
                    AlarmEntry(1, 1, due.millis, TYPE_DATE_TIME),
                    AlarmEntry(2, 2, snooze.millis, TYPE_SNOOZE)
                ),
                overdueJobs
            )
            queue.remove(overdueJobs)
            assertEquals(
                listOf(AlarmEntry(3, 3, due.plusMinutes(1).millis, TYPE_DATE_TIME)),
                queue.getJobs()
            )
        }
    }

    companion object {
        private val ONE_MINUTE = TimeUnit.MINUTES.toMillis(1)
    }
}