package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.model.Task;

public class AdvancedRepeatTests extends TodorooTestCase {


    public static void assertDatesEqual(long date, long other) {
        assertEquals("Expected: " + new Date(date) + ", Actual: " + new Date(other),
                date, other);
    }

    public void testDailyWithDaysOfWeek() throws ParseException {
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);
        rrule.setByDay(Collections.singletonList(new WeekdayNum(0, Weekday.FR)));

        Task task = new Task();
        long thursday = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(113, 7, 1).getTime());
        task.setValue(Task.DUE_DATE, thursday);

        // repeat once => due date should become friday
        long friday = thursday + DateUtilities.ONE_DAY;
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(friday, nextDueDate);

        // repeat again => due date should be one week from friday
        long nextFriday = friday + DateUtilities.ONE_WEEK;
        task.setValue(Task.DUE_DATE, friday);
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(nextFriday, nextDueDate);

        // now try with thursday, and repeat every 2 days. expect next friday
        rrule.setInterval(2);
        task.setValue(Task.DUE_DATE, thursday);
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(nextFriday, nextDueDate);

        // again with friday, expect next friday
        task.setValue(Task.DUE_DATE, friday);
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(nextFriday, nextDueDate);
    }

    public void testMonthlyWithDaysOfWeek() throws ParseException {
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.MONTHLY);
        rrule.setByDay(Arrays.asList(new WeekdayNum[] {
                new WeekdayNum(0, Weekday.SU),
                new WeekdayNum(0, Weekday.MO),
                new WeekdayNum(0, Weekday.TU),
                new WeekdayNum(0, Weekday.WE),
                new WeekdayNum(0, Weekday.TH),
                new WeekdayNum(0, Weekday.FR),
                new WeekdayNum(0, Weekday.SA),
        }));

        Task task = new Task();
        long thursday = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(113, 7, 1).getTime());
        task.setValue(Task.DUE_DATE, thursday);

        // repeat once => due date should become next month on the first
        long nextMonth = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(113, 8, 1).getTime());
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(nextMonth, nextDueDate);

        // only allow thursdays
        rrule.setByDay(Arrays.asList(new WeekdayNum[] {
                new WeekdayNum(0, Weekday.TH),
        }));
        long nextMonthOnThursday = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(113, 8, 5).getTime());
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(nextMonthOnThursday, nextDueDate);
    }

    public void testDueDateInPast() throws ParseException {
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);

        Task task = new Task();

        // repeat once => due date should become tomorrow
        long past = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(110, 7, 1).getTime());
        task.setValue(Task.DUE_DATE, past);
        long today = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateUtilities.now());
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(today, nextDueDate);

        // test specific day & time
        long pastWithTime = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 10, 4).getTime());
        task.setValue(Task.DUE_DATE, pastWithTime);
        Date date = new Date(DateUtilities.now() / 1000L * 1000L);
        date.setHours(10);
        date.setMinutes(4);
        long todayWithTime = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getTime());
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(todayWithTime + DateUtilities.ONE_DAY, nextDueDate);
    }
}
