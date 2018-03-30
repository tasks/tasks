package org.tasks.sync;

import android.app.Activity;
import android.content.ContentResolver;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.jobs.JobManager;
import org.tasks.preferences.Preferences;

public class SyncAdapters {

  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final Preferences preferences;
  private final JobManager jobManager;

  @Inject
  public SyncAdapters(GtaskSyncAdapterHelper gtaskSyncAdapterHelper, Preferences preferences,
      JobManager jobManager) {
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.preferences = preferences;
    this.jobManager = jobManager;
  }

  public boolean syncNow() {
    if (isGoogleTaskSyncEnabled() || isCaldavSyncEnabled()) {
      jobManager.syncNow();
      return true;
    }
    return false;
  }

  public boolean isSyncEnabled() {
    return isGoogleTaskSyncEnabled() || isCaldavSyncEnabled();
  }

  public boolean isGoogleTaskSyncEnabled() {
    return gtaskSyncAdapterHelper.isEnabled();
  }

  public boolean isCaldavSyncEnabled() {
    return preferences.getBoolean(R.string.p_sync_caldav, false);
  }

  public void checkPlayServices(Activity activity) {
    gtaskSyncAdapterHelper.checkPlayServices(activity);
  }
}
