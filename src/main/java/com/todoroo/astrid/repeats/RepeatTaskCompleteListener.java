/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;

import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.time.DateTime;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.date.DateTimeUtils.newDateUtc;

public class RepeatTaskCompleteListener extends InjectingBroadcastReceiver {

    @Inject TaskService taskService;
    @Inject GCalHelper gcalHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        Task task = taskService.fetchById(taskId, Task.PROPERTIES);
        if(task == null || !task.isCompleted()) {
            return;
        }

        String recurrence = task.sanitizedRecurrence();
        boolean repeatAfterCompletion = task.repeatAfterCompletion();

        if(recurrence != null && recurrence.length() > 0) {
            long newDueDate;
            try {
                newDueDate = computeNextDueDate(task, recurrence, repeatAfterCompletion);
                if(newDueDate == -1) {
                    return;
                }
            } catch (ParseException e) {
                Timber.e(e, e.getMessage());
                return;
            }

            long oldDueDate = task.getDueDate();
            long repeatUntil = task.getRepeatUntil();

            if (repeatFinished(newDueDate, repeatUntil)) {
                return;
            }

            rescheduleTask(context, gcalHelper, taskService, task, newDueDate);

            // send a broadcast
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, oldDueDate);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, newDueDate);
            context.sendOrderedBroadcast(broadcastIntent, null);
            Flags.set(Flags.REFRESH);
        }
    }

    static boolean repeatFinished(long newDueDate, long repeatUntil) {
        return repeatUntil > 0 && newDateTime(newDueDate).startOfDay().isAfter(newDateTime(repeatUntil).startOfDay());
    }

    public static void rescheduleTask(Context context, GCalHelper gcalHelper, TaskService taskService, Task task, long newDueDate) {
        long hideUntil = task.getHideUntil();
        if(hideUntil > 0 && task.getDueDate() > 0) {
            hideUntil += newDueDate - task.getDueDate();
        }

        task.setReminderSnooze(0L);
        task.setCompletionDate(0L);
        task.setDueDate(newDueDate);
        task.setHideUntil(hideUntil);
        task.putTransitory(TaskService.TRANS_REPEAT_COMPLETE, true);

        ContentResolver cr = context.getContentResolver();
        gcalHelper.rescheduleRepeatingTask(task, cr);
        taskService.save(task);
    }

    /** Compute next due date */
    public static long computeNextDueDate(Task task, String recurrence, boolean repeatAfterCompletion) throws ParseException {
        RRule rrule = initRRule(recurrence);

        // initialize startDateAsDV
        DateTime original = setUpStartDate(task, repeatAfterCompletion, rrule.getFreq());
        DateValue startDateAsDV = setUpStartDateAsDV(task, original);

        if(rrule.getFreq() == Frequency.HOURLY || rrule.getFreq() == Frequency.MINUTELY) {
            return handleSubdayRepeat(original, rrule);
        } else if(rrule.getFreq() == Frequency.WEEKLY && rrule.getByDay().size() > 0 && repeatAfterCompletion) {
            return handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime());
        } else if (rrule.getFreq() == Frequency.MONTHLY) {
            return handleMonthlyRepeat(original, startDateAsDV, task.hasDueTime(), rrule);
        } else {
            return invokeRecurrence(rrule, original, startDateAsDV);
        }
    }

    private static long handleWeeklyRepeatAfterComplete(RRule rrule, DateTime original,
            boolean hasDueTime) {
        List<WeekdayNum> byDay = rrule.getByDay();
        long newDate = original.getMillis();
        newDate += DateUtilities.ONE_WEEK * (rrule.getInterval() - 1);
        DateTime date = new DateTime(newDate);

        Collections.sort(byDay, weekdayCompare);
        WeekdayNum next = findNextWeekday(byDay, date);

        do {
            date = date.plusDays(1);
        } while (date.getDayOfWeek() != next.wday.javaDayNum);

        long time = date.getMillis();
        if(hasDueTime) {
            return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time);
        } else {
            return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, time);
        }
    }

    private static long handleMonthlyRepeat(DateTime original, DateValue startDateAsDV, boolean hasDueTime, RRule rrule) {
        if (original.isLastDayOfMonth()) {
            int interval = rrule.getInterval();

            DateTime newDateTime = original.plusMonths(interval);
            long time = newDateTime
                    .withDayOfMonth(newDateTime.getNumberOfDaysInMonth())
                    .getMillis();
            if (hasDueTime) {
                return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time);
            } else {
                return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, time);
            }
        } else {
            return invokeRecurrence(rrule, original, startDateAsDV);
        }
    }

    private static Comparator<WeekdayNum> weekdayCompare = new Comparator<WeekdayNum>() {
        @Override
        public int compare(WeekdayNum object1, WeekdayNum object2) {
            return object1.wday.javaDayNum - object2.wday.javaDayNum;
        }

    };

    private static WeekdayNum findNextWeekday(List<WeekdayNum> byDay, DateTime date) {
        WeekdayNum next = byDay.get(0);
        for (WeekdayNum weekday : byDay) {
            if (weekday.wday.javaDayNum > date.getDayOfWeek()) {
                return weekday;
            }
        }
        return next;
    }

    private static long invokeRecurrence(RRule rrule, DateTime original, DateValue startDateAsDV) {
        long newDueDate = -1;
        RecurrenceIterator iterator = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                startDateAsDV, TimeZone.getDefault());
        DateValue nextDate;

        for(int i = 0; i < 10; i++) { // ten tries then we give up
            if(!iterator.hasNext()) {
                return -1;
            }
            nextDate = iterator.next();

            if(nextDate.compareTo(startDateAsDV) == 0) {
                continue;
            }

            newDueDate = buildNewDueDate(original, nextDate);

            // detect if we finished
            if(newDueDate > original.getMillis()) {
                break;
            }
        }
        return newDueDate;
    }

    /** Compute long due date from DateValue */
    private static long buildNewDueDate(DateTime original, DateValue nextDate) {
        long newDueDate;
        if(nextDate instanceof DateTimeValueImpl) {
            DateTimeValueImpl newDateTime = (DateTimeValueImpl) nextDate;
            DateTime date = newDateUtc(newDateTime.year(), newDateTime.month(),
                    newDateTime.day(), newDateTime.hour(),
                    newDateTime.minute(), newDateTime.second())
                    .toLocal();
            // time may be inaccurate due to DST, force time to be same
            date = date
                    .withHourOfDay(original.getHourOfDay())
                    .withMinuteOfHour(original.getMinuteOfHour());
            newDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getMillis());
        } else {
            newDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                    newDate(nextDate.year(), nextDate.month(), nextDate.day()).getMillis());
        }
        return newDueDate;
    }

    /** Initialize RRule instance */
    private static RRule initRRule(String recurrence) throws ParseException {
        RRule rrule = new RRule(recurrence);

        // handle the iCalendar "byDay" field differently depending on if
        // we are weekly or otherwise
        if(rrule.getFreq() != Frequency.WEEKLY) {
            rrule.setByDay(Collections.EMPTY_LIST);
        }

        return rrule;
    }

    /** Set up repeat start date */
    private static DateTime setUpStartDate(Task task, boolean repeatAfterCompletion, Frequency frequency) {
        DateTime startDate = newDateTime();
        if(task.hasDueDate()) {
            DateTime dueDate = newDateTime(task.getDueDate());
            if(repeatAfterCompletion) {
                startDate = newDateTime(task.getCompletionDate());
            } else {
                startDate = dueDate;
            }

            if(task.hasDueTime() && frequency != Frequency.HOURLY && frequency != Frequency.MINUTELY) {
                startDate = startDate
                        .withHourOfDay(dueDate.getHourOfDay())
                        .withMinuteOfHour(dueDate.getMinuteOfHour())
                        .withSecondOfMinute(dueDate.getSecondOfMinute());
            }
        }
        return startDate;
    }

    private static DateValue setUpStartDateAsDV(Task task, DateTime startDate) {
        if(task.hasDueTime()) {
            return new DateTimeValueImpl(startDate.getYear(),
                    startDate.getMonthOfYear(), startDate.getDayOfMonth(),
                    startDate.getHourOfDay(), startDate.getMinuteOfHour(), startDate.getSecondOfMinute());
        } else {
            return new DateValueImpl(startDate.getYear(),
                    startDate.getMonthOfYear(), startDate.getDayOfMonth());
        }
    }

    static long handleSubdayRepeat(DateTime startDate, RRule rrule) {
        long millis;
        switch(rrule.getFreq()) {
        case HOURLY:
            millis = DateUtilities.ONE_HOUR;
            break;
        case MINUTELY:
            millis = DateUtilities.ONE_MINUTE;
            break;
        default:
            throw new RuntimeException("Error handing subday repeat: " + rrule.getFreq()); //$NON-NLS-1$
        }
        long newDueDate = startDate.getMillis() + millis * rrule.getInterval();
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate);
    }

}
