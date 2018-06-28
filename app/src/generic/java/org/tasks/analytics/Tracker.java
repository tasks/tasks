package org.tasks.analytics;

import javax.inject.Inject;
import timber.log.Timber;

public class Tracker {

  @Inject
  public Tracker() {}

  public static void report(Exception e) {
    Timber.e(e);
  }

  public void reportException(Throwable t) {
    Timber.e(t);
  }

  public void reportEvent(Tracking.Events event) {}

  public void reportEvent(Tracking.Events event, String string) {}

  public void reportEvent(Tracking.Events setPreference, int resId, String s) {}

  public void reportEvent(Tracking.Events category, String action, String label) {}

  public void reportIabResult(int resultCode, String skus) {}
}
