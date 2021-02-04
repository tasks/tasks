/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.Recur.Frequency
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.WeekDay.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.date.DateTimeUtils
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.printTimestamp
import java.text.ParseException
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class AdvancedRepeatTest {
    private var task: Task? = null
    private var nextDueDate: Long = 0
    private var recur: Recur? = null

    // --- date with time tests
    @Before
    fun setUp() {
        task = Task()
        task!!.completionDate = DateUtilities.now()
        recur = newRecur()
    }

    @Test
    @Throws(ParseException::class)
    fun testDueDateSpecificTime() {
        buildRecur(1, Frequency.DAILY)

        // test specific day & time
        val dayWithTime = Task.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2010, 8, 1, 10, 4, 0).millis)
        task!!.dueDate = dayWithTime
        val nextDayWithTime = dayWithTime + DateUtilities.ONE_DAY
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task!!, recur!!.toString(), false)
        assertDateTimeEquals(nextDayWithTime, nextDueDate)
    }

    // --- due date tests
    @Test
    @Throws(ParseException::class)
    fun testCompletionDateSpecificTime() {
        buildRecur(1, Frequency.DAILY)

        // test specific day & time
        val dayWithTime = Task.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME, DateTime(2010, 8, 1, 10, 4, 0).millis)
        task!!.dueDate = dayWithTime
        val todayWithTime = DateTimeUtils.newDateTime().withHourOfDay(10).withMinuteOfHour(4).withSecondOfMinute(1)
        var nextDayWithTimeLong = todayWithTime.millis
        nextDayWithTimeLong += DateUtilities.ONE_DAY
        nextDayWithTimeLong = nextDayWithTimeLong / 1000L * 1000
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task!!, recur!!.toString(), true)
        assertDateTimeEquals(nextDayWithTimeLong, nextDueDate)
    }

    /** test multiple days per week - DUE DATE  */
    @Test
    @Throws(Exception::class)
    fun testDueDateInPastSingleWeekMultiDay() {
        buildRecur(1, Frequency.WEEKLY, MO, WE, FR)
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
        buildRecur(1, Frequency.WEEKLY, MO)
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
        buildRecur(1, Frequency.WEEKLY, MO, WE, FR)
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
        buildRecur(2, Frequency.WEEKLY, MO, WE, FR)
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
        for (wday in weekdays) {
            buildRecur(1, Frequency.WEEKLY, wday)
            computeNextDueDate(true)
            val expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, getCalendarDay(wday))
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
            assertEquals(expected, nextDueDate)
        }
        for (wday1 in weekdays) {
            for (wday2 in weekdays) {
                if (wday1 == wday2) {
                    continue
                }
                buildRecur(1, Frequency.WEEKLY, wday1, wday2)
                val nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, getCalendarDay(wday1))
                val nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, getCalendarDay(wday2))
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
        for (wday in weekdays) {
            buildRecur(2, Frequency.WEEKLY, wday)
            computeNextDueDate(true)
            val expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, getCalendarDay(wday))
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
            assertEquals(expected, nextDueDate)
        }
        for (wday1 in weekdays) {
            for (wday2 in weekdays) {
                if (wday1 == wday2) {
                    continue
                }
                buildRecur(2, Frequency.WEEKLY, wday1, wday2)
                val nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, getCalendarDay(wday1))
                val nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, getCalendarDay(wday2))
                computeNextDueDate(true)
                nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate)
                assertEquals(min(nextOne, nextTwo), nextDueDate)
            }
        }
    }

    @Throws(ParseException::class)
    private fun computeNextDueDate(fromComplete: Boolean) {
        nextDueDate = RepeatTaskHelper.computeNextDueDate(task!!, recur!!.toString(), fromComplete)
    }

    private fun buildRecur(interval: Int, freq: Frequency, vararg weekdays: WeekDay) {
        recur!!.interval = interval
        recur!!.setFrequency(freq.name)
        recur!!.dayList.clear()
        recur!!.dayList.addAll(weekdays)
    }

    private fun assertDueDate(actual: Long, expectedWhich: Int, expectedDayOfWeek: Int) {
        val expected = getDate(task!!.dueDate, expectedWhich, expectedDayOfWeek)
        assertEquals(expected, actual)
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

        private val weekdays = listOf(SU, MO, TU, WE, TH, FR, SA)

        fun assertDateTimeEquals(date: Long, other: Long) {
            assertEquals("Expected: ${printTimestamp(date)}, Actual: ${printTimestamp(other)}", date, other)
        }
    }
}