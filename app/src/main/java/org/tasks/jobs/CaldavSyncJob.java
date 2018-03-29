package org.tasks.jobs;

import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import org.tasks.caldav.CaldavSynchronizer;

public class CaldavSyncJob extends Job{

  private final CaldavSynchronizer caldavSynchronizer;

  CaldavSyncJob(CaldavSynchronizer caldavSynchronizer) {
    this.caldavSynchronizer = caldavSynchronizer;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    return caldavSynchronizer.sync() ? Result.SUCCESS : Result.FAILURE;
  }
}
