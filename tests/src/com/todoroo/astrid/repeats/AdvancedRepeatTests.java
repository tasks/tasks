package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class AdvancedRepeatTests extends TodorooTestCase {


    public static void assertDatesEqual(long date, long other) {
        assertEquals("Expected: " + new Date(date) + ", Actual: " + new Date(other),
                date, other);
    }

    public void testDueDateInPast() throws ParseException {
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);

        Task task = new Task();

        // repeat once => due date should become tomorrow
        long past = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(110, 7, 1).getTime());
        task.setValue(Task.DUE_DATE, past);
        long tomorrow = task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateUtilities.now() + DateUtilities.ONE_DAY);
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(tomorrow, nextDueDate);

        // test specific day & time
        long pastWithTime = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 10, 4).getTime());
        task.setValue(Task.DUE_DATE, pastWithTime);
        Date date = new Date(DateUtilities.now() / 1000L * 1000L);
        date.setHours(10);
        date.setMinutes(4);
        date.setSeconds(0);
        long todayWithTime = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getTime()) / 1000L * 1000L;
        if(todayWithTime < DateUtilities.now())
            todayWithTime += DateUtilities.ONE_DAY;
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(todayWithTime, nextDueDate);
    }

    public void testDueDateInPastRepeatMultiple() throws ParseException {
        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);
        Task task = new Task();

        // repeat once => due date should become tomorrow
        long past = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 0, 0, 0).getTime());
        task.setValue(Task.DUE_DATE, past);
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertTrue(nextDueDate > DateUtilities.now());
        task.setValue(Task.DUE_DATE, nextDueDate);
        long evenMoreNextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertNotSame(nextDueDate, evenMoreNextDueDate);
    }

    /** test alternate completion flag */
    public void testAfterCompleteMultipleWeeks() throws Exception {
        // create a weekly task due a couple days in the past, but with the 'after completion'
        // specified. should be due 7 days from now
        Task task = new Task();
        long originalDueDate = (DateUtilities.now() - 1 * DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY, originalDueDate));
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        RRule rrule = new RRule();
        rrule.setInterval(5);
        rrule.setFreq(Frequency.WEEKLY);
        for(Weekday day : Weekday.values()) {
            ArrayList<WeekdayNum> days = new ArrayList<WeekdayNum>();
            days.add(new WeekdayNum(0, day));
            rrule.setByDay(days);

            long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
            assertTrue("Due date is '" + new Date(nextDueDate) + "', expected more like '" +
                    new Date(DateUtilities.now() + 5 * DateUtilities.ONE_WEEK) + "'",
                    Math.abs(nextDueDate - DateUtilities.now() - 5 * DateUtilities.ONE_WEEK) < DateUtilities.ONE_WEEK);
        }
    }

    /** test multiple days per week */
    public void testMultipleDaysPerWeek() throws Exception {
        Task task = new Task();
        long originalDueDate = (DateUtilities.now() - 1 * DateUtilities.ONE_DAY) / 1000L * 1000L;
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateUtilities.now()));

        RRule rrule = new RRule();
        rrule.setInterval(1);
        rrule.setFreq(Frequency.WEEKLY);
        ArrayList<WeekdayNum> days = new ArrayList<WeekdayNum>();

        days.add(new WeekdayNum(0, Weekday.MO));
        days.add(new WeekdayNum(0, Weekday.TH));
        rrule.setByDay(days);

        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertTrue("Due date is '" + new Date(nextDueDate) + "', expected no more than '" +
                new Date(DateUtilities.now() + DateUtilities.ONE_WEEK) + "'",
                nextDueDate - DateUtilities.now() < DateUtilities.ONE_WEEK);

        task.setValue(Task.DUE_DATE, nextDueDate);

        long subsequentDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertTrue("Due date is '" + new Date(subsequentDueDate) + "', expected no more than '" +
                new Date(DateUtilities.now() + 5 * DateUtilities.ONE_DAY) + "'",
                nextDueDate - DateUtilities.now() < 5 * DateUtilities.ONE_DAY);
    }

}
