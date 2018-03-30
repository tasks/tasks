package com.todoroo.astrid.repeats;

import static com.todoroo.astrid.repeats.RepeatTaskHelper.computeNextDueDate;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.astrid.data.Task;
import java.text.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class NewRepeatTests {

  @Test
  public void testRepeatMinutelyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 26, 12, 30);
    Task task = newFromDue(Frequency.MINUTELY, 1, dueDateTime);

    assertEquals(newDayTime(2016, 8, 26, 12, 31), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatHourlyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 26, 12, 30);
    Task task = newFromDue(Frequency.HOURLY, 1, dueDateTime);

    assertEquals(newDayTime(2016, 8, 26, 13, 30), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatDailyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 26, 12, 30);
    Task task = newFromDue(Frequency.DAILY, 1, dueDateTime);

    assertEquals(newDayTime(2016, 8, 27, 12, 30), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatWeeklyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 1, 34);
    Task task = newFromDue(Frequency.WEEKLY, 1, dueDateTime);

    assertEquals(newDayTime(2016, 9, 4, 1, 34), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatMonthlyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 1, 44);
    Task task = newFromDue(Frequency.MONTHLY, 1, dueDateTime);

    assertEquals(newDayTime(2016, 9, 28, 1, 44), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatYearlyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 1, 44);
    Task task = newFromDue(Frequency.YEARLY, 1, dueDateTime);

    assertEquals(newDayTime(2017, 8, 28, 1, 44), calculateNextDueDate(task));
  }

  /** Tests for repeating from completionDate */
  @Test
  public void testRepeatMinutelyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.MINUTELY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 29, 0, 15), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatMinutelyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.MINUTELY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 29, 0, 15), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatHourlyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.HOURLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 29, 1, 14), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatHourlyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.HOURLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 29, 1, 14), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatDailyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.DAILY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 30, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatDailyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.DAILY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 8, 30, 0, 4), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatWeeklyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.WEEKLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 9, 5, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatWeeklyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.WEEKLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 9, 5, 0, 4), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatMonthlyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.MONTHLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 9, 29, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatMonthlyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.MONTHLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2016, 9, 29, 0, 4), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatYearlyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 30, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.YEARLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2017, 8, 29, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testRepeatYearlyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 28, 0, 4);
    DateTime completionDateTime = newDayTime(2016, 8, 29, 0, 14);
    Task task = newFromCompleted(Frequency.YEARLY, 1, dueDateTime, completionDateTime);

    assertEquals(newDayTime(2017, 8, 29, 0, 4), calculateNextDueDate(task));
  }

  @Test
  public void testAdvancedRepeatWeeklyFromDueDate() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 29, 0, 25);
    Task task =
        newWeeklyFromDue(
            1, dueDateTime, new WeekdayNum(0, Weekday.MO), new WeekdayNum(0, Weekday.WE));

    assertEquals(newDayTime(2016, 8, 31, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testAdvancedRepeatWeeklyFromCompleteDateCompleteBefore() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 29, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 8, 28, 1, 9);
    Task task =
        newWeeklyFromCompleted(
            1,
            dueDateTime,
            completionDateTime,
            new WeekdayNum(0, Weekday.MO),
            new WeekdayNum(0, Weekday.WE));

    assertEquals(newDayTime(2016, 8, 29, 0, 25), calculateNextDueDate(task));
  }

  @Test
  public void testAdvancedRepeatWeeklyFromCompleteDateCompleteAfter() throws ParseException {
    DateTime dueDateTime = newDayTime(2016, 8, 29, 0, 25);
    DateTime completionDateTime = newDayTime(2016, 9, 1, 1, 9);
    Task task =
        newWeeklyFromCompleted(
            1,
            dueDateTime,
            completionDateTime,
            new WeekdayNum(0, Weekday.MO),
            new WeekdayNum(0, Weekday.WE));

    assertEquals(newDayTime(2016, 9, 5, 0, 25), calculateNextDueDate(task));
  }

  private DateTime newDayTime(int year, int month, int day, int hour, int minute) {
    return new DateTime(
        Task.createDueDate(
            Task.URGENCY_SPECIFIC_DAY_TIME,
            new DateTime(year, month, day, hour, minute).getMillis()));
  }

  private DateTime calculateNextDueDate(Task task) throws ParseException {
    return new DateTime(
        computeNextDueDate(task, task.sanitizedRecurrence(), task.repeatAfterCompletion()));
  }

  private Task newFromDue(Frequency frequency, int interval, DateTime dueDateTime) {
    return new Task() {
      {
        setRecurrence(getRecurrenceRule(frequency, interval, false));
        setDueDate(dueDateTime.getMillis());
      }
    };
  }

  private Task newWeeklyFromDue(int interval, DateTime dueDateTime, WeekdayNum... weekdays) {
    return new Task() {
      {
        setRecurrence(getRecurrenceRule(Frequency.WEEKLY, interval, false, weekdays));
        setDueDate(dueDateTime.getMillis());
      }
    };
  }

  private Task newFromCompleted(
      Frequency frequency, int interval, DateTime dueDateTime, DateTime completionDate) {
    return new Task() {
      {
        setRecurrence(getRecurrenceRule(frequency, interval, true));
        setDueDate(dueDateTime.getMillis());
        setCompletionDate(completionDate.getMillis());
      }
    };
  }

  private Task newWeeklyFromCompleted(
      int interval, DateTime dueDateTime, DateTime completionDate, WeekdayNum... weekdays) {
    return new Task() {
      {
        setRecurrence(getRecurrenceRule(Frequency.WEEKLY, interval, true, weekdays));
        setDueDate(dueDateTime.getMillis());
        setCompletionDate(completionDate.getMillis());
      }
    };
  }

  private String getRecurrenceRule(
      Frequency frequency, int interval, boolean fromCompletion, WeekdayNum... weekdays) {
    RRule rrule = new RRule();
    rrule.setFreq(frequency);
    rrule.setInterval(interval);
    if (weekdays != null) {
      rrule.setByDay(asList(weekdays));
    }
    String result = rrule.toIcal();
    if (fromCompletion) {
      result += ";FROM=COMPLETION";
    }
    return result;
  }
}
