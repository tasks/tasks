package org.tasks.jobs;

import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import org.tasks.LocalBroadcastManager;
import org.tasks.scheduling.RefreshScheduler;

public class RefreshJob extends Job {

  public static final String TAG = "job_refresh";

  private final RefreshScheduler refreshScheduler;
  private final LocalBroadcastManager localBroadcastManager;

  RefreshJob(RefreshScheduler refreshScheduler, LocalBroadcastManager localBroadcastManager) {
    this.refreshScheduler = refreshScheduler;
    this.localBroadcastManager = localBroadcastManager;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    localBroadcastManager.broadcastRefresh();
    refreshScheduler.scheduleNext();
    return Result.SUCCESS;
  }
}
