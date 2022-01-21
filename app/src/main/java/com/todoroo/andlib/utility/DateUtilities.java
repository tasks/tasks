/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.utility;

import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import android.text.format.DateFormat;
import androidx.annotation.Nullable;
import com.todoroo.astrid.data.Task;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.locale.Locale;
import org.tasks.time.DateTime;

public class DateUtilities {

  /** Represents a single hour */
  public static final long ONE_HOUR = 3600000L;
  /** Represents a single day */
  public static final long ONE_DAY = 24 * ONE_HOUR;
  /** Represents a single week */
  public static final long ONE_WEEK = 7 * ONE_DAY;
  /** Represents a single minute */
  public static final long ONE_MINUTE = 60000L;

  static Boolean is24HourOverride = null;

  /** Returns unixtime for current time */
  public static long now() {
    return currentTimeMillis();
  }

  /* ======================================================================
   * =========================================================== formatters
   * ====================================================================== */

  private static boolean is24HourFormat(Context context) {
    return BuildConfig.DEBUG && is24HourOverride != null
        ? is24HourOverride
        : DateFormat.is24HourFormat(context);
  }

  public static String getTimeString(Context context, DateTime date) {
    String value;
    if (is24HourFormat(context)) {
      value = "HH:mm";
    } else if (date.getMinuteOfHour() == 0) {
      value = "h a";
    } else {
      value = "h:mm a";
    }
    return date.toString(value);
  }

  public static String getLongDateString(DateTime date, java.util.Locale locale) {
    return getFullDate(date, locale, FormatStyle.LONG);
  }

  /**
   * @param date date to format
   * @return date, with month, day, and year
   */
  public static String getDateString(Context context, DateTime date) {
    return getRelativeDay(
        context, date.getMillis(), Locale.getInstance().getLocale(), FormatStyle.MEDIUM);
  }

  static String getWeekday(DateTime date, java.util.Locale locale) {
    return date.toLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
  }

  /** @return weekday */
  public static String getWeekdayShort(DateTime date, java.util.Locale locale) {
    return date.toLocalDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale);
  }

  public static String getLongDateStringWithTime(long timestamp, java.util.Locale locale) {
    return getFullDateTime(newDateTime(timestamp), locale, FormatStyle.LONG);
  }

  public static String getRelativeDateTime(
      Context context, long date, java.util.Locale locale, FormatStyle style) {
    return getRelativeDateTime(context, date, locale, style, false, false);
  }

  public static String getRelativeDateTime(
          Context context, long date, java.util.Locale locale, FormatStyle style, boolean lowercase) {
    return getRelativeDateTime(context, date, locale, style, false, lowercase);
  }

  public static String getRelativeDateTime(
      Context context, long date, java.util.Locale locale, FormatStyle style, boolean alwaysDisplayFullDate, boolean lowercase) {

    if(alwaysDisplayFullDate || !isWithinSixDays(date)) {
      return Task.hasDueTime(date)
              ? getFullDateTime(newDateTime(date), locale, style)
              : getFullDate(newDateTime(date), locale, style);
    }

    String day = getRelativeDay(context, date, locale, isAbbreviated(style), lowercase);
    if (Task.hasDueTime(date)) {
      String time = getTimeString(context, newDateTime(date));
      return newDateTime().startOfDay().equals(newDateTime(date).startOfDay()) ? time : String.format("%s %s", day, time);
    } else {
      return day;
    }
  }

  private static boolean isAbbreviated(FormatStyle style) {
    return style == FormatStyle.SHORT || style == FormatStyle.MEDIUM;
  }

  public static String getRelativeDay(
      Context context,
      long date,
      java.util.Locale locale,
      FormatStyle style) {
    return getRelativeDay(context, date, locale, style, false,false);
  }

  public static String getRelativeDay(
      Context context,
      long date,
      java.util.Locale locale,
      FormatStyle style,
      boolean alwaysDisplayFullDate,
      boolean lowercase) {
    if(alwaysDisplayFullDate) {
      return getFullDate(newDateTime(date), locale, style);
    }
    return isWithinSixDays(date)
        ? getRelativeDay(context, date, locale, isAbbreviated(style), lowercase)
        : getFullDate(newDateTime(date), locale, style);
  }

  /**
   * returns a String with the amount of time until date in the format:
   *    nD, nH, nM where n is the number of days, months, or years
   * "3D" for 3 days, "6H" for six hours, "52M" for 50 minutes
   *  Minutes is the lowest denominator, Days is the highest
   *
   * @param date
   *  The date we want to find the time difference of
   * @return
   *  A formatted string of the time until date
   */
  public static String getTimeUntil(long date, java.util.Locale locale) {
    DateTime startOfToday = newDateTime();
    DateTime startOfDate = newDateTime(date);

    //difference in milliseconds between dates
    long diff = Math.abs(startOfDate.getMillis()-startOfToday.getMillis());

    int num;
    String unit;

    if (diff >= DateUtilities.ONE_DAY) {
      num = (int) (diff / DateUtilities.ONE_DAY);
      unit = "D";
    } else if (diff >= DateUtilities.ONE_HOUR) {
      num = (int) (diff / DateUtilities.ONE_HOUR);
      unit = "H";
    } else {
      num = (int) (diff / DateUtilities.ONE_MINUTE);
      unit = "M";
    }

    return String.format(locale, "%d%s", num, unit);
  }

  private static String getFullDate(DateTime date, java.util.Locale locale, FormatStyle style) {
    return stripYear(
        DateTimeFormatter.ofLocalizedDate(style)
            .withLocale(locale)
            .format(date.toLocalDate()),
        newDateTime().getYear());
  }

  private static String getFullDateTime(DateTime date, java.util.Locale locale, FormatStyle style) {
    return stripYear(
        DateTimeFormatter.ofLocalizedDateTime(style, FormatStyle.SHORT)
            .withLocale(locale)
            .format(date.toLocalDateTime()),
        newDateTime().getYear());
  }

  private static String stripYear(String date, int year) {
    return date.replaceAll("(?: de |, |/| )?" + year + "(?:年|년 | г\\.)?", "");
  }

   private static @Nullable String getRelativeDay(Context context, long date, java.util.Locale locale, boolean abbreviated, boolean lowercase) {
    DateTime startOfToday = newDateTime().startOfDay();
    DateTime startOfDate = newDateTime(date).startOfDay();

    if (startOfToday.equals(startOfDate)) {
      return context.getString(lowercase ? R.string.today_lowercase : R.string.today);
    }

    if (startOfToday.plusDays(1).equals(startOfDate)) {
      return context.getString(
          abbreviated
              ? lowercase ? R.string.tomorrow_abbrev_lowercase : R.string.tmrw
              : lowercase ? R.string.tomorrow_lowercase : R.string.tomorrow);
    }

    if (startOfDate.plusDays(1).equals(startOfToday)) {
      return context.getString(
          abbreviated
              ? lowercase ? R.string.yesterday_abbrev_lowercase : R.string.yest
              : lowercase ? R.string.yesterday_lowercase : R.string.yesterday);
    }

    DateTime dateTime = newDateTime(date);
    return abbreviated
         ? DateUtilities.getWeekdayShort(dateTime, locale)
         : DateUtilities.getWeekday(dateTime, locale);
  }

  private static boolean isWithinSixDays(long date){
    DateTime startOfToday = newDateTime().startOfDay();
    DateTime startOfDate = newDateTime(date).startOfDay();
    return Math.abs(startOfToday.getMillis() - startOfDate.getMillis()) <= DateUtilities.ONE_DAY * 6;
  }

}
