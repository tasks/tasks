package com.todoroo.astrid.alarms

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities.ONE_WEEK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.data.createDueDate
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL_DUE
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL_DUE_TIME
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
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
                Alarm(time = now.millis, type = TYPE_DATE_TIME)
            )
        )
    }

    @Test
    fun dateTimeReminder() {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(REMINDER_LAST, now)),
            Alarm(time = now.millis + 1, type = TYPE_DATE_TIME)
        )

        assertEquals(Notification(timestamp = now.millis + 1, type = TYPE_DATE_TIME), alarm)
    }

    @Test
    fun dontIgnoreOldSnooze() {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(REMINDER_LAST, now)),
            Alarm(time = now.millis, type = TYPE_SNOOZE)
        )

        assertEquals(Notification(timestamp = now.millis, type = TYPE_SNOOZE), alarm)
    }

    @Test
    fun scheduleReminderAtDefaultDue() {
        val alarm = alarmCalculator.toAlarmEntry(newTask(with(DUE_DATE, now)), whenDue(0L))

        assertEquals(
            Notification(
                timestamp = now.startOfDay().withHourOfDay(13).millis,
                type = TYPE_REL_END
            ),
            alarm
        )
    }

    @Test
    fun scheduleReminderAtDefaultDueTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(newTask(with(DUE_TIME, now)), whenDue(0L))

        assertEquals(
            Notification(
                timestamp = now.startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_END
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
            Notification(
                timestamp = now.startOfDay().withHourOfDay(13).millis,
                type = TYPE_REL_START
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
            Notification(
                timestamp = now.startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_START
            ),
            alarm
        )
    }

    @Test
    fun scheduleRelativeAfterDue() {
        freezeAt(DateTime(2023, 11, 3, 17, 13)) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(with(DUE_DATE, newDateTime())),
                Alarm(time = DAYS.toMillis(1), type = TYPE_REL_END)
            )

            assertEquals(
                Notification(
                    timestamp = DateTime(2023, 11, 4, 13, 0, 0).millis,
                    type = TYPE_REL_END
                ),
                alarm
            )
        }
    }

    @Test
    fun scheduleRelativeAfterDueTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now)),
            Alarm(time = DAYS.toMillis(1), type = TYPE_REL_END)
        )

        assertEquals(
            Notification(
                timestamp = now.plusDays(1).startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_END
            ),
            alarm
        )
    }

    @Test
    fun scheduleRelativeAfterStart() = runBlocking {
        freezeAt(DateTime(2023, 11, 3, 17, 13)) {
            val alarm = alarmCalculator.toAlarmEntry(
                newTask(with(DUE_DATE, newDateTime()), with(HIDE_TYPE, HIDE_UNTIL_DUE)),
                Alarm(time = DAYS.toMillis(1), type = TYPE_REL_START)
            )

            assertEquals(
                Notification(
                    timestamp = DateTime(2023, 11, 4, 13, 0).millis,
                    type = TYPE_REL_START
                ),
                alarm
            )
        }
    }

    @Test
    fun scheduleRelativeAfterStartTime() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME)),
            Alarm(time = DAYS.toMillis(1), type = TYPE_REL_START)
        )

        assertEquals(
            Notification(
                timestamp = now.plusDays(1).startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_START
            ),
            alarm
        )
    }

    @Test
    fun scheduleFirstRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(4))),
            Alarm(type = TYPE_REL_END, repeat = 1, interval = MINUTES.toMillis(5))
        )

        assertEquals(
            Notification(
                timestamp = now.plusMinutes(5).startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_END
            ),
            alarm
        )
    }

    @Test
    fun scheduleSecondRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(6))),
            Alarm(type = TYPE_REL_END, repeat = 2, interval = MINUTES.toMillis(5))
        )

        assertEquals(
            Notification(
                timestamp = now.plusMinutes(10).startOfMinute().plusMillis(1000).millis,
                type = TYPE_REL_END
            ),
            alarm
        )
    }

    @Test
    fun terminateRepeatReminder() = runBlocking {
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, now), with(REMINDER_LAST, now.plusMinutes(10))),
            Alarm(type = TYPE_REL_END, repeat = 2, interval = MINUTES.toMillis(5))
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
            createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis)
                .toDateTime()
        val alarm = alarmCalculator.toAlarmEntry(
            newTask(with(DUE_TIME, dueDate), with(REMINDER_LAST, dueDate.plusDays(6))),
            whenOverdue(0L)
        )

        assertEquals(
            Notification(timestamp = dueDate.plusDays(7).millis, type = TYPE_REL_END),
            alarm
        )
    }

    @Test
    fun endDailyOverdueReminder() = runBlocking {
        val dueDate =
            createDueDate(URGENCY_SPECIFIC_DAY_TIME, DateTime(2022, 1, 30, 13, 30).millis)
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
                Alarm(time = ONE_WEEK, type = TYPE_RANDOM)
            )

            assertEquals(
                Notification(
                    timestamp = now.minusDays(14).plusMillis(584206592).millis,
                    type = TYPE_RANDOM),
                alarm
            )
        }
    }

    @Test
    fun scheduleOverdueRandomReminderForHiddenTask() {
        random.seed = 0.3865f
        freezeAt(now) {
            val task = newTask(
                with(REMINDER_LAST, now.minusDays(14)),
                with(CREATION_TIME, now.minusDays(30)),
                with(DUE_TIME, now.plusHours(1)),
                with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME),
            )
            val alarm = alarmCalculator.toAlarmEntry(
                task,
                Alarm(time = ONE_WEEK, type = TYPE_RANDOM)
            )

            assertEquals(Notification(timestamp = task.dueDate, type = TYPE_RANDOM), alarm)
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
                Alarm(time = ONE_WEEK, type = TYPE_RANDOM)
            )

            assertEquals(
                Notification(
                    timestamp = now.minusDays(1).plusMillis(584206592).millis,
                    type = TYPE_RANDOM
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
                Alarm(time = ONE_WEEK, type = TYPE_RANDOM)
            )

            assertEquals(
                Notification(
                    timestamp = now.minusDays(1).plusMillis(584206592).millis,
                    type = TYPE_RANDOM
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