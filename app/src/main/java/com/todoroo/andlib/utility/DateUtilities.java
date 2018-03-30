/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import android.text.format.DateFormat;
import com.todoroo.astrid.data.Task;
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

  private static final long abbreviationLimit = DateUtilities.ONE_DAY * 6;
  private static final String JA = "MMM d\u65E5";
  private static final String JA_YEAR = "yy\u5E74 " + JA;
  private static final String KO = "MMM d\uC77C";
  private static final String KO_YEAR = "yy\uB144 " + KO;
  private static final String ZH = "MMM d\u65E5";
  private static final String ZH_YEAR = "yy\u5E74 " + ZH;
  static Boolean is24HourOverride = null;

  /**
   * Add the specified amount of months to the given time.<br>
   * The day of month will stay the same.<br>
   *
   * @param time the base-time (in milliseconds) to which the amount of months is added
   * @param interval the amount of months to be added
   * @return the calculated time in milliseconds
   */
  public static long addCalendarMonthsToUnixtime(long time, int interval) {
    DateTime dt = new DateTime(time);
    DateTime result = dt.plusMonths(interval);
    // preserving java.util.date behavior
    int diff = dt.getDayOfMonth() - result.getDayOfMonth();
    if (diff > 0) {
      result = result.plusDays(diff);
    }
    return result.getMillis();
  }

  /** Returns unixtime for current time */
  public static long now() {
    return currentTimeMillis();
  }

  /* ======================================================================
   * =========================================================== formatters
   * ====================================================================== */

  /** Returns unixtime one month from now */
  public static long oneMonthFromNow() {
    return addCalendarMonthsToUnixtime(currentTimeMillis(), 1);
  }

  public static boolean is24HourFormat(Context context) {
    if (is24HourOverride != null) {
      return is24HourOverride;
    }

    return DateFormat.is24HourFormat(context);
  }

  public static String getTimeString(Context context, long timestamp) {
    return getTimeString(context, newDateTime(timestamp));
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

  public static String getLongDateString(DateTime date) {
    return getDateString("MMMM", date);
  }

  /**
   * @param date date to format
   * @return date, with month, day, and year
   */
  public static String getDateString(DateTime date) {
    return getDateString("MMM", date);
  }

  private static String getDateString(String simpleDateFormat, DateTime date) {
    boolean includeYear = date.getYear() != newDateTime().getYear();
    String format = getFormat(Locale.getInstance(), simpleDateFormat, includeYear);
    return date.toString(format);
  }

  private static String getFormat(Locale locale, String monthFormat, boolean includeYear) {
    switch (locale.getLanguage()) {
      case "ja":
        return includeYear ? JA_YEAR : JA;
      case "ko":
        return includeYear ? KO_YEAR : KO;
      case "zh":
        return includeYear ? ZH_YEAR : ZH;
    }
    switch (locale.getCountry()) {
      case "BZ":
      case "CA":
      case "KE":
      case "MN":
      case "US":
        return includeYear ? monthFormat + " d ''yy" : monthFormat + " d";
      default:
        return includeYear ? "d " + monthFormat + " ''yy" : "d " + monthFormat;
    }
  }

  /** @return weekday */
  public static String getWeekday(DateTime date) {
    return date.toString("EEEE");
  }

  /** @return weekday */
  public static String getWeekdayShort(DateTime date) {
    return date.toString("EEE");
  }

  public static String getLongDateStringWithTime(Context context, long timestamp) {
    DateTime date = newDateTime(timestamp);
    return getLongDateString(date) + ", " + getTimeString(context, date);
  }

  public static String getDateStringWithTime(Context context, long timestamp) {
    DateTime date = newDateTime(timestamp);
    return getDateString(date) + ", " + getTimeString(context, date);
  }

  public static String getRelativeDateStringWithTime(Context context, long timestamp) {
    String string = DateUtilities.getRelativeDay(context, timestamp, false);
    if (Task.hasDueTime(timestamp)) {
      string =
          String.format(
              "%s %s",
              string, // $NON-NLS-1$
              DateUtilities.getTimeString(context, timestamp));
    }
    return string;
  }

  /** @return yesterday, today, tomorrow, or null */
  public static String getRelativeDay(Context context, long date, boolean abbreviated) {
    long today = getStartOfDay(currentTimeMillis());
    long input = getStartOfDay(date);

    if (today == input) {
      return context.getString(R.string.today);
    }

    if (today + ONE_DAY == input) {
      return context.getString(abbreviated ? R.string.tmrw : R.string.tomorrow);
    }

    if (today == input + ONE_DAY) {
      return context.getString(abbreviated ? R.string.yest : R.string.yesterday);
    }

    if (today + abbreviationLimit >= input && today - abbreviationLimit <= input) {
      return abbreviated
          ? DateUtilities.getWeekdayShort(newDateTime(date))
          : DateUtilities.getWeekday(newDateTime(date));
    }

    return getDateString(newDateTime(date));
  }

  public static long getStartOfDay(long time) {
    return newDateTime(time).startOfDay().getMillis();
  }
}
