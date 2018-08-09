package org.tasks.jobs;

import android.support.annotation.NonNull;
import org.tasks.injection.InjectingWorker;

public abstract class RepeatingWorker extends InjectingWorker {

  @NonNull
  @Override
  public final Result doWork() {
    Result result = super.doWork();
    scheduleNext();
    return result;
  }

  protected abstract void scheduleNext();
}
