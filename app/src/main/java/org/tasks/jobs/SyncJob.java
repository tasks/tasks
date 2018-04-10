package org.tasks.jobs;

import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import org.tasks.LocalBroadcastManager;
import org.tasks.caldav.CaldavSynchronizer;
import org.tasks.gtasks.GoogleTaskSynchronizer;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class SyncJob extends Job {

  private static final Object LOCK = new Object();

  private final CaldavSynchronizer caldavSynchronizer;
  private final GoogleTaskSynchronizer googleTaskSynchronizer;
  private final LocalBroadcastManager localBroadcastManager;
  private final Preferences preferences;

  SyncJob(
      CaldavSynchronizer caldavSynchronizer,
      GoogleTaskSynchronizer googleTaskSynchronizer,
      LocalBroadcastManager localBroadcastManager,
      Preferences preferences) {
    this.caldavSynchronizer = caldavSynchronizer;
    this.googleTaskSynchronizer = googleTaskSynchronizer;
    this.localBroadcastManager = localBroadcastManager;
    this.preferences = preferences;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    synchronized (LOCK) {
      if (preferences.isSyncOngoing()) {
        return Result.RESCHEDULE;
      }
    }

    preferences.setSyncOngoing(true);
    localBroadcastManager.broadcastRefresh();
    try {
      caldavSynchronizer.sync();
      googleTaskSynchronizer.sync();
    } catch (Exception e) {
      Timber.e(e);
    } finally {
      preferences.setSyncOngoing(false);
      localBroadcastManager.broadcastRefresh();
    }
    return Result.SUCCESS;
  }
}
