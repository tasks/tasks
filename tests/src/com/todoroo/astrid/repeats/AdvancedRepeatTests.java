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
        rrule = new RRule();
    }

    public static void assertDatesEqual(long date, long other) {
        assertEquals("Expected: " + new Date(date) + ", Actual: " + new Date(other),
                date, other);
    }

    public void testDueDateInPast() throws ParseException {
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);

        // repeat once => due date should become tomorrow
        long past = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, new Date(110, 7, 1).getTime());
        task.setValue(Task.DUE_DATE, past);
        long tomorrow = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, DateUtilities.now() + DateUtilities.ONE_DAY);
        long nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(tomorrow, nextDueDate);

        // test specific day & time
        long pastWithTime = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 10, 4).getTime());
        task.setValue(Task.DUE_DATE, pastWithTime);
        Date date = new Date(DateUtilities.now() / 1000L * 1000L);
        date.setHours(10);
        date.setMinutes(4);
        date.setSeconds(0);
        long todayWithTime = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getTime()) / 1000L * 1000L;
        if(todayWithTime < DateUtilities.now())
            todayWithTime += DateUtilities.ONE_DAY;
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertDatesEqual(todayWithTime, nextDueDate);
    }

    public void testDueDateInPastRepeatMultiple() throws ParseException {
        rrule.setInterval(1);
        rrule.setFreq(Frequency.DAILY);

        // repeat once => due date should become tomorrow
        long past = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, new Date(110, 7, 1, 0, 0, 0).getTime());
        task.setValue(Task.DUE_DATE, past);
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertTrue(nextDueDate > DateUtilities.now());
        task.setValue(Task.DUE_DATE, nextDueDate);
        long evenMoreNextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
        assertNotSame(nextDueDate, evenMoreNextDueDate);
    }

    // --- in-the-past tests

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

    // --- single day tests

    /** test single day repeats - DUE DATE */
    public void testSingleDay() throws Exception {
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

    // --- multi day tests

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
        assertDueDate(nextDueDate, THIS, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, THIS, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);
    }

    /** test multiple days per week - COMPLETE DATE */
    public void testCompleteDateSingleWeekMultiDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        buildRRule(1, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT, Calendar.MONDAY);
    }

    /** test multiple days per week, multiple intervals - COMPLETE DATE */
    public void testCompleteDateMultiWeekMultiDay() throws Exception {
        task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        buildRRule(2, Frequency.WEEKLY, Weekday.MO, Weekday.WE, Weekday.FR);

        setTaskDueDate(THIS, Calendar.SUNDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT_NEXT, Calendar.MONDAY);

        setTaskDueDate(THIS, Calendar.MONDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT_NEXT, Calendar.WEDNESDAY);

        setTaskDueDate(THIS, Calendar.FRIDAY);
        computeNextDueDate();
        assertDueDate(nextDueDate, NEXT_NEXT, Calendar.MONDAY);
    }

    // --- helpers

    private void computeNextDueDate() throws ParseException{
        nextDueDate = RepeatTaskCompleteListener.computeNextDueDate(task, rrule.toIcal());
    }

    private void buildRRule(int interval, Frequency freq, Weekday mo, Weekday... weekdays) {
        rrule.setInterval(interval);
        rrule.setFreq(freq);
        setRRuleDays(rrule, weekdays);
    }

    private void assertDueDate(long actual, int expectedWhich, int expectedDayOfWeek) {
        long expected = getDate(task.getValue(Task.DUE_DATE), expectedWhich, expectedDayOfWeek);
        assertEquals("Due Date is '" + new Date(actual) + "', expected '" + new Date(expected) + "'",
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

        task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                time));
    }

    private long getDate(long start, int which, int dayOfWeek) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        int direction = which > 0 ? 1 : -1;

        which = Math.abs(which);
        while(which-- > 1)
            c.add(Calendar.DAY_OF_MONTH, direction * 7);
        while(c.get(Calendar.DAY_OF_WEEK) != dayOfWeek)
            c.add(Calendar.DAY_OF_MONTH, direction);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

}
