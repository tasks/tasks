package org.tasks.notifications;

import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

class Throttle {
  private final long[] throttle;
  private final Sleeper sleeper;
  private int oldest = 0;

  Throttle(int ratePerSecond) {
    this(ratePerSecond, Throttle::sleep);
  }

  Throttle(int ratePerSecond, Sleeper sleeper) {
    this.sleeper = sleeper;
    throttle = new long[ratePerSecond];
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ignored) {
    }
  }

  synchronized void run(Runnable runnable) {
    assertNotMainThread();
    long sleep = throttle[oldest] - (currentTimeMillis() - 1000);
    if (sleep > 0) {
      sleeper.sleep(sleep);
    }
    runnable.run();
    throttle[oldest] = currentTimeMillis();
    oldest = (oldest + 1) % throttle.length;
  }

  public interface Sleeper {
    void sleep(long millis);
  }
}
