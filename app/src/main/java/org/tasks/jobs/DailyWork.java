package org.tasks.jobs;

import android.support.annotation.NonNull;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingWorker;

public abstract class DailyWork extends InjectingWorker {

  @Inject Tracker tracker;

  @NonNull
  @Override
  public final Result doWork() {
    super.doWork();
    Result result;
    try {
      result = doDailyWork();
    } catch (Exception e) {
      tracker.reportException(e);
      result = Result.FAILURE;
    }
    scheduleNext();

    return result;
  }

  protected abstract Result doDailyWork();

  protected abstract void scheduleNext();
}
