package com.todoroo.astrid.repeats

import android.annotation.SuppressLint
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mockito
import org.tasks.Freeze.Companion.freezeClock
import org.tasks.LocalBroadcastManager
import org.tasks.makers.TaskMaker.AFTER_COMPLETE
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import java.text.ParseException

@ExperimentalCoroutinesApi
@SuppressLint("NewApi")
class RepeatTaskHelperTest {
    private lateinit var taskDao: TaskDao
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var alarmService: AlarmService
    private lateinit var gCalHelper: GCalHelper
    private lateinit var helper: RepeatTaskHelper
    private lateinit var mocks: InOrder
    private lateinit var taskCompleter: TaskCompleter

    @Before
    fun setUp() {
        taskDao = Mockito.mock(TaskDao::class.java)
        alarmService = Mockito.mock(AlarmService::class.java)
        gCalHelper = Mockito.mock(GCalHelper::class.java)
        localBroadcastManager = Mockito.mock(LocalBroadcastManager::class.java)
        taskCompleter = Mockito.mock(TaskCompleter::class.java)
        mocks = Mockito.inOrder(alarmService, gCalHelper, localBroadcastManager)
        helper = RepeatTaskHelper(gCalHelper, alarmService, taskDao, localBroadcastManager, taskCompleter)
    }

    @After
    fun after() {
        Mockito.verifyNoMoreInteractions(localBroadcastManager, gCalHelper, alarmService)
    }

    @Test
    fun noRepeat() = runBlockingTest {
        helper.handleRepeat(newTask(with(DUE_TIME, DateTime(2017, 10, 4, 13, 30))))
    }

    @Test
    @Throws(ParseException::class)
    fun testMinutelyRepeat() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=MINUTELY;INTERVAL=30"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 4, 14, 0, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testMinutelyRepeatAfterCompletion() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(COMPLETION_TIME, DateTime(2017, 10, 4, 13, 17, 45, 340)),
                with(RECUR, "RRULE:FREQ=MINUTELY;INTERVAL=30"),
                with(AFTER_COMPLETE, true))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 4, 13, 47, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testMinutelyDecrementCount() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=MINUTELY;COUNT=2;INTERVAL=30"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 4, 14, 0, 1))
        assertEquals(1, newRecur(task.recurrence!!).count)
    }

    @Test
    @Throws(ParseException::class)
    fun testMinutelyLastOccurrence() = runBlockingTest {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=MINUTELY;COUNT=1;INTERVAL=30"))
        helper.handleRepeat(task)
    }

    @Test
    @Throws(ParseException::class)
    fun testHourlyRepeat() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=HOURLY;INTERVAL=6"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 4, 19, 30, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testHourlyRepeatAfterCompletion() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(COMPLETION_TIME, DateTime(2017, 10, 4, 13, 17, 45, 340)),
                with(RECUR, "RRULE:FREQ=HOURLY;INTERVAL=6"),
                with(AFTER_COMPLETE, true))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 4, 19, 17, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testDailyRepeat() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=DAILY;INTERVAL=6"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 10, 13, 30, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatWeeklyNoDays() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=WEEKLY;INTERVAL=2"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2017, 10, 18, 13, 30, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testYearly() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=YEARLY;INTERVAL=3"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2020, 10, 4, 13, 30, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testMonthlyRepeat() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 10, 4, 13, 30)),
                with(RECUR, "RRULE:FREQ=MONTHLY;INTERVAL=3"))
        repeatAndVerify(
                task, DateTime(2017, 10, 4, 13, 30, 1), DateTime(2018, 1, 4, 13, 30, 1))
    }

    @Test
    @Throws(ParseException::class)
    fun testMonthlyRepeatAtEndOfMonth() {
        val task = newTask(
                with(ID, 1L),
                with(DUE_TIME, DateTime(2017, 1, 31, 13, 30)),
                with(RECUR, "RRULE:FREQ=MONTHLY;INTERVAL=1"))
        repeatAndVerify(
                task, DateTime(2017, 1, 31, 13, 30, 1), DateTime(2017, 2, 28, 13, 30, 1))
    }

    @Test
    fun testAlarmShiftWithNoDueDate() {
        val task = newTask(
                with(ID, 1L),
                with(RECUR, "RRULE:FREQ=DAILY")
        )
        freezeClock {
            repeatAndVerify(
                    task,
                    Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime().millis),
                    Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateTime().plusDays(1).millis)
            )
        }
    }

    private fun repeatAndVerify(task: Task, oldDueDate: DateTime, newDueDate: DateTime) =
            repeatAndVerify(task, oldDueDate.millis, newDueDate.millis)

    private fun repeatAndVerify(task: Task, oldDueDate: Long, newDueDate: Long) = runBlockingTest {
        helper.handleRepeat(task)
        mocks.verify(gCalHelper).rescheduleRepeatingTask(task)
        mocks.verify(alarmService).rescheduleAlarms(1, oldDueDate, newDueDate)
        mocks.verify(localBroadcastManager).broadcastRepeat(1, oldDueDate, newDueDate)
    }
}