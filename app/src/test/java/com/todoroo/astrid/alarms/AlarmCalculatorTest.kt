package com.todoroo.astrid.alarms

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities.ONE_WEEK
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_DUE
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_DUE_TIME
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.Alarm.Companion.TYPE_REL_END
import org.tasks.data.Alarm.Companion.TYPE_REL_START
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.makers.AlarmEntryMaker.TIME
import org.tasks.makers.AlarmEntryMaker.TYPE
import org.tasks.makers.AlarmEntryMaker.newAlarmEntry
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.REMINDER_LAST
import org.tasks.makers.TaskMaker.newTask
import org.tasks.reminders.Random
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MINUTES

class AlarmCalculatorTest {
    private lateinit var random: RandomStub
    private lateinit var alarmCalculator: AlarmCalculator
    private val now = newDateTime()

    @Before
    fun setUp() {
        random = RandomStub()
        alarmCalculator = AlarmCalculator(
            isDefaultDueTimeEnabled = true,
            random = random,
            defaultDueTime = TimeUnit.HOURS.toMillis(13).toInt(),
        )
    }

    @Test
    fun ignoreOldReminder() {
        assertNull(
            alarmCalculator.toAlarmEntry(
                newTask(with(REMINDER_LAST, now)),
                Alarm(0L, now.millis, TYPE_DATE_TIME)
            )
        )
    }

    @Test
    fun dateTimeReminder() {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(REMINDER_LAST, now)),
            Alarm(0L, now.millis + 1, TYPE_DATE_TIME)
        )

        assertEquals(newAlarmEntry(with(TIME, now.plusMillis(1)), with(TYPE, TYPE_DATE_TIME)), alarm)
    }

    @Test
    fun dontIgnoreOldSnooze() {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(REMINDER_LAST, now)),
            Alarm(0L, now.millis, TYPE_SNOOZE)
        )

        assertEquals(
            newAlarmEntry(with(TIME, now), with(TYPE, TYPE_SNOOZE)),
            alarm
        )
    }

    @Test
    fun scheduleReminderAtDefaultDue() {
        val alarm = alarmCalculator.toAlarmEntry(newTask(with(DUE_DATE, now)), whenDue(0L))

        assertEquals(
            newAlarmEntry(
                with(TIME, now.startOfDay().withHourOfDay(13)),
                with(TYPE, TYPE_REL_END)
            ),
            alarm
        )
    }

    @Test
    fun scheduleReminderAtDefaultDueTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(newTask(with(DUE_TIME, now)), whenDue(0L))

        assertEquals(
            newAlarmEntry(
                with(TIME, now.startOfMinute().plusMillis(1000)), with(TYPE, TYPE_REL_END)
            ),
            alarm
        )
    }

    @Test
    fun scheduleReminderAtDefaultStart() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_DATE, now), with(HIDE_TYPE, HIDE_UNTIL_DUE)),
            whenStarted(0L)
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.startOfDay().withHourOfDay(13)),
                with(TYPE, TYPE_REL_START)
            ),
            alarm
        )
    }

    @Test
    fun scheduleReminerAtDefaultStartTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME)),
            whenStarted(0L)
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.startOfMinute().plusMillis(1000)),
                with(TYPE, TYPE_REL_START)
            ),
            alarm
        )
    }

    @Test
    fun scheduleRelativeAfterDue() {
        freezeAt(DateTime(2023, 11, 3, 17, 13)) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(with(DUE_DATE, newDateTime())),
                Alarm(0L, DAYS.toMillis(1), TYPE_REL_END)
            )

            assertEquals(
                newAlarmEntry(
                    with(TIME, DateTime(2023, 11, 4, 13, 0, 0)),
                    with(TYPE, TYPE_REL_END)
                ),
                alarm
            )
        }
    }

    @Test
    fun scheduleRelativeAfterDueTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now)),
            Alarm(0, DAYS.toMillis(1), TYPE_REL_END)
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.plusDays(1).startOfMinute().plusMillis(1000)),
                with(TYPE, TYPE_REL_END)
            ),
            alarm
        )
    }

    @Test
    fun scheduleRelativeAfterStart() = runBlocking {
        freezeAt(DateTime(2023, 11, 3, 17, 13)) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(with(DUE_DATE, newDateTime()), with(HIDE_TYPE, HIDE_UNTIL_DUE)),
                Alarm(0, DAYS.toMillis(1), TYPE_REL_START)
            )

            assertEquals(
                newAlarmEntry(
                    with(TIME, DateTime(2023, 11, 4, 13, 0)),
                    with(TYPE, TYPE_REL_START)
                ),
                alarm
            )
        }
    }

    @Test
    fun scheduleRelativeAfterStartTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME)),
            Alarm(0, DAYS.toMillis(1), TYPE_REL_START)
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.plusDays(1).startOfMinute().plusMillis(1000)),
                with(TYPE, TYPE_REL_START)
            ),
            alarm
        )
    }

    @Test
    fun scheduleFirstRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(4))),
            Alarm(0, 0, TYPE_REL_END, 1, MINUTES.toMillis(5))
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.plusMinutes(5).startOfMinute().plusMillis(1000)),
                with(TYPE, TYPE_REL_END)
            ),
            alarm
        )
    }

    @Test
    fun scheduleSecondRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(6))),
            Alarm(0, 0, TYPE_REL_END, 2, MINUTES.toMillis(5))
        )

        assertEquals(
            newAlarmEntry(
                with(TIME, now.plusMinutes(10).startOfMinute().plusMillis(1000)),
                with(TYPE, TYPE_REL_END)
            ),
            alarm
        )
    }

    @Test
    fun terminateRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(10))),
            Alarm(0L, 0, TYPE_REL_END, 2, MINUTES.toMillis(5))
        )

        assertNull(alarm)
    }

    @Test
    fun dontScheduleRelativeEndWithNoEnd() = runBlocking {
        assertNull(alarmCalculator.toAlarmEntry(newTask(), whenDue(0L)))
    }

    @Test
    fun dontScheduleRelativeStartWithNoStart() = runBlocking {
        assertNull(
            alarmCalculator.toAlarmEntry(
                newTask(with(DUE_DATE, newDateTime())),
                whenStarted(0L)
            )
        )
    }

    @Test
    fun reminderOverdueEveryDay() = runBlocking {
        val dueDate =
            Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis)
                .toDateTime()
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, dueDate), with(REMINDER_LAST, dueDate.plusDays(6))),
            whenOverdue(0L)
        )

        assertEquals(
            newAlarmEntry(with(TIME, dueDate.plusDays(7)), with(TYPE, TYPE_REL_END)),
            alarm
        )
    }

    @Test
    fun endDailyOverdueReminder() = runBlocking {
        val dueDate =
            Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis)
                .toDateTime()
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, dueDate), with(REMINDER_LAST, dueDate.plusDays(7))),
            whenOverdue(0L)
        )

        assertNull(alarm)
    }

    @Test
    fun scheduleOverdueRandomReminder() {
        random.seed = 0.3865f
        freezeAt(now) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(
                    with(REMINDER_LAST, now.minusDays(14)),
                    with(CREATION_TIME, now.minusDays(30)),
                ),
                Alarm(0L, ONE_WEEK, TYPE_RANDOM)
            )

            assertEquals(
                newAlarmEntry(with(TIME, now.plusMillis(10148400)), with(TYPE, TYPE_RANDOM)),
                alarm
            )
        }
    }

    @Test
    fun scheduleInitialRandomReminder() {
        random.seed = 0.3865f

        freezeAt(now) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(
                    with(REMINDER_LAST, null as DateTime?),
                    with(CREATION_TIME, now.minusDays(1)),
                ),
                Alarm(0L, ONE_WEEK, TYPE_RANDOM)
            )

            assertEquals(
                newAlarmEntry(
                    with(TIME, now.minusDays(1).plusMillis(584206592)),
                    with(TYPE, TYPE_RANDOM)
                ),
                alarm
            )
        }
    }

    @Test
    fun scheduleNextRandomReminder() {
        random.seed = 0.3865f

        freezeAt(now) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(
                    with(REMINDER_LAST, now.minusDays(1)),
                    with(CREATION_TIME, now.minusDays(30)),
                ),
                Alarm(0L, ONE_WEEK, TYPE_RANDOM)
            )

            assertEquals(
                newAlarmEntry(
                    with(TIME, now.minusDays(1).plusMillis(584206592)),
                    with(TYPE, TYPE_RANDOM)
                ),
                alarm
            )
        }
    }

    internal class RandomStub : Random() {
        var seed = 1.0f

        override fun nextFloat() = seed
    }
}