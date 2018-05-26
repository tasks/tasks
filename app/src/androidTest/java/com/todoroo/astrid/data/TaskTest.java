package com.todoroo.astrid.data;

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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.data.Task.Priority;
import java.util.ArrayList;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Snippet;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class TaskTest {

  private static final DateTime now = new DateTime(2013, 12, 31, 16, 10, 53, 452);
  private static final DateTime specificDueDate = new DateTime(2014, 3, 17, 9, 54, 27, 959);

  @Before
  public void setUp() {
    freezeAt(now);
  }

  @After
  public void tearDown() {
    thaw();
  }

  @Test
  public void testCreateDueDateNoUrgency() {
    assertEquals(0, createDueDate(URGENCY_NONE, 1L));
  }

  @Test
  public void testCreateDueDateToday() {
    long expected = new DateTime(2013, 12, 31, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_TODAY, -1L));
  }

  @Test
  public void testCreateDueDateTomorrow() {
    long expected = new DateTime(2014, 1, 1, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_TOMORROW, -1L));
  }

  @Test
  public void testCreateDueDateDayAfter() {
    long expected = new DateTime(2014, 1, 2, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_DAY_AFTER, -1L));
  }

  @Test
  public void testCreateDueDateNextWeek() {
    long expected = new DateTime(2014, 1, 7, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_NEXT_WEEK, -1L));
  }

  @Test
  public void testCreateDueDateInTwoWeeks() {
    long expected = new DateTime(2014, 1, 14, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_IN_TWO_WEEKS, -1L));
  }

  @Test
  public void testCreateDueDateNextMonth() {
    long expected = new DateTime(2014, 1, 31, 12, 0, 0, 0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_NEXT_MONTH, -1L));
  }

  @Test
  public void testRemoveTimeForSpecificDay() {
    long expected =
        specificDueDate
            .withHourOfDay(12)
            .withMinuteOfHour(0)
            .withSecondOfMinute(0)
            .withMillisOfSecond(0)
            .getMillis();
    assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY, specificDueDate.getMillis()));
  }

  @Test
  public void testRemoveSecondsForSpecificTime() {
    long expected = specificDueDate.withSecondOfMinute(1).withMillisOfSecond(0).getMillis();
    assertEquals(expected, createDueDate(URGENCY_SPECIFIC_DAY_TIME, specificDueDate.getMillis()));
  }

  @Test
  public void testTaskHasDueTime() {
    Task task = new Task();
    task.setDueDate(1388516076000L);
    assertTrue(task.hasDueTime());
    assertTrue(task.hasDueDate());
  }

  @Test
  public void testTaskHasDueDate() {
    Task task = new Task();
    task.setDueDate(1388469600000L);
    assertFalse(task.hasDueTime());
    assertTrue(task.hasDueDate());
  }

  @Test
  public void testDoesHaveDueTime() {
    assertTrue(hasDueTime(1388516076000L));
  }

  @Test
  public void testNoDueTime() {
    assertFalse(hasDueTime(newDateTime().startOfDay().getMillis()));
    assertFalse(hasDueTime(newDateTime().withMillisOfDay(60000).getMillis()));
  }

  @Test
  public void testHasDueTime() {
    assertTrue(hasDueTime(newDateTime().withMillisOfDay(1).getMillis()));
    assertTrue(hasDueTime(newDateTime().withMillisOfDay(1000).getMillis()));
    assertTrue(hasDueTime(newDateTime().withMillisOfDay(59999).getMillis()));
  }

  @Test
  public void testDoesNotHaveDueTime() {
    assertFalse(hasDueTime(1388469600000L));
  }

  @Test
  public void testNewTaskIsNotCompleted() {
    assertFalse(new Task().isCompleted());
  }

  @Test
  public void testNewTaskNotDeleted() {
    assertFalse(new Task().isDeleted());
  }

  @Test
  public void testNewTaskNotHidden() {
    assertFalse(new Task().isHidden());
  }

  @Test
  public void testNewTaskDoesNotHaveDueDateOrTime() {
    assertFalse(new Task().hasDueDate());
    assertFalse(new Task().hasDueTime());
  }

  @Test
  public void testTaskIsCompleted() {
    Task task = new Task();
    task.setCompletionDate(1L);
    assertTrue(task.isCompleted());
  }

  @Test
  public void testTaskIsNotHiddenAtHideUntilTime() {
    final long now = currentTimeMillis();
    freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setHideUntil(now);
                assertFalse(task.isHidden());
              }
            });
  }

  @Test
  public void testTaskIsHiddenBeforeHideUntilTime() {
    final long now = currentTimeMillis();
    freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setHideUntil(now + 1);
                assertTrue(task.isHidden());
              }
            });
  }

  @Test
  public void testTaskIsDeleted() {
    Task task = new Task();
    task.setDeletionDate(1L);
    assertTrue(task.isDeleted());
  }

  @Test
  public void testTaskWithNoDueDateIsOverdue() {
    assertTrue(new Task().isOverdue());
  }

  @Test
  public void testTaskNotOverdueAtDueTime() {
    final long now = currentTimeMillis();
    freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setDueDate(now);
                assertFalse(task.isOverdue());
              }
            });
  }

  @Test
  public void testTaskIsOverduePastDueTime() {
    final long dueDate = currentTimeMillis();
    freezeAt(dueDate + 1)
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setDueDate(dueDate);
                assertTrue(task.isOverdue());
              }
            });
  }

  @Test
  public void testTaskNotOverdueBeforeNoonOnDueDate() {
    final DateTime dueDate = new DateTime().startOfDay();
    freezeAt(dueDate.plusHours(12).minusMillis(1))
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setDueDate(dueDate.getMillis());
                assertFalse(task.hasDueTime());
                assertFalse(task.isOverdue());
              }
            });
  }

  @Test
  public void testTaskOverdueAtNoonOnDueDate() {
    final DateTime dueDate = new DateTime().startOfDay();
    freezeAt(dueDate.plusHours(12))
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setDueDate(dueDate.getMillis());
                assertFalse(task.hasDueTime());
                assertFalse(task.isOverdue());
              }
            });
  }

  @Test
  public void testTaskWithNoDueTimeIsOverdue() {
    final DateTime dueDate = new DateTime().startOfDay();
    freezeAt(dueDate.plusDays(1))
        .thawAfter(
            new Snippet() {
              {
                Task task = new Task();
                task.setDueDate(dueDate.getMillis());
                assertFalse(task.hasDueTime());
                assertTrue(task.isOverdue());
              }
            });
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testSanity() {
    assertTrue(Priority.HIGH < Priority.MEDIUM);
    assertTrue(Priority.MEDIUM < Priority.LOW);
    assertTrue(Priority.LOW < Priority.NONE);

    ArrayList<Integer> reminderFlags = new ArrayList<>();
    reminderFlags.add(Task.NOTIFY_AFTER_DEADLINE);
    reminderFlags.add(Task.NOTIFY_AT_DEADLINE);
    reminderFlags.add(Task.NOTIFY_MODE_NONSTOP);

    // assert no duplicates
    assertEquals(new TreeSet<>(reminderFlags).size(), reminderFlags.size());
  }
}
