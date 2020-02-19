package org.tasks.analytics;

import javax.inject.Inject;
import timber.log.Timber;

public class Tracker {

  @Inject
  public Tracker() {}

  public void reportException(Throwable t) {
    Timber.e(t);
  }
}
