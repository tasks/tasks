package org.tasks.time;

import static org.tasks.date.DateTimeUtils.newDateTime;

import java.util.Date;
import org.tasks.BuildConfig;

public class DateTimeUtils {

  private static final SystemMillisProvider SYSTEM_MILLIS_PROVIDER = new SystemMillisProvider();
  private static volatile MillisProvider MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER;

  public static long currentTimeMillis() {
    return MILLIS_PROVIDER.getMillis();
  }

  public static void setCurrentMillisFixed(long millis) {
    MILLIS_PROVIDER = new FixedMillisProvider(millis);
  }

  public static void setCurrentMillisSystem() {
    MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER;
  }

  public static long nextMidnight() {
    return nextMidnight(currentTimeMillis());
  }

  private static long nextMidnight(long timestamp) {
    return newDateTime(timestamp).startOfDay().plusDays(1).getMillis();
  }

  public static String printTimestamp(long timestamp) {
    return BuildConfig.DEBUG ? new Date(timestamp).toString() : Long.toString(timestamp);
  }
}
