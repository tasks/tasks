/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import com.google.ical.values.Frequency
import com.google.ical.values.RRule
import com.google.ical.values.Weekday
import com.google.ical.values.WeekdayNum
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.date.DateTimeUtils
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.printTimestamp
import java.text.ParseException
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class AdvancedRepeatTest {
    private var task: Task? = null
    private var nextDueDate: Long = 0
    private var rrule: RRule? = null

    // --- date with time tests
    @Before
    fun setUp() {
        task = Task()
        task!!.completionDate = DateUtilities.now()
        rrule = RRule()
    }

    @Test
    @Throws(ParseException::class)
    fun testDueDateSpecificTime() {
        buildRRule(1, Frequency.DAILY)

        // test specific day & time
        val dayWithTime = Task.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2010, 8, 1, 10, 4, 0).millis)
        task!!.dueDate = dayWithTime
        val nextDayWithTime = dayWithTime + DateUtilities.ONE_DAY
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task, rrule!!.toIcal(), false)
        assertDateTimeEquals(nextDayWithTime, nextDueDate)
    }

    // --- due date tests
    @Test
    @Throws(ParseException::class)
    fun testCompletionDateSpecificTime() {
        buildRRule(1, Frequency.DAILY)

        // test specific day & time
        val dayWithTime = Task.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2010, 8, 1, 10, 4, 0).millis)
        task!!.dueDate = dayWithTime
        val todayWithTime = DateTimeUtils.newDateTime().withHourOfDay(10).withMinuteOfHour(4).withSecondOfMinute(1)
        var nextDayWithTimeLong = todayWithTime.millis
        nextDayWithTimeLong += DateUtilities.ONE_DAY
        nextDayWithTimeLong = nextDayWithTimeLong / 1000L * 1000
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task, rrule!!.toIcal(), true)
        assertDateTimeEquals(nextDayWithTimeLong, nextDueDate)
    }

    /** test multiple days per week - DUE DATE  */
    @Test
    @Throws(Exception::class)
    fun testDueDateInPastSingleWeekMultiDay() {
        buildRRule(1, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR)
        setTaskDueDate(THIS, Calendar.SUNDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
        setTaskDueDate(THIS, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY)
        setTaskDueDate(THIS, Calendar.FRIDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
    }

    /** test single day repeats - DUE DATE  */
    @Test
    @Throws(Exception::class)
    fun testDueDateSingleDay() {
        buildRRule(1, Frequency.WEEKLY, Weekday.MO)
        setTaskDueDate(PREV_PREV, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY)
        setTaskDueDate(PREV_PREV, Calendar.FRIDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
        setTaskDueDate(PREV, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY)
        setTaskDueDate(PREV, Calendar.FRIDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
        setTaskDueDate(THIS, Calendar.SUNDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
        setTaskDueDate(THIS, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY)
    }

    /** test multiple days per week - DUE DATE  */
    @Test
    @Throws(Exception::class)
    fun testDueDateSingleWeekMultiDay() {
        buildRRule(1, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR)
        setTaskDueDate(THIS, Calendar.SUNDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
        setTaskDueDate(THIS, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY)
        setTaskDueDate(THIS, Calendar.FRIDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY)
    }
    // --- completion tests
    /** test multiple days per week, multiple intervals - DUE DATE  */
    @Test
    @Throws(Exception::class)
    fun testDueDateMultiWeekMultiDay() {
        buildRRule(2, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR)
        setTaskDueDate(THIS, Calendar.SUNDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY)
        setTaskDueDate(THIS, Calendar.MONDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY)
        setTaskDueDate(THIS, Calendar.FRIDAY)
        computeNextDueDate(false)
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY)
    }

    /** test multiple days per week - COMPLETE DATE  */
    @Test
    @Throws(Exception::class)
    fun testCompleteDateSingleWeek() {
        for (wday in Weekday.values()) {
            buildRRule(1, Frequency.WEEKLY, wday)
            computeNextDueDate(true)
            val expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday.javaDayNum)
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
            assertEquals(expected, nextDueDate)
        }
        for (wday1 in Weekday.values()) {
            for (wday2 in Weekday.values()) {
                if (wday1 == wday2) {
                    continue
                }
                buildRRule(1, Frequency.WEEKLY, wday1, wday2)
                val nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday1.javaDayNum)
                val nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday2.javaDayNum)
                computeNextDueDate(true)
                nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
                assertEquals(min(nextOne, nextTwo), nextDueDate)
            }
        }
    }
    // --- helpers
    /** test multiple days per week, multiple intervals - COMPLETE DATE  */
    @Test
    @Throws(Exception::class)
    fun testCompleteDateMultiWeek() {
        for (wday in Weekday.values()) {
            buildRRule(2, Frequency.WEEKLY, wday)
            computeNextDueDate(true)
            val expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday.javaDayNum)
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
            assertEquals(expected, nextDueDate)
        }
        for (wday1 in Weekday.values()) {
            for (wday2 in Weekday.values()) {
                if (wday1 == wday2) {
                    continue
                }
                buildRRule(2, Frequency.WEEKLY, wday1, wday2)
                val nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday1.javaDayNum)
                val nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday2.javaDayNum)
                computeNextDueDate(true)
                nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
                assertEquals(min(nextOne, nextTwo), nextDueDate)
            }
        }
    }

    @Throws(ParseException::class)
    private fun computeNextDueDate(fromComplete: Boolean) {
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task, rrule!!.toIcal(), fromComplete)
    }

    private fun buildRRule(interval: Int, freq: Frequency, vararg weekdays: Weekday) {
        rrule!!.interval = interval
        rrule!!.freq = freq
        setRRuleDays(rrule, *weekdays)
    }

    private fun assertDueDate(actual: Long, expectedWhich: Int, expectedDayOfWeek: Int) {
        val expected = getDate(task!!.dueDate, expectedWhich, expectedDayOfWeek)
        assertEquals(expected, actual)
    }

    private fun setRRuleDays(rrule: RRule?, vararg weekdays: Weekday) {
        val days = ArrayList<WeekdayNum>()
        for (wd in weekdays) {
            days.add(WeekdayNum(0, wd))
        }
        rrule!!.byDay = days
    }

    private fun setTaskDueDate(which: Int, day: Int) {
        val time = getDate(DateUtilities.now(), which, day)
        task!!.dueDate = time
    }

    private fun getDate(start: Long, which: Int, dayOfWeek: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = start
        val direction = if (which > 0) 1 else -1
        while (c[Calendar.DAY_OF_WEEK] != dayOfWeek) {
            c.add(Calendar.DAY_OF_MONTH, direction)
        }
        c.add(Calendar.DAY_OF_MONTH, (abs(which) - 1) * direction * 7)
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, c.timeInMillis)
    }

    companion object {
        private const val PREV_PREV = -2
        private const val PREV = -1
        private const val THIS = 1
        private const val NEXT = 2

        fun assertDateTimeEquals(date: Long, other: Long) {
            assertEquals("Expected: ${printTimestamp(date)}, Actual: ${printTimestamp(other)}", date, other)
        }
    }
}