package org.tasks.jobs;

import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import org.tasks.caldav.CaldavSynchronizer;
import org.tasks.gtasks.GoogleTaskSynchronizer;

public class SyncJob extends Job {

  private final CaldavSynchronizer caldavSynchronizer;
  private final GoogleTaskSynchronizer googleTaskSynchronizer;

  SyncJob(CaldavSynchronizer caldavSynchronizer, GoogleTaskSynchronizer googleTaskSynchronizer) {
    this.caldavSynchronizer = caldavSynchronizer;
    this.googleTaskSynchronizer = googleTaskSynchronizer;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    caldavSynchronizer.sync();
    googleTaskSynchronizer.sync();
    return Result.SUCCESS;
  }
}
