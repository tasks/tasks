package com.todoroo.astrid.alarms

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.tasks.DatabaseTest
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.reminders.Random
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_HOUR

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmServiceTriggerCharacterizationTest : DatabaseTest() {
    private val alarmDao = db.alarmDao()
    private val taskDao = db.taskDao()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val preferences: AppPreferences = mock {
        onBlocking { isCurrentlyQuietHours() } doReturn false
        onBlocking { defaultDueTime() } doReturn 0
    }

    private val alarmService = AlarmService(
        alarmDao = alarmDao,
        taskDao = taskDao,
        dirtyDao = db.dirtyDao(),
        refreshBroadcaster = mock(),
        notifier = mock(),
        alarmCalculator = AlarmCalculator(Random()),
        preferences = preferences,
    )

    @Test
    fun everythingOverdueIsHandedToTheTriggerStampedNow() = runTest(testDispatcher) {
        val task = overdueTask()
        val before = currentTimeMillis()
        var delivered: List<Notification> = emptyList()

        alarmService.triggerAlarms { delivered = it; it.handled() }

        assertEquals(listOf(task.id), delivered.map { it.taskId })
        assertTrue(delivered.single().timestamp >= before)
    }

    @Test
    fun snoozesAreClearedForEveryOverdueTaskEvenWhenNothingWasDelivered() = runTest(testDispatcher) {
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = currentTimeMillis() - ONE_HOUR, type = TYPE_SNOOZE)
        )

        alarmService.triggerAlarms { it.handled() }

        assertTrue(alarmDao.getAlarms(task.id).none { it.type == TYPE_SNOOZE })
    }

    @Test
    fun aScanThatDeliveredNothingSchedulesNothing() = runTest(testDispatcher) {
        overdueTask()

        val next = alarmService.triggerAlarms { it.handled() }

        assertEquals(0L, next)
    }

    @Test
    fun theNextFutureAlarmIsReturned() = runTest(testDispatcher) {
        overdueTask()
        val later = createTask()
        val future = currentTimeMillis() + 2 * ONE_HOUR
        alarmDao.insert(Alarm(task = later.id, time = future, type = TYPE_DATE_TIME))

        val next = alarmService.triggerAlarms { it.handled() }

        assertEquals(future, next)
    }

    @Test
    fun quietHoursTriggerNothingAndReturnTheEndOfTheQuietPeriod() = runTest(testDispatcher) {
        val wakeUp = currentTimeMillis() + 8 * ONE_HOUR
        preferences.stub {
            onBlocking { isCurrentlyQuietHours() } doReturn true
            onBlocking { adjustForQuietHours(org.mockito.kotlin.any()) } doReturn wakeUp
        }
        val task = createTask()
        alarmDao.insert(
            Alarm(task = task.id, time = currentTimeMillis() - ONE_HOUR, type = TYPE_SNOOZE)
        )
        var triggered = false

        val next = alarmService.triggerAlarms { triggered = true; it.handled() }

        assertEquals(wakeUp, next)
        assertTrue(!triggered)
        assertTrue(alarmDao.getAlarms(task.id).any { it.type == TYPE_SNOOZE })
    }

    private fun List<Notification>.handled(): Collection<Long> = map { it.taskId }

    private suspend fun overdueTask(): Task = createTask().also {
        alarmDao.insert(
            Alarm(task = it.id, time = currentTimeMillis() - ONE_HOUR, type = TYPE_DATE_TIME)
        )
    }

    private suspend fun createTask(): Task = Task().also { taskDao.createNew(it) }
}
