/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class AdvancedRepeatTests extends TodorooTestCase {


    private static final int PREV_PREV = -2;
    private static final int PREV = -1;
    private static final int THIS = 1;
    private static final int NEXT = 2;
    private static final int NEXT_NEXT = 3;

    private Task task;
    private long nextDueDate;
    private RRule rrule;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        task = new Task();
        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        rrule = new RRule();
    }

    // --- date with time tests

    public void testDueDateSpecificTime() throws ParseException {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);
        buildRRule(1, Frequency.DAILY);

        // test specific day & time
        long dayWithTime = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 10, 4).getTime());
        task.setValue(Task.DUE_DATE, dayWithTime);

        long nextDayWithTime = dayWithTime + DateUtilities.ONE_DAY;
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDateTimeEquals(nextDayWithTime, nextDueDate);
    }

    public void testCompletionDateSpecificTime() throws ParseException {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);
        buildRRule(1, Frequency.DAILY);

        // test specific day & time
        long dayWithTime = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 10, 4, 0).getTime());
        task.setValue(Task.DUE_DATE, dayWithTime);

        Date todayWithTime = new Date();
        todayWithTime.setHours(10);
        todayWithTime.setMinutes(4);
        todayWithTime.setSeconds(1);
        long nextDayWithTimeLong = todayWithTime.getTime();
        nextDayWithTimeLong += DateUtilities.ONE_DAY;
        nextDayWithTimeLong = nextDayWithTimeLong / 1000L * 1000;

        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDateTimeEquals(nextDayWithTimeLong, nextDueDate);
    }

    // --- due date tests

    /** test multiple days per week - DUE DATE */
    public void testDueDateInPastSingleWeekMultiDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

        buildRRule(1, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);
    }

    /** test single day repeats - DUE DATE */
    public void testDueDateSingleDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

        buildRRule(1, Frequency.WEEKLY, Weekday.MO);

        setTaskDueDate(PREV_PREV, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);

        setTaskDueDate(PREV_PREV, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(PREV, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);

        setTaskDueDate(PREV, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);
    }

    /** test multiple days per week - DUE DATE */
    public void testDueDateSingleWeekMultiDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

        buildRRule(1, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);
    }

    /** test multiple days per week, multiple intervals - DUE DATE */
    public void testDueDateMultiWeekMultiDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

        buildRRule(2, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);
    }

    // --- completion tests

    /** test multiple days per week - COMPLETE DATE */
    public void testCompleteDateSingleWeek() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        for(Weekday wday : Weekday.values()) {
            buildRRule(1, Frequency.WEEKLY, wday);
            computeNextDueDate();
            long expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday.javaDayNum);
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate);
            assertDateEquals(nextDueDate, expected);
        }

        for(Weekday wday1 : Weekday.values()) {
            for(Weekday wday2 : Weekday.values()) {
                if(wday1 == wday2)
                    continue;

                buildRRule(1, Frequency.WEEKLY, wday1, wday2);
                long nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday1.javaDayNum);
                long nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, THIS, wday2.javaDayNum);
                computeNextDueDate();
                nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate);
                assertDateEquals(nextDueDate, Math.min(nextOne, nextTwo));
            }
        }
    }

    /** test multiple days per week, multiple intervals - COMPLETE DATE */
    public void testCompleteDateMultiWeek() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        for(Weekday wday : Weekday.values()) {
            buildRRule(2, Frequency.WEEKLY, wday);
            computeNextDueDate();
            long expected = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday.javaDayNum);
            nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate);
            assertDateEquals(nextDueDate, expected);
        }

        for(Weekday wday1 : Weekday.values()) {
            for(Weekday wday2 : Weekday.values()) {
                if(wday1 == wday2)
                    continue;

                buildRRule(2, Frequency.WEEKLY, wday1, wday2);
                long nextOne = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday1.javaDayNum);
                long nextTwo = getDate(DateUtilities.now() + DateUtilities.ONE_DAY, NEXT, wday2.javaDayNum);
                computeNextDueDate();
                nextDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, nextDueDate);
                assertDateEquals(nextDueDate, Math.min(nextOne, nextTwo));
            }
        }
    }

    // --- helpers

    private void computeNextDueDate() throws ParseException{
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
    }

    private void buildRRule(int interval, Frequency freq, Weekday... weekdays) {
        rrule.setInterval(interval);
        rrule.setFreq(freq);
        setRRuleDays(rrule, weekdays);
    }

    private void assertDueDate(long actual, int expectedWhich, int expectedDayOfWeek) {
        long expected = getDate(task.getValue(Task.DUE_DATE), expectedWhich, expectedDayOfWeek);
        assertDateEquals(actual, expected);
    }

    public static void assertDateTimeEquals(long date, long other) {
        assertEquals("Expected: " + new Date(date) + ", Actual: " + new Date(other),
                date, other);
    }

    private void assertDateEquals(long actual, long expected) {
        assertEquals("Due Date is '" + DateUtilities.getDateStringWithWeekday(getContext(), new Date(actual))
                + "', expected '" + DateUtilities.getDateStringWithWeekday(getContext(), new Date(expected)) + "'",
                expected, actual);
    }

    private void setRRuleDays(RRule rrule, Weekday... weekdays) {
        ArrayList<WeekdayNum> days = new ArrayList<WeekdayNum>();
        for(Weekday wd : weekdays)
            days.add(new WeekdayNum(0, wd));
        rrule.setByDay(days);
    }


    private void setTaskDueDate(int which, int day) {
        long time = getDate(DateUtilities.now(), which, day);

        task.setValue(Task.DUE_DATE, time);
    }

    private long getDate(long start, int which, int dayOfWeek) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        int direction = which > 0 ? 1 : -1;

        while(c.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            c.add(Calendar.DAY_OF_MONTH, direction);
        }
        c.add(Calendar.DAY_OF_MONTH, (Math.abs(which) - 1) * direction * 7);
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, c.getTimeInMillis());
    }

}
