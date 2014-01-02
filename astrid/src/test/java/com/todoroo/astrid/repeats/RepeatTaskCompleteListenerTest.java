package com.todoroo.astrid.repeats;

import android.annotation.SuppressLint;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.astrid.data.Task;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.text.ParseException;

import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
import static com.todoroo.andlib.utility.DateUtilities.addCalendarMonthsToUnixtime;
import static com.todoroo.astrid.repeats.RepeatTaskCompleteListener.computeNextDueDate;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;

@SuppressLint("NewApi")
@RunWith(RobolectricTestRunner.class)
public class RepeatTaskCompleteListenerTest {

    private final Task task = new Task();
    private final long dueDate;
    private final long completionDate;

    {
        completionDate = currentTimeMillis();
        dueDate = completionDate - DAYS.toMillis(7);
        task.setValue(Task.DUE_DATE, dueDate);
        task.setValue(Task.COMPLETION_DATE, completionDate);
    }

    @Test
    public void minutelyRepeat() {
        checkFrequency(6, MINUTES.toMillis(1), MINUTELY);
    }

    @Test
    public void hourlyRepeat() {
        checkFrequency(6, HOURS.toMillis(1), HOURLY);
    }

    @Test
    public void dailyRepeat() {
        checkFrequency(6, DAYS.toMillis(1), DAILY);
    }

    @Test
    public void weeklyRepeat() {
        checkFrequency(6, DAYS.toMillis(7), WEEKLY);
    }

    @Test
    public void monthlyRepeat() {
        checkExpected(6, addCalendarMonthsToUnixtime(dueDate, 6), MONTHLY, false);
        checkExpected(6, addCalendarMonthsToUnixtime(completionDate, 6), MONTHLY, true);
    }

    @Test
    public void yearlyRepeat() {
        checkExpected(6, addCalendarMonthsToUnixtime(dueDate, 6 * 12), YEARLY, false);
        checkExpected(6, addCalendarMonthsToUnixtime(completionDate, 6 * 12), YEARLY, true);
    }

    private void checkFrequency(int count, long interval, Frequency frequency) {
        checkExpected(count, dueDate + count * interval, frequency, false);
        checkExpected(count, completionDate + count * interval, frequency, true);
    }

    private void checkExpected(int count, long expected, Frequency frequency, boolean repeatAfterCompletion) {
        RRule rrule = new RRule();
        rrule.setInterval(count);
        rrule.setFreq(frequency);
        long nextDueDate;
        try {
            nextDueDate = computeNextDueDate(task, rrule.toIcal(), repeatAfterCompletion);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        assertEquals(
                new DateTime(expected)
                        .withSecondOfMinute(1)
                        .withMillisOfSecond(0)
                        .getMillis(),
                nextDueDate);
    }
}
