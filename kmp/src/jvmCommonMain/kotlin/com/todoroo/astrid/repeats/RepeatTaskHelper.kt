/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import org.tasks.calendars.CalendarHelper
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.RepeatFrom
import org.tasks.data.setRecurrence
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.repeats.ByDay
import org.tasks.repeats.Frequency
import org.tasks.repeats.Recur
import org.tasks.repeats.Until
import org.tasks.repeats.nextOccurrence
import org.tasks.repeats.toDateTime
import org.tasks.time.DateTime
import org.tasks.time.ONE_HOUR
import org.tasks.time.ONE_MINUTE
import org.tasks.time.ONE_WEEK

private const val TAG = "RepeatTaskHelper"

class RepeatTaskHelper(
    private val calendarHelper: CalendarHelper,
    private val alarmService: AlarmService,
    private val taskSaver: TaskSaver,
) {
    suspend fun handleRepeat(task: Task): Boolean {
        val recurrence = task.recurrence
        if (recurrence.isNullOrBlank()) {
            return false
        }
        val repeatAfterCompletion = task.repeatFrom == RepeatFrom.COMPLETION_DATE
        val newDueDate: Long
        val rrule: Recur
        val count: Int?
        try {
            rrule = initRRule(recurrence)
            count = rrule.count
            if (count == 1) {
                return true
            }
            newDueDate = computeNextDueDate(task, recurrence, repeatAfterCompletion)
            if (newDueDate == -1L) {
                return true
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(e, tag = TAG) { "" }
            return false
        }
        val original = task.copy()
        if (count != null && count > 1) {
            task.setRecurrence(rrule.copy(count = count - 1))
        }
        task.reminderLast = 0L
        task.reminderDismissed = 0L
        task.completionDate = 0L
        val oldDueDate = task.dueDate
        task.setDueDateAdjustingHideUntil(newDueDate)
        calendarHelper.rescheduleRepeatingTask(task)
        taskSaver.save(task, original)
        val previousDueDate = oldDueDate.takeIf { it > 0 } ?: computePreviousDueDate(task)
        rescheduleAlarms(task.id, previousDueDate, newDueDate)
        return true
    }

    suspend fun undoRepeat(task: Task, oldDueDate: Long) {
        val original = task.copy()
        if (task.completionDate > 0) {
            task.completionDate = 0
            taskSaver.save(task, original)
            return
        }
        try {
            val recur = Recur.parse(task.recurrence!!)
            val count = recur.count
            task.setRecurrence(if (count != null && count > 0) recur.copy(count = count + 1) else recur)
            val newDueDate = task.dueDate
            task.setDueDateAdjustingHideUntil(
                if (oldDueDate > 0) {
                    oldDueDate
                } else {
                    newDueDate - (computeNextDueDate(task, task.recurrence!!, false) - newDueDate)
                }
            )
            rescheduleAlarms(task.id, newDueDate, task.dueDate)
        } catch (e: IllegalArgumentException) {
            Logger.e(e, tag = TAG) { "" }
        }
        taskSaver.save(task, original)
    }

    private suspend fun rescheduleAlarms(taskId: Long, oldDueDate: Long, newDueDate: Long) {
        if (oldDueDate <= 0 || newDueDate <= 0) {
            return
        }
        alarmService.getAlarms(taskId)
            .filter { it.type != TYPE_SNOOZE }
            .map {
                if (it.type == Alarm.TYPE_DATE_TIME) {
                    it.copy(time = it.time + newDueDate - oldDueDate)
                } else {
                    it
                }
            }
            .let { alarmService.synchronizeAlarms(taskId, it.toMutableSet()) }
    }

    companion object {
        fun computePreviousDueDate(task: Task): Long =
            task.dueDate - (computeNextDueDate(task, task.recurrence!!, task.repeatFrom == RepeatFrom.COMPLETION_DATE) - task.dueDate)

        /** Compute next due date  */
        @Throws(IllegalArgumentException::class)
        fun computeNextDueDate(task: Task, recurrence: String, repeatAfterCompletion: Boolean): Long {
            var rrule = initRRule(recurrence)
            val until = rrule.until
            if (until is Until.Date && task.hasDueTime()) {
                // Tasks lets you create tasks with due date-times, but recurrence until with due dates
                // This violates the spec and should be fixed in the picker
                rrule = rrule.copy(until = Until.DateTime(until.toDateTime().endOfDay().millis, utc = false))
            }

            val original = setUpStartDate(task, repeatAfterCompletion, rrule.frequency)
            return when {
                rrule.frequency == Frequency.SECONDLY ||
                rrule.frequency == Frequency.MINUTELY ||
                rrule.frequency == Frequency.HOURLY ->
                    handleSubdayRepeat(original, rrule)
                rrule.frequency == Frequency.WEEKLY && rrule.byDay.isNotEmpty() && repeatAfterCompletion ->
                    handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime())
                rrule.frequency == Frequency.MONTHLY && rrule.byDay.isEmpty() ->
                    handleMonthlyRepeat(original, task.hasDueTime(), rrule)
                else ->
                    invokeRecurrence(rrule, original, task.hasDueTime())
            }
        }

        @Deprecated("probably don't need this?")
        private fun handleWeeklyRepeatAfterComplete(
                recur: Recur, original: DateTime, hasDueTime: Boolean): Long {
            val byDay = recur.byDay.sortedBy { it.day.calendarDay }
            var newDate = original.millis
            newDate += ONE_WEEK * ((recur.interval ?: 1).coerceAtLeast(1) - 1)
            var date = DateTime(newDate)
            val next = findNextWeekday(byDay, date)
            do {
                date = date.plusDays(1)
            } while (date.dayOfWeek != next.day.calendarDay)
            val time = date.millis
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
            }
        }

        private fun handleMonthlyRepeat(
                original: DateTime, hasDueTime: Boolean, recur: Recur): Long {
            if (recur.byMonth.isNotEmpty()) {
                return invokeRecurrence(recur, original, hasDueTime)
            }
            val monthDays = recur.byMonthDay
            val anchor = when {
                monthDays.isEmpty() -> original.dayOfMonth
                monthDays.size == 1 && monthDays[0] > 0 -> monthDays[0]
                else -> return invokeRecurrence(recur, original, hasDueTime)
            }
            val interval = (recur.interval ?: 1).coerceAtLeast(1)
            val newDateTime = original.plusMonths(interval)
            if (anchor <= newDateTime.numberOfDaysInMonth) {
                return invokeRecurrence(recur, original, hasDueTime)
            }
            val time = newDateTime.withDayOfMonth(newDateTime.numberOfDaysInMonth).millis
            val until = recur.until?.let { it.toDateTime().endOfDay().millis }
            if (until != null && time > until) {
                return -1
            }
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, time)
            }
        }

        private fun findNextWeekday(byDay: List<ByDay>, date: DateTime): ByDay {
            val next = byDay[0]
            for (weekday in byDay) {
                if (weekday.day.calendarDay > date.dayOfWeek) {
                    return weekday
                }
            }
            return next
        }

        private fun invokeRecurrence(recur: Recur, original: DateTime, hasDueTime: Boolean): Long {
            val next = recur.nextOccurrence(original, hasDueTime) ?: return -1
            return if (hasDueTime) {
                createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, next.millis)
            } else {
                createDueDate(Task.URGENCY_SPECIFIC_DAY, next.millis)
            }
        }

        /** Initialize RRule instance  */
        @Throws(IllegalArgumentException::class)
        private fun initRRule(recurrence: String): Recur {
            val rrule = Recur.parse(recurrence)

            // handle the iCalendar "byDay" field differently depending on if
            // we are weekly or otherwise
            return if (rrule.frequency != Frequency.WEEKLY && rrule.frequency != Frequency.MONTHLY) {
                rrule.copy(byDay = emptyList())
            } else {
                rrule
            }
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

        @Deprecated("probably don't need this?")
        private fun handleSubdayRepeat(startDate: DateTime, recur: Recur): Long {
            val millis: Long = when (recur.frequency) {
                Frequency.HOURLY -> ONE_HOUR
                Frequency.MINUTELY -> ONE_MINUTE
                else -> throw RuntimeException(
                        "Error handing subday repeat: " + recur.frequency)
            }
            val newDueDate = startDate.millis + millis * (recur.interval ?: 1).coerceAtLeast(1)
            return createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate)
        }
    }
}
