package org.tasks.time;

import android.annotation.SuppressLint;
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

  public static String printTimestamp(long timestamp) {
    return BuildConfig.DEBUG ? new Date(timestamp).toString() : Long.toString(timestamp);
  }

  @SuppressLint("DefaultLocale")
  public static String printDuration(long millis) {
    if (BuildConfig.DEBUG) {
      long seconds = millis / 1000;
      return String.format(
          "%dh %dm %ds", seconds / 3600L, (int) (seconds % 3600L / 60L), (int) (seconds % 60L));
    } else {
      return Long.toString(millis);
    }
  }
}
