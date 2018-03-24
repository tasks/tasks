package com.todoroo.andlib.utility;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.todoroo.andlib.utility.DateUtilities.getRelativeDay;
import static junit.framework.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;

import android.support.test.runner.AndroidJUnit4;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

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
    checkRelativeDay(new DateTime().minusDays(7), "Dec 24", "Dec 24");
  }

  @Test
  public void testRelativeDayOneWeekNextYear() {
    checkRelativeDay(new DateTime().plusDays(7), "Jan 7 '14", "Jan 7 '14");
  }

  private void checkRelativeDay(DateTime now, String full, String abbreviated) {
    assertEquals(full, getRelativeDay(getTargetContext(), now.getMillis(), false));
    assertEquals(abbreviated, getRelativeDay(getTargetContext(), now.getMillis(), true));
  }
}
