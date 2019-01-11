package org.tasks.caldav;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.TimeZone;

import androidx.test.runner.AndroidJUnit4;

import static junit.framework.Assert.assertTrue;
import static org.tasks.TestUtilities.vtodo;

@RunWith(AndroidJUnit4.class)
public class SynologyTests {

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
  public void completedWithoutDueDate() {
    assertTrue(vtodo("synology/complete_no_due_date.txt").isCompleted());
  }

  @Test
  public void completedWithDueDate() {
    assertTrue(vtodo("synology/complete_with_date.txt").isCompleted());
  }

  @Test
  public void completedWithDateTime() {
    assertTrue(vtodo("synology/complete_with_date_time.txt").isCompleted());
  }
}
