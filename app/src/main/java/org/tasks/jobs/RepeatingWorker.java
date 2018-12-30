package org.tasks.jobs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import org.tasks.injection.InjectingWorker;

public abstract class RepeatingWorker extends InjectingWorker {

  public RepeatingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public final Result doWork() {
    Result result = super.doWork();
    scheduleNext();
    return result;
  }

  protected abstract void scheduleNext();
}
