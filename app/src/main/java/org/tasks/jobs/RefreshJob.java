package org.tasks.jobs;

import android.support.annotation.NonNull;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.InjectingJob;
import org.tasks.injection.JobComponent;
import org.tasks.scheduling.RefreshScheduler;

public class RefreshJob extends InjectingJob {

  public static final String TAG = "job_refresh";

  @Inject RefreshScheduler refreshScheduler;
  @Inject LocalBroadcastManager localBroadcastManager;

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    super.onRunJob(params);

    localBroadcastManager.broadcastRefresh();
    refreshScheduler.scheduleNext();
    return Result.SUCCESS;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
