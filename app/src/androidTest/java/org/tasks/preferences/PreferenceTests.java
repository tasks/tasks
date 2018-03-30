package org.tasks.preferences;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

import android.annotation.SuppressLint;
import android.support.test.runner.AndroidJUnit4;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class PreferenceTests {

  @SuppressLint("NewApi")
  private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

  private Preferences preferences;

  @Before
  public void setUp() {
    preferences = new Preferences(getTargetContext(), null);
    preferences.clear();
    preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
  }

  @Test
  public void testNotQuietWhenQuietHoursDisabled() {
    preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 29, 8, 0, 1).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsQuietAtStartOfQuietHoursNoWrap() {
    setQuietHoursStart(18);
    setQuietHoursEnd(19);

    long dueDate = new DateTime(2015, 12, 29, 18, 0, 1).getMillis();

    assertEquals(
        new DateTime(2015, 12, 29, 19, 0).getMillis(), preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsQuietAtStartOfQuietHoursWrap() {
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 29, 22, 0, 1).getMillis();

    assertEquals(
        new DateTime(2015, 12, 30, 10, 0).getMillis(), preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testAdjustForQuietHoursNightWrap() {
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 29, 23, 30).getMillis();

    assertEquals(
        new DateTime(2015, 12, 30, 10, 0).getMillis(), preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testAdjustForQuietHoursMorningWrap() {
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 30, 7, 15).getMillis();

    assertEquals(
        new DateTime(2015, 12, 30, 10, 0).getMillis(), preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testAdjustForQuietHoursWhenStartAndEndAreSame() {
    setQuietHoursStart(18);
    setQuietHoursEnd(18);

    long dueDate = new DateTime(2015, 12, 29, 18, 0, 0).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsNotQuietAtEndOfQuietHoursNoWrap() {
    setQuietHoursStart(17);
    setQuietHoursEnd(18);

    long dueDate = new DateTime(2015, 12, 29, 18, 0).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsNotQuietAtEndOfQuietHoursWrap() {
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 29, 10, 0).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsNotQuietBeforeNoWrap() {
    setQuietHoursStart(17);
    setQuietHoursEnd(18);

    long dueDate = new DateTime(2015, 12, 29, 11, 30).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsNotQuietAfterNoWrap() {
    setQuietHoursStart(17);
    setQuietHoursEnd(18);

    long dueDate = new DateTime(2015, 12, 29, 22, 15).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  @Test
  public void testIsNotQuietWrap() {
    setQuietHoursStart(22);
    setQuietHoursEnd(10);

    long dueDate = new DateTime(2015, 12, 29, 13, 45).getMillis();

    assertEquals(dueDate, preferences.adjustForQuietHours(dueDate));
  }

  private void setQuietHoursStart(int hour) {
    preferences.setInt(R.string.p_rmd_quietStart, hour * MILLIS_PER_HOUR);
  }

  private void setQuietHoursEnd(int hour) {
    preferences.setInt(R.string.p_rmd_quietEnd, hour * MILLIS_PER_HOUR);
  }
}
