package org.tasks.sync;

import android.app.Activity;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.jobs.JobManager;

public class SyncAdapters {

  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final JobManager jobManager;
  private final CaldavDao caldavDao;

  @Inject
  public SyncAdapters(
      GtaskSyncAdapterHelper gtaskSyncAdapterHelper, JobManager jobManager, CaldavDao caldavDao) {
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.jobManager = jobManager;
    this.caldavDao = caldavDao;
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
    return caldavDao.getAccounts().size() > 0;
  }

  public void checkPlayServices(Activity activity) {
    gtaskSyncAdapterHelper.checkPlayServices(activity);
  }
}
