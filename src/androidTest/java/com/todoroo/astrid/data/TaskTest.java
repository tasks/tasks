package com.todoroo.astrid.data;

import android.test.AndroidTestCase;

import org.tasks.time.DateTime;
import org.tasks.Snippet;

import java.util.ArrayList;
import java.util.TreeSet;

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
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class TaskTest extends AndroidTestCase {

    private static final DateTime now = new DateTime(2013, 12, 31, 16, 10, 53, 452);
    private static final DateTime specificDueDate = new DateTime(2014, 3, 17, 9, 54, 27, 959);

    @Override
    public void setUp() {
        freezeAt(now);
    }

    @Override
    public void tearDown() {
        thaw();
    }

    public void testCreateDueDateNoUrgency() {
        assertEquals(0, createDueDate(URGENCY_NONE, 1L));
    }

    public void testCreateDueDateToday() {
        long expected = new DateTime(2013, 12, 31, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_TODAY, -1L));
    }

    public void testCreateDueDateTomorrow() {
        long expected = new DateTime(2014, 1, 1, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_TOMORROW, -1L));
    }

    public void testCreateDueDateDayAfter() {
        long expected = new DateTime(2014, 1, 2, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_DAY_AFTER, -1L));
    }

    public void testCreateDueDateNextWeek() {
        long expected = new DateTime(2014, 1, 7, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_NEXT_WEEK, -1L));
    }

    public void testCreateDueDateInTwoWeeks() {
        long expected = new DateTime(2014, 1, 14, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_IN_TWO_WEEKS, -1L));
    }

    public void testCreateDueDateNextMonth() {
        long expected = new DateTime(2014, 1, 31, 12, 0, 0, 0).getMillis();
        assertEquals(expected, createDueDate(URGENCY_NEXT_MONTH, -1L));
    }

    public void testRemoveTimeForSpecificDay() {
        long expected = specificDueDate
                .withHourOfDay(12)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0)
                .getMillis();
        assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY, specificDueDate.getMillis()));
    }

    public void testRemoveSecondsForSpecificTime() {
        long expected = specificDueDate
                .withSecondOfMinute(1)
                .withMillisOfSecond(0)
                .getMillis();
        assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY_TIME, specificDueDate.getMillis()));
    }

    public void testTaskHasDueTime() {
        Task task = new Task();
        task.setValue(DUE_DATE, 1388516076000L);
        assertTrue(task.hasDueTime());
        assertTrue(task.hasDueDate());
    }

    public void testTaskHasDueDate() {
        Task task = new Task();
        task.setValue(DUE_DATE, 1388469600000L);
        assertFalse(task.hasDueTime());
        assertTrue(task.hasDueDate());
    }

    public void testDoesHaveDueTime() {
        assertTrue(hasDueTime(1388516076000L));
    }

    public void testNoDueTime() {
        assertFalse(hasDueTime(newDateTime().startOfDay().getMillis()));
        assertFalse(hasDueTime(newDateTime().withMillisOfDay(60000).getMillis()));
    }

    public void testHasDueTime() {
        assertTrue(hasDueTime(newDateTime().withMillisOfDay(1).getMillis()));
        assertTrue(hasDueTime(newDateTime().withMillisOfDay(1000).getMillis()));
        assertTrue(hasDueTime(newDateTime().withMillisOfDay(59999).getMillis()));
    }

    public void testDoesNotHaveDueTime() {
        assertFalse(hasDueTime(1388469600000L));
    }

    public void testNewTaskIsNotCompleted() {
        assertFalse(new Task().isCompleted());
    }

    public void testNewTaskNotDeleted() {
        assertFalse(new Task().isDeleted());
    }

    public void testNewTaskNotHidden() {
        assertFalse(new Task().isHidden());
    }

    public void testNewTaskDoesNotHaveDueDateOrTime() {
        assertFalse(new Task().hasDueDate());
        assertFalse(new Task().hasDueTime());
    }

    public void testTaskIsCompleted() {
        Task task = new Task();
        task.setValue(COMPLETION_DATE, 1L);
        assertTrue(task.isCompleted());
    }

    public void testTaskIsNotHiddenAtHideUntilTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(HIDE_UNTIL, now);
            assertFalse(task.isHidden());
        }});
    }

    public void testTaskIsHiddenBeforeHideUntilTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(HIDE_UNTIL, now + 1);
            assertTrue(task.isHidden());
        }});
    }

    public void testTaskIsDeleted() {
        Task task = new Task();
        task.setValue(DELETION_DATE, 1L);
        assertTrue(task.isDeleted());
    }

    public void testTaskWithNoDueDateIsOverdue() {
        assertTrue(new Task().isOverdue());
    }

    public void testTaskNotOverdueAtDueTime() {
        final long now = currentTimeMillis();
        freezeAt(now).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, now);
            assertFalse(task.isOverdue());
        }});
    }

    public void testTaskIsOverduePastDueTime() {
        final long dueDate = currentTimeMillis();
        freezeAt(dueDate + 1).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate);
            assertTrue(task.isOverdue());
        }});
    }

    public void testTaskNotOverdueBeforeNoonOnDueDate() {
        final DateTime dueDate = new DateTime().startOfDay();
        freezeAt(dueDate.plusHours(12).minusMillis(1)).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate.getMillis());
            assertFalse(task.hasDueTime());
            assertFalse(task.isOverdue());
        }});
    }

    public void testTaskOverdueAtNoonOnDueDate() {
        final DateTime dueDate = new DateTime().startOfDay();
        freezeAt(dueDate.plusHours(12)).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate.getMillis());
            assertFalse(task.hasDueTime());
            assertFalse(task.isOverdue());
        }});
    }

    public void testTaskWithNoDueTimeIsOverdue() {
        final DateTime dueDate = new DateTime().startOfDay();
        freezeAt(dueDate.plusDays(1)).thawAfter(new Snippet() {{
            Task task = new Task();
            task.setValue(DUE_DATE, dueDate.getMillis());
            assertFalse(task.hasDueTime());
            assertTrue(task.isOverdue());
        }});
    }

    public void testSanity() {
        assertTrue(Task.IMPORTANCE_DO_OR_DIE < Task.IMPORTANCE_MUST_DO);
        assertTrue(Task.IMPORTANCE_MUST_DO < Task.IMPORTANCE_SHOULD_DO);
        assertTrue(Task.IMPORTANCE_SHOULD_DO < Task.IMPORTANCE_NONE);

        ArrayList<Integer> reminderFlags = new ArrayList<>();
        reminderFlags.add(Task.NOTIFY_AFTER_DEADLINE);
        reminderFlags.add(Task.NOTIFY_AT_DEADLINE);
        reminderFlags.add(Task.NOTIFY_MODE_NONSTOP);

        // assert no duplicates
        assertEquals(new TreeSet<>(reminderFlags).size(), reminderFlags.size());
    }
}
