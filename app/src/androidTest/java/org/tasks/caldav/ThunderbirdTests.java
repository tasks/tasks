package org.tasks.caldav;

import static junit.framework.Assert.assertEquals;
import static org.tasks.TestUtilities.vtodo;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.data.Task.Priority;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class ThunderbirdTests {

  private TimeZone defaultTimeZone = TimeZone.getDefault();

  @Before
  public void before() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
  }

  @After
  public void after() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  public void readTitle() {
    assertEquals("Test title", vtodo("thunderbird/basic_no_due_date.txt").getTitle());
  }

  @Test
  public void readDescription() {
    assertEquals("Test description", vtodo("thunderbird/basic_no_due_date.txt").getNotes());
  }

  @Test
  public void readCreationDate() {
    assertEquals(
        new DateTime(2018, 4, 17, 11, 31, 52).getMillis(),
        (long) vtodo("thunderbird/basic_no_due_date.txt").getCreationDate());
  }

  @Test
  public void readDueDate() {
    assertEquals(
        new DateTime(2018, 4, 17, 14, 0, 1).getMillis(),
        (long) vtodo("thunderbird/basic_due_date.txt").getDueDate());
  }

  @Test
  public void completed() {
    assertEquals(
        new DateTime(2018, 4, 17, 16, 24, 29).getMillis(),
        (long) vtodo("thunderbird/basic_completed.txt").getCompletionDate());
  }

  @Test
  public void repeatDaily() {
    assertEquals(
        "RRULE:FREQ=DAILY;INTERVAL=1", vtodo("thunderbird/repeat_daily.txt").getRecurrence());
  }

  @Test
  public void priorityNotSet() {
    assertEquals(Priority.NONE, (int) vtodo("thunderbird/basic_no_due_date.txt").getPriority());
  }

  @Test
  public void priorityNotSpecified() {
    assertEquals(Priority.NONE, (int) vtodo("thunderbird/priority_unspecified.txt").getPriority());
  }

  @Test
  public void lowPriority() {
    assertEquals(Priority.LOW, (int) vtodo("thunderbird/priority_low.txt").getPriority());
  }

  @Test
  public void normalPriority() {
    assertEquals(Priority.MEDIUM, (int) vtodo("thunderbird/priority_normal.txt").getPriority());
  }

  @Test
  public void highPriority() {
    assertEquals(Priority.HIGH, (int) vtodo("thunderbird/priority_high.txt").getPriority());
  }
}
