package com.todoroo.astrid.repeats;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.astrid.data.Task;

import org.tasks.time.DateTime;

import java.text.ParseException;

import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
import static com.todoroo.andlib.utility.DateUtilities.addCalendarMonthsToUnixtime;
import static com.todoroo.astrid.repeats.RepeatTaskCompleteListener.computeNextDueDate;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

@SuppressLint("NewApi")
public class RepeatTaskCompleteListenerTest extends AndroidTestCase {

    private final Task task = new Task();
    private final long dueDate;
    private final long completionDate;

    {
        completionDate = new DateTime(2014, 1, 7, 17, 17, 32, 900).getMillis();
        dueDate = new DateTime(2013, 12, 31, 17, 17, 32, 900).getMillis();
        task.setDueDate(dueDate);
        task.setCompletionDate(completionDate);
    }

    public void testMinutelyRepeat() {
        checkFrequency(6, MINUTES.toMillis(1), MINUTELY);
    }

    public void testHourlyRepeat() {
        checkFrequency(6, HOURS.toMillis(1), HOURLY);
    }

    public void testDailyRepeat() {
        checkFrequency(6, DAYS.toMillis(1), DAILY);
    }

    public void testWeeklyRepeat() {
        checkFrequency(6, DAYS.toMillis(7), WEEKLY);
    }

    public void testMonthlyRepeat() {
        assertEquals(
                new DateTime(2014, 7, 7, 17, 17, 1, 0).getMillis(),
                nextDueDate(6, Frequency.MONTHLY, true));
    }

    public void testMonthlyRepeatAtEndOfMonth() {
        assertEquals(
                new DateTime(2014, 6, 30, 17, 17, 1, 0).getMillis(),
                nextDueDate(6, Frequency.MONTHLY, false));
    }

    public void testYearlyRepeat() {
        checkExpected(6, addCalendarMonthsToUnixtime(dueDate, 6 * 12), YEARLY, false);
        checkExpected(6, addCalendarMonthsToUnixtime(completionDate, 6 * 12), YEARLY, true);
    }

    private void checkFrequency(int count, long interval, Frequency frequency) {
        checkExpected(count, dueDate + count * interval, frequency, false);
        checkExpected(count, completionDate + count * interval, frequency, true);
    }

    private void checkExpected(int count, long expected, Frequency frequency, boolean repeatAfterCompletion) {
        assertEquals(
                new DateTime(expected)
                        .withSecondOfMinute(1)
                        .withMillisOfSecond(0)
                        .getMillis(),
                nextDueDate(count, frequency, repeatAfterCompletion));
    }

    private long nextDueDate(int count, Frequency frequency, boolean repeatAfterCompletion) {
        RRule rrule = new RRule();
        rrule.setInterval(count);
        rrule.setFreq(frequency);
        try {
            return computeNextDueDate(task, rrule.toIcal(), repeatAfterCompletion);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
