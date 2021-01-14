/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import com.google.ical.iter.RecurrenceIteratorFactory
import com.google.ical.values.*
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.gcal.GCalHelper
import org.tasks.LocalBroadcastManager
import org.tasks.date.DateTimeUtils.newDate
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.newDateUtc
import org.tasks.time.DateTime
import timber.log.Timber
import java.text.ParseException
import java.util.*
import javax.inject.Inject

class RepeatTaskHelper @Inject constructor(
        private val gcalHelper: GCalHelper,
        private val alarmService: AlarmService,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager) {
    suspend fun handleRepeat(task: Task) {
        val recurrence = task.sanitizedRecurrence()
        val repeatAfterCompletion = task.repeatAfterCompletion()
        if (!recurrence.isNullOrBlank()) {
            val newDueDate: Long
            val rrule: RRule
            try {
                rrule = initRRule(task.getRecurrenceWithoutFrom())
                newDueDate = computeNextDueDate(task, recurrence, repeatAfterCompletion)
                if (newDueDate == -1L) {
                    return
                }
            } catch (e: ParseException) {
                Timber.e(e)
                return
            }
            val oldDueDate = task.dueDate
            val repeatUntil = task.repeatUntil
            if (repeatFinished(newDueDate, repeatUntil)) {
                return
            }
            val count = rrule.count
            if (count == 1) {
                return
            }
            if (count > 1) {
                rrule.count = count - 1
                task.setRecurrence(rrule, repeatAfterCompletion)
            }
            task.reminderLast = 0L
            task.reminderSnooze = 0L
            task.completionDate = 0L
            task.setDueDateAdjustingHideUntil(newDueDate)
            gcalHelper.rescheduleRepeatingTask(task)
            taskDao.save(task)
            val previousDueDate =
                    oldDueDate
                            .takeIf { it > 0 }
                            ?: newDueDate - (computeNextDueDate(task, recurrence, repeatAfterCompletion) - newDueDate)
            alarmService.rescheduleAlarms(task.id, previousDueDate, newDueDate)
            localBroadcastManager.broadcastRepeat(task.id, previousDueDate, newDueDate)
        }
    }

    companion object {
        private val weekdayCompare = Comparator { object1: WeekdayNum, object2: WeekdayNum -> object1.wday.javaDayNum - object2.wday.javaDayNum }
        private fun repeatFinished(newDueDate: Long, repeatUntil: Long): Boolean {
            return (repeatUntil > 0
                    && newDateTime(newDueDate).startOfDay().isAfter(newDateTime(repeatUntil).startOfDay()))
        }

        /** Compute next due date  */
        @Throws(ParseException::class)
        fun computeNextDueDate(task: Task, recurrence: String?, repeatAfterCompletion: Boolean): Long {
            val rrule = initRRule(recurrence)

            // initialize startDateAsDV
            val original = setUpStartDate(task, repeatAfterCompletion, rrule.freq)
            val startDateAsDV = setUpStartDateAsDV(task, original)
            return if (rrule.freq == Frequency.HOURLY || rrule.freq == Frequency.MINUTELY) {
                handleSubdayRepeat(original, rrule)
            } else if (rrule.freq == Frequency.WEEKLY && rrule.byDay.size > 0 && repeatAfterCompletion) {
                handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime())
            } else if (rrule.freq == Frequency.MONTHLY && rrule.byDay.isEmpty()) {
                handleMonthlyRepeat(original, startDateAsDV, task.hasDueTime(), rrule)
            } else {
                invokeRecurrence(rrule, original, startDateAsDV)
            }
        }

        private fun handleWeeklyRepeatAfterComplete(
                rrule: RRule, original: DateTime, hasDueTime: Boolean): Long {
            val byDay = rrule.byDay
            var newDate = original.millis
            newDate += DateUtilities.ONE_WEEK * (rrule.interval - 1)
            var date = DateTime(newDate)
            Collections.sort(byDay, weekdayCompare)
            val next = findNextWeekday(byDay, date)
            do {
                date = date.plusDays(1)
            } while (date.dayOfWeek != next.wday.javaDayNum)
            val time = date.millis
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
            }
        }

        private fun handleMonthlyRepeat(
                original: DateTime, startDateAsDV: DateValue, hasDueTime: Boolean, rrule: RRule): Long {
            return if (original.isLastDayOfMonth) {
                val interval = rrule.interval
                val newDateTime = original.plusMonths(interval)
                val time = newDateTime.withDayOfMonth(newDateTime.numberOfDaysInMonth).millis
                if (hasDueTime) {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
                } else {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
                }
            } else {
                invokeRecurrence(rrule, original, startDateAsDV)
            }
        }

        private fun findNextWeekday(byDay: List<WeekdayNum>, date: DateTime): WeekdayNum {
            val next = byDay[0]
            for (weekday in byDay) {
                if (weekday.wday.javaDayNum > date.dayOfWeek) {
                    return weekday
                }
            }
            return next
        }

        private fun invokeRecurrence(rrule: RRule, original: DateTime, startDateAsDV: DateValue): Long {
            var newDueDate: Long = -1
            val iterator = RecurrenceIteratorFactory.createRecurrenceIterator(
                    rrule, startDateAsDV, TimeZone.getDefault())
            var nextDate: DateValue
            for (i in 0..9) { // ten tries then we give up
                if (!iterator.hasNext()) {
                    return -1
                }
                nextDate = iterator.next()
                if (nextDate.compareTo(startDateAsDV) == 0) {
                    continue
                }
                newDueDate = buildNewDueDate(original, nextDate)

                // detect if we finished
                if (newDueDate > original.millis) {
                    break
                }
            }
            return newDueDate
        }

        /** Compute long due date from DateValue  */
        private fun buildNewDueDate(original: DateTime, nextDate: DateValue): Long {
            val newDueDate: Long
            if (nextDate is DateTimeValueImpl) {
                var date = newDateUtc(
                        nextDate.year(),
                        nextDate.month(),
                        nextDate.day(),
                        nextDate.hour(),
                        nextDate.minute(),
                        nextDate.second())
                        .toLocal()
                // time may be inaccurate due to DST, force time to be same
                date = date.withHourOfDay(original.hourOfDay).withMinuteOfHour(original.minuteOfHour)
                newDueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.millis)
            } else {
                newDueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        newDate(nextDate.year(), nextDate.month(), nextDate.day()).millis)
            }
            return newDueDate
        }

        /** Initialize RRule instance  */
        @Throws(ParseException::class)
        private fun initRRule(recurrence: String?): RRule {
            val rrule = RRule(recurrence)

            if (rrule.count < 0) {
                rrule.count = 0
            }
            // handle the iCalendar "byDay" field differently depending on if
            // we are weekly or otherwise
            if (rrule.freq != Frequency.WEEKLY && rrule.freq != Frequency.MONTHLY) {
                rrule.byDay = emptyList()
            }
            return rrule
        }

        /** Set up repeat start date  */
        private fun setUpStartDate(
                task: Task, repeatAfterCompletion: Boolean, frequency: Frequency): DateTime {
            return if (repeatAfterCompletion) {
                var startDate = if (task.isCompleted) newDateTime(task.completionDate) else newDateTime()
                if (task.hasDueTime() && frequency != Frequency.HOURLY && frequency != Frequency.MINUTELY) {
                    val dueDate = newDateTime(task.dueDate)
                    startDate = startDate
                            .withHourOfDay(dueDate.hourOfDay)
                            .withMinuteOfHour(dueDate.minuteOfHour)
                            .withSecondOfMinute(dueDate.secondOfMinute)
                }
                startDate
            } else {
                if (task.hasDueDate()) newDateTime(task.dueDate) else newDateTime()
            }
        }

        private fun setUpStartDateAsDV(task: Task, startDate: DateTime): DateValue {
            return if (task.hasDueTime()) {
                DateTimeValueImpl(
                        startDate.year,
                        startDate.monthOfYear,
                        startDate.dayOfMonth,
                        startDate.hourOfDay,
                        startDate.minuteOfHour,
                        startDate.secondOfMinute)
            } else {
                DateValueImpl(
                        startDate.year, startDate.monthOfYear, startDate.dayOfMonth)
            }
        }

        private fun handleSubdayRepeat(startDate: DateTime, rrule: RRule): Long {
            val millis: Long = when (rrule.freq) {
                Frequency.HOURLY -> DateUtilities.ONE_HOUR
                Frequency.MINUTELY -> DateUtilities.ONE_MINUTE
                else -> throw RuntimeException(
                        "Error handing subday repeat: " + rrule.freq) // $NON-NLS-1$
            }
            val newDueDate = startDate.millis + millis * rrule.interval
            return createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate)
        }
    }
}