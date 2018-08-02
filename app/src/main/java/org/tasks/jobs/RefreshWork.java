package org.tasks.jobs;

import android.support.annotation.NonNull;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.scheduling.RefreshScheduler;

public class RefreshWork extends InjectingWorker {

  @Inject RefreshScheduler refreshScheduler;
  @Inject LocalBroadcastManager localBroadcastManager;

  @NonNull
  @Override
  public Result doWork() {
    super.doWork();
      localBroadcastManager.broadcastRefresh();
      refreshScheduler.scheduleNext();
      return Result.SUCCESS;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
