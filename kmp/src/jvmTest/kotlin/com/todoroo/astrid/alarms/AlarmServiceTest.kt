package com.todoroo.astrid.alarms

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.tasks.DatabaseTest
import org.tasks.preferences.AppPreferences
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Task
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.reminders.Random
import org.tasks.time.DateTimeUtils2
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_MINUTE
import org.tasks.time.startOfMinute
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmServiceTest : DatabaseTest() {
    private val alarmDao = db.alarmDao()
    private val taskDao = db.taskDao()
    private val dirtyDao = db.dirtyDao()
    private val notifier: Notifier = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val preferences: AppPreferences = mock {
        onBlocking { isCurrentlyQuietHours() } doReturn false
        onBlocking { defaultDueTime() } doReturn 0
    }

    private val alarmService = AlarmService(
        alarmDao = alarmDao,
        taskDao = taskDao,
        dirtyDao = dirtyDao,
        refreshBroadcaster = mock(),
        notifier = notifier,
        alarmCalculator = AlarmCalculator(Random()),
        preferences = preferences,
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
            mutableSetOf(Alarm(time = currentTimeMillis() - ONE_HOUR, type = TYPE_SNOOZE))
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

    @Test
    fun snoozeSetWhileTheBatchIsStillGoingOutSurvives() = runTest(testDispatcher) {
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = currentTimeMillis() - ONE_HOUR, type = TYPE_SNOOZE)
        )
        val newSnooze = currentTimeMillis() + ONE_HOUR

        val notified = mutableListOf<Long>()
        alarmService.triggerAlarms { entries ->
            alarmDao.insert(Alarm(task = task.id, time = newSnooze, type = TYPE_SNOOZE))
            entries.map { it.taskId }.also { notified.addAll(it) }
        }

        assertEquals(listOf(task.id), notified)

        assertEquals(
            listOf(newSnooze),
            alarmDao.getAlarms(task.id).filter { it.type == TYPE_SNOOZE }.map { it.time },
        )
    }

    @Test
    fun aSnoozeSetInTheCurrentMinuteSurvivesTheScanItWasSetDuring() = runTest(testDispatcher) {
        val scanAt = currentTimeMillis().startOfMinute() + 20_000L
        DateTimeUtils2.setCurrentMillisFixed(scanAt)
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = currentTimeMillis() - ONE_HOUR, type = TYPE_SNOOZE)
        )
        val newSnooze = scanAt.startOfMinute()

        val notified = mutableListOf<Long>()
        alarmService.triggerAlarms { entries ->
            alarmDao.insert(Alarm(task = task.id, time = newSnooze, type = TYPE_SNOOZE))
            entries.map { it.taskId }.also { notified.addAll(it) }
        }

        assertEquals(listOf(task.id), notified)
        assertEquals(
            listOf(newSnooze),
            alarmDao.getAlarms(task.id).filter { it.type == TYPE_SNOOZE }.map { it.time },
        )
    }

    @Test
    fun aSnoozeDueLaterInTheFiringMinuteIsCleared() = runTest(testDispatcher) {
        val scanAt = currentTimeMillis().startOfMinute()
        DateTimeUtils2.setCurrentMillisFixed(scanAt)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = scanAt + 23_000L, type = TYPE_SNOOZE))

        val notified = mutableListOf<Long>()
        alarmService.triggerAlarms { entries ->
            entries.map { it.taskId }.also { notified.addAll(it) }
        }

        assertEquals(listOf(task.id), notified)
        assertEquals(
            emptyList<Long>(),
            alarmDao.getAlarms(task.id).filter { it.type == TYPE_SNOOZE }.map { it.time },
        )
    }

    @Test
    fun aSnoozeIsOnlyClearedForAReminderThatActuallyWentOut() = runTest(testDispatcher) {
        val delivered = createTask()
        val held = createTask()
        val due = currentTimeMillis() - ONE_HOUR
        alarmDao.insert(Alarm(task = delivered.id, time = due, type = TYPE_SNOOZE))
        alarmDao.insert(Alarm(task = held.id, time = due, type = TYPE_SNOOZE))

        alarmService.triggerAlarms { entries ->
            entries.map { it.taskId }.filter { it == delivered.id }
        }

        assertEquals(
            emptyList<Long>(),
            alarmDao.getAlarms(delivered.id).filter { it.type == TYPE_SNOOZE }.map { it.time },
        )
        assertEquals(
            listOf(due),
            alarmDao.getAlarms(held.id).filter { it.type == TYPE_SNOOZE }.map { it.time },
        )
    }

    @Test
    fun aScanThatDeliveredNothingSchedulesNothing() = runTest(testDispatcher) {
        val now = currentTimeMillis().startOfMinute()
        DateTimeUtils2.setCurrentMillisFixed(now)
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = now - ONE_HOUR, type = TYPE_SNOOZE)
        )

        val next = alarmService.triggerAlarms { emptyList() }

        assertEquals(AlarmService.NO_ALARM, next)
    }

    @Test
    fun aScanThatDeliveredEverythingSchedulesNothingExtra() = runTest(testDispatcher) {
        val now = currentTimeMillis().startOfMinute()
        DateTimeUtils2.setCurrentMillisFixed(now)
        val task = createTask()
        alarmDao.insert(Alarm(task = task.id, time = now - ONE_HOUR, type = TYPE_SNOOZE))

        val next = alarmService.triggerAlarms { entries -> entries.map { it.taskId } }

        assertEquals(0L, next)
    }

    @After
    fun restoreClock() {
        DateTimeUtils2.setCurrentMillisSystem()
    }

    private suspend fun createTask(): Task {
        val task = Task()
        taskDao.createNew(task)
        return task
    }

    private fun futureSnooze() =
        Alarm(time = currentTimeMillis() + ONE_HOUR, type = TYPE_SNOOZE)

    companion object {
        private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
    }
}
