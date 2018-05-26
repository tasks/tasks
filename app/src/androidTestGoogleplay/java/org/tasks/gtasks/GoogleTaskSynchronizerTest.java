package org.tasks.gtasks;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DUE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DUE_TIME;
import static junit.framework.Assert.assertEquals;
import static org.tasks.gtasks.GoogleTaskSynchronizer.mergeDates;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.HIDE_TYPE;
import static org.tasks.makers.TaskMaker.newTask;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.data.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class GoogleTaskSynchronizerTest {

  @Test
  public void testMergeDate() {
    Task local = newTask(with(DUE_DATE, new DateTime(2016, 3, 12)));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 11))).getDueDate(), local);

    assertEquals(new DateTime(2016, 3, 11, 12, 0).getMillis(), local.getDueDate().longValue());
  }

  @Test
  public void testMergeTime() {
    Task local = newTask(with(DUE_TIME, new DateTime(2016, 3, 11, 13, 30)));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 11))).getDueDate(), local);

    assertEquals(new DateTime(2016, 3, 11, 13, 30, 1).getMillis(), local.getDueDate().longValue());
  }

  @Test
  public void testDueDateAdjustHideBackwards() {
    Task local =
        newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 11))).getDueDate(), local);

    assertEquals(new DateTime(2016, 3, 11).getMillis(), local.getHideUntil().longValue());
  }

  @Test
  public void testDueDateAdjustHideForwards() {
    Task local =
        newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 14))).getDueDate(), local);

    assertEquals(new DateTime(2016, 3, 14).getMillis(), local.getHideUntil().longValue());
  }

  @Test
  public void testDueTimeAdjustHideBackwards() {
    Task local =
        newTask(
            with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
            with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 11))).getDueDate(), local);

    assertEquals(
        new DateTime(2016, 3, 11, 13, 30, 1).getMillis(), local.getHideUntil().longValue());
  }

  @Test
  public void testDueTimeAdjustTimeForwards() {
    Task local =
        newTask(
            with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
            with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

    mergeDates(newTask(with(DUE_DATE, new DateTime(2016, 3, 14))).getDueDate(), local);

    assertEquals(
        new DateTime(2016, 3, 14, 13, 30, 1).getMillis(), local.getHideUntil().longValue());
  }

  @Test
  public void testDueDateClearHide() {
    Task local =
        newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

    mergeDates(newTask().getDueDate(), local);

    assertEquals(0, local.getHideUntil().longValue());
  }

  @Test
  public void testDueTimeClearHide() {
    Task local =
        newTask(
            with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
            with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

    mergeDates(newTask().getDueDate(), local);

    assertEquals(0, local.getHideUntil().longValue());
  }
}
