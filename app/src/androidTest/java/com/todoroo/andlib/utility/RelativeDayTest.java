package com.todoroo.andlib.utility;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.todoroo.andlib.utility.DateUtilities.getRelativeDay;
import static junit.framework.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;
import org.threeten.bp.format.FormatStyle;

@RunWith(AndroidJUnit4.class)
public class RelativeDayTest {

  private static final DateTime now = new DateTime(2013, 12, 31, 11, 9, 42, 357);
  private static Locale defaultLocale;

  @Before
  public void setUp() {
    defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    freezeAt(now);
  }

  @After
  public void tearDown() {
    Locale.setDefault(defaultLocale);
    thaw();
  }

  @Test
  public void testRelativeDayIsToday() {
    checkRelativeDay(new DateTime(), "Today", "Today");
  }

  @Test
  public void testRelativeDayIsTomorrow() {
    checkRelativeDay(new DateTime().plusDays(1), "Tomorrow", "Tmrw");
  }

  @Test
  public void testRelativeDayIsYesterday() {
    checkRelativeDay(new DateTime().minusDays(1), "Yesterday", "Yest");
  }

  @Test
  public void testRelativeDayTwo() {
    checkRelativeDay(new DateTime().minusDays(2), "Sunday", "Sun");
    checkRelativeDay(new DateTime().plusDays(2), "Thursday", "Thu");
  }

  @Test
  public void testRelativeDaySix() {
    checkRelativeDay(new DateTime().minusDays(6), "Wednesday", "Wed");
    checkRelativeDay(new DateTime().plusDays(6), "Monday", "Mon");
  }

  @Test
  public void testRelativeDayOneWeek() {
    checkRelativeDay(new DateTime().minusDays(7), "December 24", "Dec 24");
  }

  @Test
  public void testRelativeDayOneWeekNextYear() {
    checkRelativeDay(new DateTime().plusDays(7), "January 7, 2014", "Jan 7, 2014");
  }

  private void checkRelativeDay(DateTime now, String full, String abbreviated) {
    assertEquals(
        full,
        getRelativeDay(getApplicationContext(), now.getMillis(), Locale.US, FormatStyle.LONG));
    assertEquals(
        abbreviated,
        getRelativeDay(getApplicationContext(), now.getMillis(), Locale.US, FormatStyle.MEDIUM));
  }
}
