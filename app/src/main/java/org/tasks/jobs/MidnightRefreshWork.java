package org.tasks.jobs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.JobComponent;

public class MidnightRefreshWork extends RepeatingWorker {

  @Inject WorkManager workManager;
  @Inject LocalBroadcastManager localBroadcastManager;

  public MidnightRefreshWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @Override
  protected Result run() {
    localBroadcastManager.broadcastRefresh();
    return Result.success();
  }

  @Override
  protected void scheduleNext() {
    workManager.scheduleMidnightRefresh();
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
