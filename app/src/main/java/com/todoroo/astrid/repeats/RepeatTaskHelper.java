/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.date.DateTimeUtils.newDateUtc;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.time.DateTime;
import timber.log.Timber;

public class RepeatTaskHelper {

  private static final Comparator<WeekdayNum> weekdayCompare =
      (object1, object2) -> object1.wday.javaDayNum - object2.wday.javaDayNum;
  private final GCalHelper gcalHelper;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final AlarmService alarmService;

  @Inject
  public RepeatTaskHelper(
      GCalHelper gcalHelper,
      AlarmService alarmService,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager) {
    this.gcalHelper = gcalHelper;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.alarmService = alarmService;
  }

  private static boolean repeatFinished(long newDueDate, long repeatUntil) {
    return repeatUntil > 0
        && newDateTime(newDueDate).startOfDay().isAfter(newDateTime(repeatUntil).startOfDay());
  }

  /** Compute next due date */
  static long computeNextDueDate(Task task, String recurrence, boolean repeatAfterCompletion)
      throws ParseException {
    RRule rrule = initRRule(recurrence);

    // initialize startDateAsDV
    DateTime original = setUpStartDate(task, repeatAfterCompletion, rrule.getFreq());
    DateValue startDateAsDV = setUpStartDateAsDV(task, original);

    if (rrule.getFreq() == Frequency.HOURLY || rrule.getFreq() == Frequency.MINUTELY) {
      return handleSubdayRepeat(original, rrule);
    } else if (rrule.getFreq() == Frequency.WEEKLY
        && rrule.getByDay().size() > 0
        && repeatAfterCompletion) {
      return handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime());
    } else if (rrule.getFreq() == Frequency.MONTHLY && rrule.getByDay().isEmpty()) {
      return handleMonthlyRepeat(original, startDateAsDV, task.hasDueTime(), rrule);
    } else {
      return invokeRecurrence(rrule, original, startDateAsDV);
    }
  }

  private static long handleWeeklyRepeatAfterComplete(
      RRule rrule, DateTime original, boolean hasDueTime) {
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
    if (hasDueTime) {
      return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time);
    } else {
      return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, time);
    }
  }

  private static long handleMonthlyRepeat(
      DateTime original, DateValue startDateAsDV, boolean hasDueTime, RRule rrule) {
    if (original.isLastDayOfMonth()) {
      int interval = rrule.getInterval();

      DateTime newDateTime = original.plusMonths(interval);
      long time = newDateTime.withDayOfMonth(newDateTime.getNumberOfDaysInMonth()).getMillis();
      if (hasDueTime) {
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time);
      } else {
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, time);
      }
    } else {
      return invokeRecurrence(rrule, original, startDateAsDV);
    }
  }

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
    RecurrenceIterator iterator =
        RecurrenceIteratorFactory.createRecurrenceIterator(
            rrule, startDateAsDV, TimeZone.getDefault());
    DateValue nextDate;

    for (int i = 0; i < 10; i++) { // ten tries then we give up
      if (!iterator.hasNext()) {
        return -1;
      }
      nextDate = iterator.next();

      if (nextDate.compareTo(startDateAsDV) == 0) {
        continue;
      }

      newDueDate = buildNewDueDate(original, nextDate);

      // detect if we finished
      if (newDueDate > original.getMillis()) {
        break;
      }
    }
    return newDueDate;
  }

  /** Compute long due date from DateValue */
  private static long buildNewDueDate(DateTime original, DateValue nextDate) {
    long newDueDate;
    if (nextDate instanceof DateTimeValueImpl) {
      DateTimeValueImpl newDateTime = (DateTimeValueImpl) nextDate;
      DateTime date =
          newDateUtc(
                  newDateTime.year(),
                  newDateTime.month(),
                  newDateTime.day(),
                  newDateTime.hour(),
                  newDateTime.minute(),
                  newDateTime.second())
              .toLocal();
      // time may be inaccurate due to DST, force time to be same
      date =
          date.withHourOfDay(original.getHourOfDay()).withMinuteOfHour(original.getMinuteOfHour());
      newDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getMillis());
    } else {
      newDueDate =
          Task.createDueDate(
              Task.URGENCY_SPECIFIC_DAY,
              newDate(nextDate.year(), nextDate.month(), nextDate.day()).getMillis());
    }
    return newDueDate;
  }

  /** Initialize RRule instance */
  private static RRule initRRule(String recurrence) throws ParseException {
    RRule rrule = new RRule(recurrence);

    // handle the iCalendar "byDay" field differently depending on if
    // we are weekly or otherwise
    if (rrule.getFreq() != Frequency.WEEKLY && rrule.getFreq() != Frequency.MONTHLY) {
      rrule.setByDay(Collections.emptyList());
    }

    return rrule;
  }

  /** Set up repeat start date */
  private static DateTime setUpStartDate(
      Task task, boolean repeatAfterCompletion, Frequency frequency) {
    if (repeatAfterCompletion) {
      DateTime startDate =
          task.isCompleted() ? newDateTime(task.getCompletionDate()) : newDateTime();
      if (task.hasDueTime() && frequency != Frequency.HOURLY && frequency != Frequency.MINUTELY) {
        DateTime dueDate = newDateTime(task.getDueDate());
        startDate =
            startDate
                .withHourOfDay(dueDate.getHourOfDay())
                .withMinuteOfHour(dueDate.getMinuteOfHour())
                .withSecondOfMinute(dueDate.getSecondOfMinute());
      }
      return startDate;
    } else {
      return task.hasDueDate() ? newDateTime(task.getDueDate()) : newDateTime();
    }
  }

  private static DateValue setUpStartDateAsDV(Task task, DateTime startDate) {
    if (task.hasDueTime()) {
      return new DateTimeValueImpl(
          startDate.getYear(),
          startDate.getMonthOfYear(),
          startDate.getDayOfMonth(),
          startDate.getHourOfDay(),
          startDate.getMinuteOfHour(),
          startDate.getSecondOfMinute());
    } else {
      return new DateValueImpl(
          startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth());
    }
  }

  private static long handleSubdayRepeat(DateTime startDate, RRule rrule) {
    long millis;
    switch (rrule.getFreq()) {
      case HOURLY:
        millis = DateUtilities.ONE_HOUR;
        break;
      case MINUTELY:
        millis = DateUtilities.ONE_MINUTE;
        break;
      default:
        throw new RuntimeException(
            "Error handing subday repeat: " + rrule.getFreq()); // $NON-NLS-1$
    }
    long newDueDate = startDate.getMillis() + millis * rrule.getInterval();
    return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDueDate);
  }

  public void handleRepeat(Task task) {
    String recurrence = task.sanitizedRecurrence();
    boolean repeatAfterCompletion = task.repeatAfterCompletion();

    if (recurrence != null && recurrence.length() > 0) {
      long newDueDate;
      RRule rrule;
      try {
        rrule = initRRule(task.getRecurrenceWithoutFrom());
        newDueDate = computeNextDueDate(task, recurrence, repeatAfterCompletion);
        if (newDueDate == -1) {
          return;
        }
      } catch (ParseException e) {
        Timber.e(e);
        return;
      }

      long oldDueDate = task.getDueDate();
      long repeatUntil = task.getRepeatUntil();

      if (repeatFinished(newDueDate, repeatUntil)) {
        return;
      }

      int count = rrule.getCount();
      if (count == 1) {
        return;
      }
      if (count > 1) {
        rrule.setCount(count - 1);
        task.setRecurrence(rrule, repeatAfterCompletion);
      }

      task.setReminderSnooze(0L);
      task.setCompletionDate(0L);
      task.setDueDateAdjustingHideUntil(newDueDate);

      gcalHelper.rescheduleRepeatingTask(task);
      taskDao.save(task);

      alarmService.rescheduleAlarms(task.getId(), oldDueDate, newDueDate);

      localBroadcastManager.broadcastRepeat(task.getId(), oldDueDate, newDueDate);
    }
  }
}
