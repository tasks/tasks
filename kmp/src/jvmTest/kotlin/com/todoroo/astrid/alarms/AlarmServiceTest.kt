package com.todoroo.astrid.alarms

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.tasks.DatabaseTest
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Task
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.reminders.Random
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmServiceTest : DatabaseTest() {
    private val alarmDao = db.alarmDao()
    private val taskDao = db.taskDao()
    private val notifier: Notifier = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val alarmService = AlarmService(
        alarmDao = alarmDao,
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = notifier,
        alarmCalculator = AlarmCalculator(Random(), defaultDueTime = 0),
        preferences = mock(),
    )

    @Test
    fun cancelNotificationWhenFutureSnoozeSynchronized() = runTest(testDispatcher) {
        val task = createTask()

        alarmService.synchronizeAlarms(task.id, mutableSetOf(futureSnooze()))

        verifyBlocking(notifier) { cancel(listOf(task.id), CancelReason.SNOOZE) }
    }

    @Test
    fun dontCancelNotificationForPastSnooze() = runTest(testDispatcher) {
        val task = createTask()

        alarmService.synchronizeAlarms(
            task.id,
            mutableSetOf(
                Alarm(time = currentTimeMillis() - TimeUnit.HOURS.toMillis(1), type = TYPE_SNOOZE)
            )
        )

        verifyBlocking(notifier, never()) { cancel(any<List<Long>>(), eq(CancelReason.SNOOZE)) }
    }

    @Test
    fun dontCancelNotificationForNonSnoozeAlarm() = runTest(testDispatcher) {
        val task = createTask()

        alarmService.synchronizeAlarms(task.id, mutableSetOf(Alarm(type = TYPE_REL_END)))

        verifyBlocking(notifier, never()) { cancel(any<List<Long>>(), eq(CancelReason.SNOOZE)) }
    }

    @Test
    fun dontCancelNotificationWhenSnoozeUnchanged() = runTest(testDispatcher) {
        val task = createTask()
        val snooze = futureSnooze()
        alarmService.synchronizeAlarms(task.id, mutableSetOf(snooze))

        // re-applying the same snooze is not a change, so nothing should be cancelled again
        alarmService.synchronizeAlarms(task.id, mutableSetOf(snooze))

        // the only cancel should be the one from the first (new) snooze
        verifyBlocking(notifier) { cancel(listOf(task.id), CancelReason.SNOOZE) }
    }

    private suspend fun createTask(): Task {
        val task = Task()
        taskDao.createNew(task)
        return task
    }

    private fun futureSnooze() =
        Alarm(time = currentTimeMillis() + TimeUnit.HOURS.toMillis(1), type = TYPE_SNOOZE)
}
