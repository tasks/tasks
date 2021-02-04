package com.todoroo.astrid.repeats

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task
import net.fortuna.ical4j.model.Recur.Frequency
import net.fortuna.ical4j.model.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.makers.TaskMaker.AFTER_COMPLETE
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.newTask
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import java.text.ParseException

class NewRepeatTests {
    @Test
    @Throws(ParseException::class)
    fun testRepeatMinutelyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 26, 12, 30)
        val task = newFromDue(Frequency.MINUTELY, 1, dueDateTime)
        assertEquals(newDayTime(2016, 8, 26, 12, 31), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatHourlyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 26, 12, 30)
        val task = newFromDue(Frequency.HOURLY, 1, dueDateTime)
        assertEquals(newDayTime(2016, 8, 26, 13, 30), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatDailyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 26, 12, 30)
        val task = newFromDue(Frequency.DAILY, 1, dueDateTime)
        assertEquals(newDayTime(2016, 8, 27, 12, 30), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatWeeklyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 28, 1, 34)
        val task = newFromDue(Frequency.WEEKLY, 1, dueDateTime)
        assertEquals(newDayTime(2016, 9, 4, 1, 34), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatMonthlyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 28, 1, 44)
        val task = newFromDue(Frequency.MONTHLY, 1, dueDateTime)
        assertEquals(newDayTime(2016, 9, 28, 1, 44), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatYearlyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 28, 1, 44)
        val task = newFromDue(Frequency.YEARLY, 1, dueDateTime)
        assertEquals(newDayTime(2017, 8, 28, 1, 44), calculateNextDueDate(task))
    }

    /** Tests for repeating from completionDate  */
    @Test
    @Throws(ParseException::class)
    fun testRepeatMinutelyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.MINUTELY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 29, 0, 15), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatMinutelyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.MINUTELY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 29, 0, 15), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatHourlyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.HOURLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 29, 1, 14), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatHourlyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.HOURLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 29, 1, 14), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatDailyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.DAILY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 30, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatDailyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.DAILY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 8, 30, 0, 4), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatWeeklyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.WEEKLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 9, 5, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatWeeklyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.WEEKLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 9, 5, 0, 4), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatMonthlyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.MONTHLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 9, 29, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatMonthlyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.MONTHLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2016, 9, 29, 0, 4), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatYearlyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 30, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.YEARLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2017, 8, 29, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testRepeatYearlyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 28, 0, 4)
        val completionDateTime = newDayTime(2016, 8, 29, 0, 14)
        val task = newFromCompleted(Frequency.YEARLY, 1, dueDateTime, completionDateTime)
        assertEquals(newDayTime(2017, 8, 29, 0, 4), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testAdvancedRepeatWeeklyFromDueDate() {
        val dueDateTime = newDayTime(2016, 8, 29, 0, 25)
        val task = newWeeklyFromDue(
                1, dueDateTime, WeekDay(WeekDay.MO, 0), WeekDay(WeekDay.WE, 0))
        assertEquals(newDayTime(2016, 8, 31, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testAdvancedRepeatWeeklyFromCompleteDateCompleteBefore() {
        val dueDateTime = newDayTime(2016, 8, 29, 0, 25)
        val completionDateTime = newDayTime(2016, 8, 28, 1, 9)
        val task = newWeeklyFromCompleted(
                1,
                dueDateTime,
                completionDateTime,
                WeekDay(WeekDay.MO, 0),
                WeekDay(WeekDay.WE, 0))
        assertEquals(newDayTime(2016, 8, 29, 0, 25), calculateNextDueDate(task))
    }

    @Test
    @Throws(ParseException::class)
    fun testAdvancedRepeatWeeklyFromCompleteDateCompleteAfter() {
        val dueDateTime = newDayTime(2016, 8, 29, 0, 25)
        val completionDateTime = newDayTime(2016, 9, 1, 1, 9)
        val task = newWeeklyFromCompleted(
                1,
                dueDateTime,
                completionDateTime,
                WeekDay(WeekDay.MO, 0),
                WeekDay(WeekDay.WE, 0))
        assertEquals(newDayTime(2016, 9, 5, 0, 25), calculateNextDueDate(task))
    }

    private fun newDayTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): DateTime {
        return DateTime(
                Task.createDueDate(
                        Task.URGENCY_SPECIFIC_DAY_TIME,
                        DateTime(year, month, day, hour, minute).millis))
    }

    @Throws(ParseException::class)
    private fun calculateNextDueDate(task: Task): DateTime {
        return DateTime(
                RepeatTaskHelper.computeNextDueDate(task, task.recurrence!!, task.repeatAfterCompletion()))
    }

    private fun newFromDue(frequency: Frequency, interval: Int, dueDateTime: DateTime): Task {
        return newTask(
                with(RECUR, getRecurrenceRule(frequency, interval)),
                with(AFTER_COMPLETE, false),
                with(DUE_TIME, dueDateTime))
    }

    private fun newWeeklyFromDue(interval: Int, dueDateTime: DateTime, vararg weekdays: WeekDay): Task {
        return newTask(
                with(RECUR, getRecurrenceRule(Frequency.WEEKLY, interval, *weekdays)),
                with(AFTER_COMPLETE, false),
                with(DUE_TIME, dueDateTime))
    }

    private fun newFromCompleted(
            frequency: Frequency, interval: Int, dueDateTime: DateTime, completionDate: DateTime): Task {
        return newTask(
                with(RECUR, getRecurrenceRule(frequency, interval)),
                with(AFTER_COMPLETE, true),
                with(DUE_TIME, dueDateTime),
                with(COMPLETION_TIME, completionDate))
    }

    private fun newWeeklyFromCompleted(
            interval: Int, dueDateTime: DateTime, completionDate: DateTime, vararg weekdays: WeekDay): Task {
        return newTask(
                with(RECUR, getRecurrenceRule(Frequency.WEEKLY, interval, *weekdays)),
                with(AFTER_COMPLETE, true),
                with(DUE_TIME, dueDateTime),
                with(COMPLETION_TIME, completionDate))
    }

    private fun getRecurrenceRule(
            frequency: Frequency, interval: Int, vararg weekdays: WeekDay): String {
        val rrule = newRecur()
        rrule.setFrequency(frequency.name)
        rrule.interval = interval
        if (weekdays.isNotEmpty()) {
            rrule.dayList.addAll(weekdays)
        }
        return rrule.toString()
    }
}