package org.tasks.time;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.support.test.runner.AndroidJUnit4;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Freeze;
import org.tasks.Snippet;

@RunWith(AndroidJUnit4.class)
public class DateTimeTest {

  @Test
  public void testGetMillisOfDay() {
    assertEquals(7248412, new DateTime(2015, 10, 6, 2, 0, 48, 412).getMillisOfDay());
  }

  @Test
  public void testWithMillisOfDay() {
    assertEquals(
        new DateTime(2015, 10, 6, 2, 0, 48, 412),
        new DateTime(2015, 10, 6, 0, 0, 0, 0).withMillisOfDay(7248412));
  }

  @Test
  public void testWithMillisOfDayDuringDST() {
    TimeZone def = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
      assertEquals(
          2,
          new DateTime(2015, 10, 31, 2, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(2))
              .getHourOfDay());
    } finally {
      TimeZone.setDefault(def);
    }
  }

  @Test
  public void testWithMillisOfDayAfterDST() {
    TimeZone def = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
      assertEquals(
          2,
          new DateTime(2015, 11, 2, 2, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(2))
              .getHourOfDay());
    } finally {
      TimeZone.setDefault(def);
    }
  }

  @Test
  public void testWithMillisOfDayStartDST() {
    TimeZone def = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
      assertEquals(
          1,
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(1))
              .getHourOfDay());
      assertEquals(
          3,
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(2))
              .getHourOfDay());
      assertEquals(
          3,
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(3))
              .getHourOfDay());
      assertEquals(
          4,
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(4))
              .getHourOfDay());

      assertEquals(
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(2))
              .getMillis(),
          new DateTime(2015, 3, 8, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(3))
              .getMillis());
    } finally {
      TimeZone.setDefault(def);
    }
  }

  @Test
  public void testWithMillisOfDayEndDST() {
    TimeZone def = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
      assertEquals(
          1,
          new DateTime(2015, 11, 1, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(1))
              .getHourOfDay());
      assertEquals(
          2,
          new DateTime(2015, 11, 1, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(2))
              .getHourOfDay());
      assertEquals(
          3,
          new DateTime(2015, 11, 1, 0, 0, 0)
              .withMillisOfDay((int) TimeUnit.HOURS.toMillis(3))
              .getHourOfDay());
    } finally {
      TimeZone.setDefault(def);
    }
  }

  @Test
  public void testPlusMonths() {
    assertEquals(
        new DateTime(2015, 11, 6, 2, 0, 48, 412),
        new DateTime(2015, 10, 6, 2, 0, 48, 412).plusMonths(1));
  }

  @Test
  public void testPlusMonthsWrapYear() {
    assertEquals(
        new DateTime(2016, 1, 6, 2, 0, 48, 412),
        new DateTime(2015, 10, 6, 2, 0, 48, 412).plusMonths(3));
  }

  @Test
  public void testGetDayOfMonth() {
    assertEquals(5, new DateTime(2015, 10, 5, 0, 0, 0).getDayOfMonth());
  }

  @Test
  public void testPlusDays() {
    assertEquals(
        new DateTime(2015, 10, 6, 2, 0, 48, 412),
        new DateTime(2015, 10, 5, 2, 0, 48, 412).plusDays(1));
  }

  @Test
  public void testPlusDaysWrapMonth() {
    assertEquals(
        new DateTime(2015, 11, 1, 2, 0, 48, 412),
        new DateTime(2015, 10, 31, 2, 0, 48, 412).plusDays(1));
  }

  @Test
  public void testMinuteOfHour() {
    assertEquals(43, new DateTime(2015, 10, 5, 2, 43, 48).getMinuteOfHour());
  }

  @Test
  public void testIsEndOfMonth() {
    assertTrue(new DateTime(2014, 1, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 2, 28, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 3, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 4, 30, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 5, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 6, 30, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 7, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 8, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 9, 30, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 10, 31, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 11, 30, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2014, 12, 31, 0, 0, 0).isLastDayOfMonth());
  }

  @Test
  public void testNotTheEndOfTheMonth() {
    for (int month = 1; month <= 12; month++) {
      int lastDay = new DateTime(2014, month, 1, 0, 0, 0, 0).getNumberOfDaysInMonth();
      for (int day = 1; day < lastDay; day++) {
        assertFalse(new DateTime(2014, month, day, 0, 0, 0).isLastDayOfMonth());
      }
    }
  }

  @Test
  public void testCheckEndOfMonthDuringLeapYear() {
    assertFalse(new DateTime(2016, 2, 28, 0, 0, 0).isLastDayOfMonth());
    assertTrue(new DateTime(2016, 2, 29, 0, 0, 0).isLastDayOfMonth());
  }

  @Test
  public void testNumberOfDaysInMonth() {
    assertEquals(31, new DateTime(2015, 1, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(28, new DateTime(2015, 2, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 3, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(30, new DateTime(2015, 4, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 5, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(30, new DateTime(2015, 6, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 7, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 8, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(30, new DateTime(2015, 9, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 10, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(30, new DateTime(2015, 11, 5, 9, 45, 34).getNumberOfDaysInMonth());
    assertEquals(31, new DateTime(2015, 12, 5, 9, 45, 34).getNumberOfDaysInMonth());
  }

  @Test
  public void testWithMillisOfSecond() {
    assertEquals(
        new DateTime(2015, 11, 6, 13, 34, 56, 453),
        new DateTime(2015, 11, 6, 13, 34, 56, 0).withMillisOfSecond(453));
  }

  @Test
  public void testWithHourOfDay() {
    assertEquals(
        new DateTime(2015, 11, 6, 23, 0, 0), new DateTime(2015, 11, 6, 1, 0, 0).withHourOfDay(23));
  }

  @Test
  public void testWithMinuteOfHour() {
    assertEquals(
        new DateTime(2015, 11, 6, 23, 13, 0),
        new DateTime(2015, 11, 6, 23, 1, 0).withMinuteOfHour(13));
  }

  @Test
  public void testWithSecondOfMinute() {
    assertEquals(
        new DateTime(2015, 11, 6, 23, 13, 56),
        new DateTime(2015, 11, 6, 23, 13, 1).withSecondOfMinute(56));
  }

  @Test
  public void testGetYear() {
    assertEquals(2015, new DateTime(2015, 1, 1, 1, 1, 1).getYear());
  }

  @Test
  public void testMinusMinutes() {
    assertEquals(
        new DateTime(2015, 11, 4, 23, 59, 0), new DateTime(2015, 11, 5, 0, 1, 0).minusMinutes(2));
  }

  @Test
  public void testIsBefore() {
    assertTrue(new DateTime(2015, 11, 4, 23, 59, 0).isBefore(new DateTime(2015, 11, 4, 23, 59, 1)));

    assertFalse(
        new DateTime(2015, 11, 4, 23, 59, 0).isBefore(new DateTime(2015, 11, 4, 23, 59, 0)));
  }

  @Test
  public void testGetMonthOfYear() {
    assertEquals(1, new DateTime(2015, 1, 2, 3, 4, 5).getMonthOfYear());
  }

  @Test
  public void testIsAfter() {
    assertTrue(new DateTime(2015, 11, 4, 23, 59, 1).isAfter(new DateTime(2015, 11, 4, 23, 59, 0)));

    assertFalse(new DateTime(2015, 11, 4, 23, 59, 0).isAfter(new DateTime(2015, 11, 4, 23, 59, 0)));
  }

  @Test
  public void testWithYear() {
    assertEquals(
        new DateTime(2016, 1, 1, 1, 1, 1), new DateTime(2015, 1, 1, 1, 1, 1).withYear(2016));
  }

  @Test
  public void testWithMonthOfYear() {
    assertEquals(
        new DateTime(2015, 1, 2, 3, 4, 5), new DateTime(2015, 2, 2, 3, 4, 5).withMonthOfYear(1));
  }

  @Test
  public void testGetHourOfDay() {
    assertEquals(3, new DateTime(2015, 1, 2, 3, 4, 5).getHourOfDay());
  }

  @Test
  public void testWithDayOfMonth() {
    assertEquals(
        new DateTime(2015, 1, 2, 3, 4, 5), new DateTime(2015, 1, 1, 3, 4, 5).withDayOfMonth(2));
  }

  @Test
  public void testPlusMinutes() {
    assertEquals(
        new DateTime(2015, 1, 2, 3, 4, 5), new DateTime(2015, 1, 2, 2, 59, 5).plusMinutes(5));
  }

  @Test
  public void testPlusHours() {
    assertEquals(
        new DateTime(2015, 1, 2, 3, 4, 5), new DateTime(2015, 1, 1, 3, 4, 5).plusHours(24));
  }

  @Test
  public void testPlusWeeks() {
    assertEquals(
        new DateTime(2015, 1, 2, 3, 4, 5), new DateTime(2014, 12, 12, 3, 4, 5).plusWeeks(3));
  }

  @Test
  public void testIsBeforeNow() {
    Freeze.freezeAt(new DateTime(2015, 10, 6, 16, 15, 27))
        .thawAfter(
            new Snippet() {
              {
                assertFalse(new DateTime(2015, 10, 6, 16, 15, 27).isBeforeNow());
                assertTrue(new DateTime(2015, 10, 6, 16, 15, 26).isBeforeNow());
              }
            });
  }

  @Test
  public void testMinusMillis() {
    assertEquals(
        new DateTime(2015, 11, 6, 16, 18, 20, 452),
        new DateTime(2015, 11, 6, 16, 18, 21, 374).minusMillis(922));
  }

  @Test
  public void testMinusDays() {
    assertEquals(
        new DateTime(2015, 11, 6, 16, 19, 16), new DateTime(2015, 12, 4, 16, 19, 16).minusDays(28));

    assertEquals(
        new DateTime(2015, 11, 6, 16, 19, 16), new DateTime(2015, 11, 7, 16, 19, 16).minusDays(1));
  }

  @Test
  public void testGetSecondOfMinute() {
    assertEquals(32, new DateTime(2015, 11, 6, 16, 19, 32).getSecondOfMinute());
  }

  @Test
  public void testToUTC() {
    TimeZone def = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
      assertEquals(
          new DateTime(2015, 10, 6, 14, 45, 15, 0, TimeZone.getTimeZone("GMT")),
          new DateTime(2015, 10, 6, 9, 45, 15).toUTC());
    } finally {
      TimeZone.setDefault(def);
    }
  }

  @Test
  public void testStartOfMinute() {
    assertEquals(
        new DateTime(2017, 9, 3, 0, 51, 0, 0),
        new DateTime(2017, 9, 3, 0, 51, 13, 427).startOfMinute());
  }

  @Test
  public void testEndOfMinute() {
    assertEquals(
        new DateTime(2017, 9, 22, 14, 47, 59, 999),
        new DateTime(2017, 9, 22, 14, 47, 14, 453).endOfMinute());
  }
}
