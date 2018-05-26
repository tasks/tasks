package org.tasks.date;

import java.util.TimeZone;
import org.tasks.time.DateTime;

public class DateTimeUtils {

  public static DateTime newDate(int year, int month, int day) {
    return new DateTime(year, month, day, 0, 0, 0);
  }

  public static DateTime newDateUtc(
      int year, int month, int day, int hour, int minute, int second) {
    return new DateTime(year, month, day, hour, minute, second, 0, TimeZone.getTimeZone("GMT"));
  }

  public static DateTime newDateTime() {
    return new DateTime();
  }

  public static DateTime newDateTime(long timestamp) {
    return new DateTime(timestamp);
  }
}
