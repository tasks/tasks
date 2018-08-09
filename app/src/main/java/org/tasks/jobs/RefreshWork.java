package org.tasks.jobs;

import android.support.annotation.NonNull;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.scheduling.RefreshScheduler;

public class RefreshWork extends RepeatingWorker {

  @Inject RefreshScheduler refreshScheduler;
  @Inject LocalBroadcastManager localBroadcastManager;

  @NonNull
  @Override
  public Result run() {
      localBroadcastManager.broadcastRefresh();
      return Result.SUCCESS;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }

  @Override
  protected void scheduleNext() {
    refreshScheduler.scheduleNext();
  }
}
