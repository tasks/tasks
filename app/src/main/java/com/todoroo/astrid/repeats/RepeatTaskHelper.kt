/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.LocalBroadcastManager
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import timber.log.Timber
import java.text.ParseException
import java.util.*
import javax.inject.Inject

class RepeatTaskHelper @Inject constructor(
        private val gcalHelper: GCalHelper,
        private val alarmService: AlarmService,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val taskCompleter: TaskCompleter,
) {
    suspend fun handleRepeat(task: Task) {
        val recurrence = task.recurrence
        if (recurrence.isNullOrBlank()) {
            return
        }
        val repeatAfterCompletion = task.repeatAfterCompletion()
        val newDueDate: Long
        val rrule: Recur
        val count: Int
        try {
            rrule = initRRule(recurrence)
            count = rrule.count
            if (count == 1) {
                broadcastCompletion(task)
                return
            }
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
            broadcastCompletion(task)
            return
        }
        if (count > 1) {
            rrule.count = count - 1
            task.setRecurrence(rrule)
        }
        task.reminderLast = 0L
        task.completionDate = 0L
        task.setDueDateAdjustingHideUntil(newDueDate)
        gcalHelper.rescheduleRepeatingTask(task)
        taskDao.save(task)
        val previousDueDate =
                oldDueDate
                        .takeIf { it > 0 }
                        ?: (newDueDate - (computeNextDueDate(task, recurrence, repeatAfterCompletion) - newDueDate))
        rescheduleAlarms(task.id, previousDueDate, newDueDate)
        taskCompleter.setComplete(task, false)
        broadcastCompletion(task, previousDueDate)
    }

    private fun broadcastCompletion(task: Task, oldDueDate: Long = 0L) {
        if (!task.isSuppressRefresh()) {
            localBroadcastManager.broadcastTaskCompleted(task.id, oldDueDate)
        }
    }

    suspend fun undoRepeat(task: Task, oldDueDate: Long) {
        task.completionDate = 0L
        try {
            val recur = newRecur(task.recurrence!!)
            val count = recur.count
            if (count > 0) {
                recur.count = count + 1
            }
            task.setRecurrence(recur)
            val newDueDate = task.dueDate
            task.setDueDateAdjustingHideUntil(
                if (oldDueDate > 0) {
                    oldDueDate
                } else {
                    newDueDate - (computeNextDueDate(task, task.recurrence!!, false) - newDueDate)
                }
            )
            rescheduleAlarms(task.id, newDueDate, task.dueDate)
        } catch (e: ParseException) {
            Timber.e(e)
        }
        taskDao.save(task)
    }

    private suspend fun rescheduleAlarms(taskId: Long, oldDueDate: Long, newDueDate: Long) {
        if (oldDueDate <= 0 || newDueDate <= 0) {
            return
        }
        alarmService.getAlarms(taskId)
            .filter { it.type != TYPE_SNOOZE }
            .onEach {
                if (it.type == Alarm.TYPE_DATE_TIME) {
                    it.time += newDueDate - oldDueDate
                }
            }
            .let { alarmService.synchronizeAlarms(taskId, it.toMutableSet()) }
    }

    companion object {
        private val weekdayCompare = Comparator { object1: WeekDay, object2: WeekDay -> WeekDay.getCalendarDay(object1) - WeekDay.getCalendarDay(object2) }
        private fun repeatFinished(newDueDate: Long, repeatUntil: Long): Boolean {
            return repeatUntil > 0 && newDateTime(newDueDate).startOfDay().millis > repeatUntil
        }

        /** Compute next due date  */
        @Throws(ParseException::class)
        fun computeNextDueDate(task: Task, recurrence: String, repeatAfterCompletion: Boolean): Long {
            val rrule = initRRule(recurrence)

            // initialize startDateAsDV
            val original = setUpStartDate(task, repeatAfterCompletion, rrule.frequency)
            val startDateAsDV = setUpStartDateAsDV(task, original)
            return if (rrule.frequency == Recur.Frequency.HOURLY || rrule.frequency == Recur.Frequency.MINUTELY) {
                handleSubdayRepeat(original, rrule)
            } else if (rrule.frequency == Recur.Frequency.WEEKLY && rrule.dayList.isNotEmpty() && repeatAfterCompletion) {
                handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime())
            } else if (rrule.frequency == Recur.Frequency.MONTHLY && rrule.dayList.isEmpty()) {
                handleMonthlyRepeat(original, startDateAsDV, task.hasDueTime(), rrule)
            } else {
                invokeRecurrence(rrule, original, startDateAsDV)
            }
        }

        private fun handleWeeklyRepeatAfterComplete(
                recur: Recur, original: DateTime, hasDueTime: Boolean): Long {
            val byDay = recur.dayList
            var newDate = original.millis
            newDate += DateUtilities.ONE_WEEK * (recur.interval.coerceAtLeast(1) - 1)
            var date = DateTime(newDate)
            Collections.sort(byDay, weekdayCompare)
            val next = findNextWeekday(byDay, date)
            do {
                date = date.plusDays(1)
            } while (date.dayOfWeek != WeekDay.getCalendarDay(next))
            val time = date.millis
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
            }
        }

        private fun handleMonthlyRepeat(
                original: DateTime, startDateAsDV: Date, hasDueTime: Boolean, recur: Recur): Long {
            return if (original.isLastDayOfMonth) {
                val interval = recur.interval.coerceAtLeast(1)
                val newDateTime = original.plusMonths(interval)
                val time = newDateTime.withDayOfMonth(newDateTime.numberOfDaysInMonth).millis
                if (hasDueTime) {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
                } else {
                    createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
                }
            } else {
                invokeRecurrence(recur, original, startDateAsDV)
            }
        }

        private fun findNextWeekday(byDay: List<WeekDay>, date: DateTime): WeekDay {
            val next = byDay[0]
            for (weekday in byDay) {
                if (WeekDay.getCalendarDay(weekday) > date.dayOfWeek) {
                    return weekday
                }
            }
            return next
        }

        private fun invokeRecurrence(recur: Recur, original: DateTime, startDateAsDV: Date): Long {
            return recur.getNextDate(startDateAsDV, startDateAsDV)
                ?.let { buildNewDueDate(original, it) }
                ?: throw IllegalStateException("recur=$recur original=$original startDateAsDv=$startDateAsDV")
        }

        /** Compute long due date from DateValue  */
        private fun buildNewDueDate(original: DateTime, nextDate: Date): Long {
            val newDueDate: Long
            if (nextDate is net.fortuna.ical4j.model.DateTime) {
                var date = DateTime.from(nextDate)
                // time may be inaccurate due to DST, force time to be same
                date = date.withHourOfDay(original.hourOfDay).withMinuteOfHour(original.minuteOfHour)
                newDueDate = createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.millis)
            } else {
                newDueDate = createDueDate(
                        Task.URGENCY_SPECIFIC_DAY,
                        DateTime.from(nextDate).millis)
            }
            return newDueDate
        }

        /** Initialize RRule instance  */
        @Throws(ParseException::class)
        private fun initRRule(recurrence: String): Recur {
            val rrule = newRecur(recurrence)

            // handle the iCalendar "byDay" field differently depending on if
            // we are weekly or otherwise
            if (rrule.frequency != Recur.Frequency.WEEKLY && rrule.frequency != Recur.Frequency.MONTHLY) {
                rrule.dayList.clear()
            }
            return rrule
        }

        /** Set up repeat start date  */
        private fun setUpStartDate(
                task: Task, repeatAfterCompletion: Boolean, frequency: Recur.Frequency): DateTime {
            return if (repeatAfterCompletion) {
                var startDate = if (task.isCompleted) newDateTime(task.completionDate) else newDateTime()
                if (task.hasDueTime() && frequency != Recur.Frequency.HOURLY && frequency != Recur.Frequency.MINUTELY) {
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

        private fun setUpStartDateAsDV(task: Task, startDate: DateTime): Date {
            return if (task.hasDueTime()) {
                startDate.toDateTime()
            } else {
                startDate.toDate()
            }
        }

        private fun handleSubdayRepeat(startDate: DateTime, recur: Recur): Long {
            val millis: Long = when (recur.frequency) {
                Recur.Frequency.HOURLY -> DateUtilities.ONE_HOUR
                Recur.Frequency.MINUTELY -> DateUtilities.ONE_MINUTE
                else -> throw RuntimeException(
                        "Error handing subday repeat: " + recur.frequency) // $NON-NLS-1$
            }
            val newDueDate = startDate.millis + millis * recur.interval.coerceAtLeast(1)
            return createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate)
        }

        private val Task.repeatUntil: Long
            get() = recurrence
                ?.takeIf { it.isNotBlank() }
                ?.let { newRecur(it) }
                ?.until
                ?.let { DateTime.from(it) }
                ?.millis
                ?: 0L
    }
}