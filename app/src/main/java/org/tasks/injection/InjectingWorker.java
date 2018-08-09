package org.tasks.injection;

import android.support.annotation.NonNull;
import androidx.work.Worker;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import timber.log.Timber;

public abstract class InjectingWorker extends Worker {

  @Inject Tracker tracker;

  @NonNull
  @Override
  public Result doWork() {
    Timber.d("%s.doWork()", getClass().getSimpleName());
    JobComponent component =
        ((InjectingApplication) getApplicationContext()).getComponent().plus(new WorkModule());
    inject(component);
    try {
      return run();
    } catch (Exception e) {
      tracker.reportException(e);
      return Result.FAILURE;
    }
  }

  protected abstract Result run();

  protected abstract void inject(JobComponent component);
}
