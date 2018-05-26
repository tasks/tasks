package org.tasks;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import org.tasks.time.DateTime;
import org.tasks.time.DateTimeUtils;

public class Freeze {

  public static Freeze freezeClock() {
    return freezeAt(currentTimeMillis());
  }

  public static Freeze freezeAt(DateTime dateTime) {
    return freezeAt(dateTime.getMillis());
  }

  public static Freeze freezeAt(long millis) {
    DateTimeUtils.setCurrentMillisFixed(millis);
    return new Freeze();
  }

  public static void thaw() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @SuppressWarnings("UnusedParameters")
  public void thawAfter(Snippet snippet) {
    thaw();
  }

  public void thawAfter(Runnable run) {
    try {
      run.run();
    } finally {
      thaw();
    }
  }
}
