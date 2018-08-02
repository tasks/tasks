package org.tasks.sync;

import android.app.Activity;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.jobs.WorkManager;

public class SyncAdapters {

  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final WorkManager workManager;
  private final CaldavDao caldavDao;

  @Inject
  public SyncAdapters(
      GtaskSyncAdapterHelper gtaskSyncAdapterHelper, WorkManager workManager, CaldavDao caldavDao) {
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.workManager = workManager;
    this.caldavDao = caldavDao;
  }

  public boolean syncNow() {
    if (isGoogleTaskSyncEnabled() || isCaldavSyncEnabled()) {
      workManager.syncNow();
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
