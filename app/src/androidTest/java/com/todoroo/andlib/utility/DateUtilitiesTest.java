/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.utility;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.todoroo.andlib.utility.DateUtilities.getDateString;
import static com.todoroo.andlib.utility.DateUtilities.getTimeString;
import static com.todoroo.andlib.utility.DateUtilities.getWeekday;
import static com.todoroo.andlib.utility.DateUtilities.getWeekdayShort;
import static junit.framework.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.date.DateTimeUtils.newDate;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Locale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Snippet;
import org.tasks.time.DateTime;
import org.threeten.bp.format.FormatStyle;

@RunWith(AndroidJUnit4.class)
public class DateUtilitiesTest {

  @After
  public void after() {
    DateUtilities.is24HourOverride = null;
  }

  @Test
  public void testGet24HourTime() {
    DateUtilities.is24HourOverride = true;
    assertEquals("09:05", getTimeString(null, new DateTime(2014, 1, 4, 9, 5, 36)));
    assertEquals("13:00", getTimeString(null, new DateTime(2014, 1, 4, 13, 0, 1)));
  }

  @Test
  public void testGetTime() {
    DateUtilities.is24HourOverride = false;
    assertEquals("9:05 AM", getTimeString(null, new DateTime(2014, 1, 4, 9, 5, 36)));
    assertEquals("1:05 PM", getTimeString(null, new DateTime(2014, 1, 4, 13, 5, 36)));
  }

  @Test
  public void testGetTimeWithNoMinutes() {
    DateUtilities.is24HourOverride = false;
    assertEquals("1 PM", getTimeString(null, new DateTime(2014, 1, 4, 13, 0, 59))); // derp?
  }

  @Test
  public void testGetDateStringWithYear() {
    assertEquals("Jan 4, 2014", getDateString(getApplicationContext(), new DateTime(2014, 1, 4, 0, 0, 0)));
  }

  @Test
  public void testGetDateStringHidingYear() {
    freezeAt(newDate(2014, 2, 1))
        .thawAfter(
            new Snippet() {
              {
                assertEquals("Jan 1", getDateString(getApplicationContext(), new DateTime(2014, 1, 1)));
              }
            });
  }

  @Test
  public void testGetDateStringWithDifferentYear() {
    freezeAt(newDate(2013, 12, 1))
        .thawAfter(
            new Snippet() {
              {
                assertEquals("Jan 1, 2014", getDateString(getApplicationContext(),new DateTime(2014, 1, 1, 0, 0, 0)));
              }
            });
  }

  @Test
  public void testGetWeekdayLongString() {
    assertEquals("Sunday", getWeekday(newDate(2013, 12, 29), Locale.US));
    assertEquals("Monday", getWeekday(newDate(2013, 12, 30), Locale.US));
    assertEquals("Tuesday", getWeekday(newDate(2013, 12, 31), Locale.US));
    assertEquals("Wednesday", getWeekday(newDate(2014, 1, 1), Locale.US));
    assertEquals("Thursday", getWeekday(newDate(2014, 1, 2), Locale.US));
    assertEquals("Friday", getWeekday(newDate(2014, 1, 3), Locale.US));
    assertEquals("Saturday", getWeekday(newDate(2014, 1, 4), Locale.US));
  }

  @Test
  public void testGetWeekdayShortString() {
    assertEquals("Sun", getWeekdayShort(newDate(2013, 12, 29), Locale.US));
    assertEquals("Mon", getWeekdayShort(newDate(2013, 12, 30), Locale.US));
    assertEquals("Tue", getWeekdayShort(newDate(2013, 12, 31), Locale.US));
    assertEquals("Wed", getWeekdayShort(newDate(2014, 1, 1), Locale.US));
    assertEquals("Thu", getWeekdayShort(newDate(2014, 1, 2), Locale.US));
    assertEquals("Fri", getWeekdayShort(newDate(2014, 1, 3), Locale.US));
    assertEquals("Sat", getWeekdayShort(newDate(2014, 1, 4), Locale.US));
  }

  @Test
  public void getRelativeFullDate() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                assertEquals(
                    "Sunday, January 14",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.US,
                        FormatStyle.FULL)));
  }

  @Test
  public void getRelativeFullDateWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                assertEquals(
                    "Sunday, January 14, 2018",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.US,
                        FormatStyle.FULL)));
  }

  @Test
  public void getRelativeFullDateTime() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                assertEquals(
                    "Sunday, January 14 1:43 PM",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 43, 1).getMillis(),
                        Locale.US,
                        FormatStyle.FULL)));
  }

  @Test
  public void getRelativeFullDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                assertEquals(
                    "Sunday, January 14, 2018 11:50 AM",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 11, 50, 1).getMillis(),
                        Locale.US,
                        FormatStyle.FULL)));
  }

  @Test
  public void germanDateNoYear() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "Sonntag, 14. Januar",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.GERMAN,
                        FormatStyle.FULL)));
  }

  @Test
  public void germanDateWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "Sonntag, 14. Januar 2018",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.GERMAN,
                        FormatStyle.FULL)));
  }

  @Test
  public void koreanDateNoYear() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "1월 14일 일요일",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.KOREAN,
                        FormatStyle.FULL)));
  }

  @Test
  public void koreanDateWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "2018년 1월 14일 일요일",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.KOREAN,
                        FormatStyle.FULL)));
  }

  @Test
  public void japaneseDateNoYear() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "1月14日日曜日",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.JAPANESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void japaneseDateWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "2018年1月14日日曜日",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.JAPANESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void chineseDateNoYear() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "1月14日星期日",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.CHINESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void chineseDateWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "2018年1月14日星期日",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14).getMillis(),
                        Locale.CHINESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void chineseDateTimeNoYear() {
    freezeAt(new DateTime(2018, 1, 1))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "1月14日星期日 上午11:53",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 11, 53, 1).getMillis(),
                        Locale.CHINESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void chineseDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "2018年1月14日星期日 下午1:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.CHINESE,
                        FormatStyle.FULL)));
  }

  @Test
  public void frenchDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "dimanche 14 janvier 2018 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.FRENCH,
                        FormatStyle.FULL)));
  }

  @Test
  public void indiaDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "रविवार, 14 जनवरी 2018 1:45 pm",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("hi-IN"),
                        FormatStyle.FULL)));
  }

  @Test
  public void russiaDateTimeNoYear() {
    freezeAt(new DateTime(2018, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "воскресенье, 14 января 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("ru"),
                        FormatStyle.FULL)));
  }

  @Test
  public void russiaDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "воскресенье, 14 января 2018 г. 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("ru"),
                        FormatStyle.FULL)));
  }

  @Test
  public void brazilDateTimeNoYear() {
    freezeAt(new DateTime(2018, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "domingo, 14 de janeiro 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("pt-br"),
                        FormatStyle.FULL)));
  }

  @Test
  public void brazilDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "domingo, 14 de janeiro de 2018 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("pt-br"),
                        FormatStyle.FULL)));
  }

  @Test
  public void spainDateTimeNoYear() {
    freezeAt(new DateTime(2018, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "domingo, 14 de enero 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("es"),
                        FormatStyle.FULL)));
  }

  @Test
  public void spainDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "domingo, 14 de enero de 2018 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("es"),
                        FormatStyle.FULL)));
  }

  @Test
  public void hebrewDateTimeNoYear() {
    freezeAt(new DateTime(2018, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "יום ראשון, 14 בינואר 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("iw"),
                        FormatStyle.FULL)));
  }

  @Test
  public void hebrewDateTimeWithYear() {
    freezeAt(new DateTime(2017, 12, 12))
        .thawAfter(
            () ->
                Assert.assertEquals(
                    "יום ראשון, 14 בינואר 2018 13:45",
                    DateUtilities.getRelativeDateTime(
                        getApplicationContext(),
                        new DateTime(2018, 1, 14, 13, 45, 1).getMillis(),
                        Locale.forLanguageTag("iw"),
                        FormatStyle.FULL)));
  }
}
