package com.todoroo.astrid.data;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.Snippet;

import static com.todoroo.astrid.data.Task.COMPLETION_DATE;
import static com.todoroo.astrid.data.Task.DELETION_DATE;
import static com.todoroo.astrid.data.Task.DUE_DATE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL;
import static com.todoroo.astrid.data.Task.URGENCY_DAY_AFTER;
import static com.todoroo.astrid.data.Task.URGENCY_IN_TWO_WEEKS;
import static com.todoroo.astrid.data.Task.URGENCY_NEXT_MONTH;
import static com.todoroo.astrid.data.Task.URGENCY_NEXT_WEEK;
import static com.todoroo.astrid.data.Task.URGENCY_NONE;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY_TIME;
import static com.todoroo.astrid.data.Task.URGENCY_TODAY;
import static com.todoroo.astrid.data.Task.URGENCY_TOMORROW;
import static com.todoroo.astrid.data.Task.createDueDate;
import static com.todoroo.astrid.data.Task.hasDueTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;

@RunWith(RobolectricTestRunner.class)
public class TaskTest {

    private static final DateTime now = new DateTime(2013, 12, 31, 16, 10, 53, 452);
    private static final DateTime specificDueDate = new DateTime(2014, 3, 17, 9, 54, 27, 959);

    @Before
    public void before() {
        freezeAt(now);
    }

    @After
    public void after() {
        thaw();
    }

    @Test
    public void createDueDateNoUrgency() {
        assertEquals(0, createDueDate(URGENCY_NONE, 1L));
    }

    @Test
    public void createDueDateToday() {
        long expected = new DateTime(2013, 12, 31, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_TODAY, -1L));
    }

    @Test
    public void createDueDateTomorrow() {
        long expected = new DateTime(2014, 1, 1, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_TOMORROW, -1L));
    }

    @Test
    public void createDueDateDayAfter() {
        long expected = new DateTime(2014, 1, 2, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_DAY_AFTER, -1L));
    }

    @Test
    public void createDueDateNextWeek() {
        long expected = new DateTime(2014, 1, 7, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_NEXT_WEEK, -1L));
    }

    @Test
    public void createDueDateInTwoWeeks() {
        long expected = new DateTime(2014, 1, 14, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_IN_TWO_WEEKS, -1L));
    }

    @Test
    public void createDueDateNextMonth() {
        long expected = new DateTime(2014, 1, 31, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_NEXT_MONTH, -1L));
    }

    @Test
    public void removeTimeForSpecificDay() {
        long expected = specificDueDate
                .withHourOfDay(12)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)
                .getMillis();
        assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY, specificDueDate.getMillis()));
    }

    @Test
    public void removeSecondsForSpecificTime() {
        long expected = specificDueDate
                .withSecondOfMinute(1)
                .withMillisOfSecond(0)
                .getMillis();
        assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY_TIME, specificDueDate.getMillis()));
    }

    @Test
    public void taskHasDueTime() {
        Task task = new Task();
        task.setValue(DUE_DATE, 1388516076000L);
        assertTrue(task.hasDueTime());
        assertTrue(task.hasDueDate());
    }

    @Test
    public void taskHasDueDate() {
        Task task = new Task();
        task.setValue(DUE_DATE, 1388469600000L);
        assertFalse(task.hasDueTime());
        assertTrue(task.hasDueDate());
    }

    @Test
    public void doesHaveDueTime() {
        assertTrue(hasDueTime(1388516076000L));
    }

    @Test
    public void doesNotHaveDueTime() {
        assertFalse(hasDueTime(1388469600000L));
    }

    @Test
    public void newTaskIsNotCompleted() {
        assertFalse(new Task().isCompleted());
    }

    @Test
    public void newTaskNotDeleted() {
        assertFalse(new Task().isDeleted());
    }

    @Test
    public void newTaskNotHidden() {
        assertFalse(new Task().isHidden());
    }

    @Test
    public void newTaskDoesNotHaveDueDateOrTime() {
        assertFalse(new Task().hasDueDate());
        assertFalse(new Task().hasDueTime());
    }

    @Test
    public void taskIsCompleted() {
        Task task = new Task();
        task.setValue(COMPLETION_DATE, 1L);
        assertTrue(task.isCompleted());
    }

    @Test
    public void taskIsNotHiddenAtHideUntilTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(HIDE_UNTIL, now);
            assertFalse(task.isHidden());
        }});
    }

    @Test
    public void taskIsHiddenBeforeHideUntilTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(HIDE_UNTIL, now + 1);
            assertTrue(task.isHidden());
        }});
    }

    @Test
    public void taskIsDeleted() {
        Task task = new Task();
        task.setValue(DELETION_DATE, 1L);
        assertTrue(task.isDeleted());
    }

    @Test
    public void taskWithNoDueDateIsOverdue() {
        assertTrue(new Task().isOverdue());
    }

    @Test
    public void taskNotOverdueAtDueTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, now);
            assertFalse(task.isOverdue());
        }});
    }

    @Test
    public void taskIsOverduePastDueTime() {
        final long dueDate = currentTimeMillis();
        freezeAt(dueDate + 1).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate);
            assertTrue(task.isOverdue());
        }});
    }

    @Test
    public void taskNotOverdueBeforeNoonOnDueDate() {
        final DateTime dueDate = new DateTime().withMillisOfDay(0);
        freezeAt(dueDate.plusHours(12).minusMillis(1)).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate.getMillis());
            assertFalse(task.hasDueTime());
            assertFalse(task.isOverdue());
        }});
    }

    @Test
    public void taskOverdueAtNoonOnDueDate() {
        final DateTime dueDate = new DateTime().withMillisOfDay(0);
        freezeAt(dueDate.plusHours(12)).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate.getMillis());
            assertFalse(task.hasDueTime());
            assertTrue(task.isOverdue());
        }});
    }
}
