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
public class NextCloudTests {

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
    assertEquals("Test title", vtodo("nextcloud/basic_no_due_date.txt").getTitle());
  }

  @Test
  public void readDescription() {
    assertEquals("Test description", vtodo("nextcloud/basic_no_due_date.txt").getNotes());
  }

  @Test
  public void readCreationDate() {
    assertEquals(
        new DateTime(2018, 4, 17, 16, 32, 3).getMillis(),
        (long) vtodo("nextcloud/basic_no_due_date.txt").getCreationDate());
  }

  @Test
  public void readDueDate() {
    assertEquals(
        new DateTime(2018, 4, 17, 17, 0, 1).getMillis(),
        (long) vtodo("nextcloud/basic_due_date.txt").getDueDate());
  }

  @Test
  public void priorityNoStars() {
    assertEquals(Priority.NONE, (int) vtodo("nextcloud/priority_no_stars.txt").getPriority());
  }

  @Test
  public void priorityOneStar() {
    assertEquals(Priority.LOW, (int) vtodo("nextcloud/priority_1_star.txt").getPriority());
  }

  @Test
  public void priorityTwoStars() {
    assertEquals(Priority.LOW, (int) vtodo("nextcloud/priority_2_stars.txt").getPriority());
  }

  @Test
  public void priorityThreeStars() {
    assertEquals(Priority.LOW, (int) vtodo("nextcloud/priority_3_stars.txt").getPriority());
  }

  @Test
  public void priorityFourStars() {
    assertEquals(Priority.LOW, (int) vtodo("nextcloud/priority_4_stars.txt").getPriority());
  }

  @Test
  public void priorityFiveStars() {
    assertEquals(Priority.MEDIUM, (int) vtodo("nextcloud/priority_5_stars.txt").getPriority());
  }

  @Test
  public void prioritySixStars() {
    assertEquals(Priority.HIGH, (int) vtodo("nextcloud/priority_6_stars.txt").getPriority());
  }

  @Test
  public void prioritySevenStars() {
    assertEquals(Priority.HIGH, (int) vtodo("nextcloud/priority_7_stars.txt").getPriority());
  }

  @Test
  public void priorityEightStars() {
    assertEquals(Priority.HIGH, (int) vtodo("nextcloud/priority_8_stars.txt").getPriority());
  }

  @Test
  public void priorityNineStars() {
    assertEquals(Priority.HIGH, (int) vtodo("nextcloud/priority_9_stars.txt").getPriority());
  }
}
