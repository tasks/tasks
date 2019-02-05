package org.tasks.injection;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import timber.log.Timber;

public abstract class InjectingWorker extends Worker {

  @Inject Tracker tracker;

  public InjectingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

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
      return Result.failure();
    }
  }

  protected abstract Result run();

  protected abstract void inject(JobComponent component);
}
