package org.tasks.injection;

import android.support.annotation.NonNull;
import androidx.work.Worker;

public abstract class InjectingWorker extends Worker {

  @NonNull
  @Override
  public Result doWork() {
    JobComponent component =
        ((InjectingApplication) getApplicationContext()).getComponent().plus(new WorkModule());
    inject(component);
    return Result.SUCCESS;
  }

  protected abstract void inject(JobComponent component);
}
