package org.tasks.jobs

import com.todoroo.astrid.reminders.ReminderService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.AdditionalAnswers
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.tasks.Freeze.Companion.freezeAt
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
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong())).then(AdditionalAnswers.returnsFirstArg<Any>())
        workManager = Mockito.mock(WorkManager::class.java)
        queue = NotificationQueue(preferences, workManager)
    }

    @After
    fun after() {
        Mockito.verifyNoMoreInteractions(workManager)
    }

    @Test
    fun alarmAndReminderSameTimeSameID() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(AlarmEntry(1, 1, now))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                    setOf(AlarmEntry(1, 1, now), ReminderEntry(1, now, ReminderService.TYPE_DUE)),
                    queue.overdueJobs.toSet())
        }
    }

    @Test
    fun alarmAndReminderSameTimeDifferentId() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(AlarmEntry(1, 2, now))
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                    setOf(AlarmEntry(1, 2, now), ReminderEntry(1, now, ReminderService.TYPE_DUE)),
                    queue.overdueJobs.toSet())
        }
    }

    @Test
    fun removeAlarmLeaveReminder() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(AlarmEntry(1, 1, now))
        Mockito.verify(workManager).scheduleNotification(now)
        queue.remove(listOf(AlarmEntry(1, 1, now)))
        freezeAt(now) {
            assertEquals(
                    listOf(ReminderEntry(1, now, ReminderService.TYPE_DUE)), queue.overdueJobs)
        }
    }

    @Test
    fun removeReminderLeaveAlarm() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(AlarmEntry(1, 1, now))
        Mockito.verify(workManager).scheduleNotification(now)
        queue.remove(listOf(ReminderEntry(1, now, ReminderService.TYPE_DUE)))
        freezeAt(now) {
            assertEquals(listOf(AlarmEntry(1, 1, now)), queue.overdueJobs)
        }
    }

    @Test
    fun twoJobsAtSameTime() {
        queue.add(ReminderEntry(1, 1, 0))
        queue.add(ReminderEntry(2, 1, 0))
        Mockito.verify(workManager).scheduleNotification(1)
        assertEquals(2, queue.size())
    }

    @Test
    fun rescheduleForFirstJob() {
        queue.add(ReminderEntry(1, 1, 0))
        Mockito.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun dontRescheduleForLaterJobs() {
        queue.add(ReminderEntry(1, 1, 0))
        queue.add(ReminderEntry(2, 2, 0))
        Mockito.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun rescheduleForNewerJob() {
        queue.add(ReminderEntry(1, 2, 0))
        queue.add(ReminderEntry(1, 1, 0))
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(2)
        order.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun rescheduleWhenCancelingOnlyJob() {
        queue.add(ReminderEntry(1, 2, 0))
        queue.cancelReminder(1)
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(2)
        order.verify(workManager).cancelNotifications()
    }

    @Test
    fun rescheduleWhenCancelingFirstJob() {
        queue.add(ReminderEntry(1, 1, 0))
        queue.add(ReminderEntry(2, 2, 0))
        queue.cancelReminder(1)
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(1)
        order.verify(workManager).scheduleNotification(2)
    }

    @Test
    fun dontRescheduleWhenCancelingLaterJob() {
        queue.add(ReminderEntry(1, 1, 0))
        queue.add(ReminderEntry(2, 2, 0))
        queue.cancelReminder(2)
        Mockito.verify(workManager).scheduleNotification(1)
    }

    @Test
    fun nextScheduledTimeIsZeroWhenQueueIsEmpty() {
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong())).thenReturn(1234L)
        assertEquals(0, queue.nextScheduledTime())
    }

    @Test
    fun adjustNextScheduledTimeForQuietHours() {
        Mockito.`when`(preferences.adjustForQuietHours(ArgumentMatchers.anyLong())).thenReturn(1234L)
        queue.add(ReminderEntry(1, 1, 1))
        Mockito.verify(workManager).scheduleNotification(1234)
    }

    @Test
    fun overdueJobsAreReturned() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                    listOf(ReminderEntry(1, now, ReminderService.TYPE_DUE)), queue.overdueJobs)
        }
    }

    @Test
    fun twoOverdueJobsAtSameTimeReturned() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, now, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            assertEquals(
                    listOf(
                            ReminderEntry(1, now, ReminderService.TYPE_DUE), ReminderEntry(2, now, ReminderService.TYPE_DUE)),
                    queue.overdueJobs)
        }
    }

    @Test
    fun twoOverdueJobsAtDifferentTimes() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now + 2 * ONE_MINUTE) {
            assertEquals(
                    listOf(
                            ReminderEntry(1, now, ReminderService.TYPE_DUE),
                            ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE)),
                    queue.overdueJobs)
        }
    }

    @Test
    fun overdueJobsAreRemoved() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now) {
            queue.remove(queue.overdueJobs)
        }
        assertEquals(listOf(ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE)), queue.getJobs())
    }

    @Test
    fun multipleOverduePeriodsLapsed() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, now + ONE_MINUTE, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(3, now + 2 * ONE_MINUTE, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(now)
        freezeAt(now + ONE_MINUTE) {
            queue.remove(queue.overdueJobs)
        }
        assertEquals(
                listOf(ReminderEntry(3, now + 2 * ONE_MINUTE, ReminderService.TYPE_DUE)), queue.getJobs())
    }

    @Test
    fun clearShouldCancelExisting() {
        queue.add(ReminderEntry(1, 1, 0))
        queue.clear()
        val order = Mockito.inOrder(workManager)
        order.verify(workManager).scheduleNotification(1)
        order.verify(workManager).cancelNotifications()
        assertEquals(0, queue.size())
    }

    @Test
    fun ignoreInvalidCancel() {
        val now = DateTimeUtils.currentTimeMillis()
        queue.add(ReminderEntry(1, now, ReminderService.TYPE_DUE))
        queue.cancelReminder(2)
        Mockito.verify(workManager).scheduleNotification(now)
    }

    @Test
    fun allDuringSameMinuteAreOverdue() {
        val now = DateTime(2017, 9, 3, 0, 14, 6, 455)
        val due = DateTime(2017, 9, 3, 0, 14, 0, 0)
        val snooze = DateTime(2017, 9, 3, 0, 14, 59, 999)
        queue.add(ReminderEntry(1, due.millis, ReminderService.TYPE_DUE))
        queue.add(ReminderEntry(2, snooze.millis, ReminderService.TYPE_SNOOZE))
        queue.add(ReminderEntry(3, due.plusMinutes(1).millis, ReminderService.TYPE_DUE))
        Mockito.verify(workManager).scheduleNotification(due.millis)
        freezeAt(now) {
            val overdueJobs = queue.overdueJobs
            assertEquals(
                    listOf(
                            ReminderEntry(1, due.millis, ReminderService.TYPE_DUE),
                            ReminderEntry(2, snooze.millis, ReminderService.TYPE_SNOOZE)),
                    overdueJobs)
            queue.remove(overdueJobs)
            assertEquals(
                    listOf(ReminderEntry(3, due.plusMinutes(1).millis, ReminderService.TYPE_DUE)),
                    queue.getJobs())
        }
    }

    companion object {
        private val ONE_MINUTE = TimeUnit.MINUTES.toMillis(1)
    }
}