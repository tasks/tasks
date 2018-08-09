package org.tasks.jobs;

import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.JobComponent;

public class MidnightRefreshWork extends RepeatingWorker {

  @Inject WorkManager workManager;
  @Inject LocalBroadcastManager localBroadcastManager;

  @Override
  protected Result run() {
    localBroadcastManager.broadcastRefresh();
    return Result.SUCCESS;
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
