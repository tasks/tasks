package com.todoroo.astrid.gtasks.api;

import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksCompletedTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksDueTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksCompletionTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksDueDate;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import android.support.test.runner.AndroidJUnit4;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class GtasksApiUtilitiesTest {

  private static final Locale defaultLocale = Locale.getDefault();
  private static final TimeZone defaultDateTimeZone = TimeZone.getDefault();

  @Before
  public void setUp() {
    Locale.setDefault(Locale.US);
    TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
  }

  @After
  public void tearDown() {
    Locale.setDefault(defaultLocale);
    TimeZone.setDefault(defaultDateTimeZone);
  }

  @Test
  public void testConvertUnixToGoogleCompletionTime() {
    long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
    assertEquals(now, unixTimeToGtasksCompletionTime(now).getValue());
  }

  @Test
  public void testConvertGoogleCompletedTimeToUnixTime() {
    long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
    com.google.api.client.util.DateTime gtime = new com.google.api.client.util.DateTime(now);
    assertEquals(now, gtasksCompletedTimeToUnixTime(gtime));
  }

  @Test
  public void testConvertDueDateTimeToGoogleDueDate() {
    DateTime now = new DateTime(2014, 1, 8, 8, 53, 20, 109);

    assertEquals(
        new DateTime(2014, 1, 8, 0, 0, 0, 0, TimeZone.getTimeZone("GMT")).getMillis(),
        unixTimeToGtasksDueDate(now.getMillis()).getValue());
  }

  @Test
  public void testConvertGoogleDueDateToUnixTime() {
    com.google.api.client.util.DateTime googleDueDate =
        new com.google.api.client.util.DateTime(
            new Date(new DateTime(2014, 1, 8, 0, 0, 0, 0).getMillis()),
            TimeZone.getTimeZone("GMT"));

    assertEquals(
        new DateTime(2014, 1, 8, 6, 0, 0, 0).getMillis(), gtasksDueTimeToUnixTime(googleDueDate));
  }

  @Test
  public void testConvertToInvalidGtaskTimes() {
    assertNull(unixTimeToGtasksCompletionTime(-1));
    assertNull(unixTimeToGtasksDueDate(-1));
  }

  @Test
  public void testConvertFromInvalidGtaskTimes() {
    assertEquals(0, gtasksCompletedTimeToUnixTime(null));
    assertEquals(0, gtasksDueTimeToUnixTime(null));
  }
}
