package org.tasks;

import android.util.Log;
import javax.inject.Inject;
import timber.log.Timber;

public class BuildSetup {

  @Inject
  public BuildSetup() {}

  public boolean setup() {
    Timber.plant(new ErrorReportingTree());
    return true;
  }

  private static class ErrorReportingTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
      if (priority < Log.WARN) {
        return;
      }
      if (priority == Log.ERROR) {
        if (t == null) {
          Log.e(tag, message);
        } else {
          Log.e(tag, message, t);
        }
      } else if (priority == Log.WARN) {
        if (t == null) {
          Log.w(tag, message);
        } else {
          Log.w(tag, message, t);
        }
      }
    }
  }
}
