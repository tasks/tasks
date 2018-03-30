package org.tasks.date;

import static junit.framework.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.date.DateTimeUtils.newDateUtc;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.support.test.runner.AndroidJUnit4;
import java.util.TimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Snippet;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class DateTimeUtilsTest {

  private final DateTime now = new DateTime(2014, 1, 1, 15, 17, 53, 0);

  @Test
  public void testGetCurrentTime() {
    freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(now.getMillis(), currentTimeMillis());
              }
            });
  }

  @Test
  public void testCreateNewUtcDate() {
    DateTime utc = now.toUTC();
    DateTime actual =
        newDateUtc(
            utc.getYear(),
            utc.getMonthOfYear(),
            utc.getDayOfMonth(),
            utc.getHourOfDay(),
            utc.getMinuteOfHour(),
            utc.getSecondOfMinute());
    assertEquals(utc.getMillis(), actual.getMillis());
  }

  @Test
  public void testIllegalInstant() {
    new DateTime(2015, 7, 24, 0, 0, 0, 0, TimeZone.getTimeZone("Africa/Cairo"));
    new DateTime(2015, 10, 18, 0, 0, 0, 0, TimeZone.getTimeZone("America/Sao_Paulo"));
    new DateTime(2015, 10, 4, 0, 0, 0, 0, TimeZone.getTimeZone("America/Asuncion"));
  }
}
