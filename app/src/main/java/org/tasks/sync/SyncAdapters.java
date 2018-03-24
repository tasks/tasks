package org.tasks.sync;

import android.app.Activity;
import android.content.ContentResolver;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.caldav.CaldavAccountManager;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;

public class SyncAdapters {

  private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  private final Preferences preferences;
  private final CaldavAccountManager caldavAccountManager;
  private final PermissionChecker permissionChecker;

  @Inject
  public SyncAdapters(GtaskSyncAdapterHelper gtaskSyncAdapterHelper, Preferences preferences,
      CaldavAccountManager caldavAccountManager, PermissionChecker permissionChecker) {
    this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
    this.preferences = preferences;
    this.caldavAccountManager = caldavAccountManager;
    this.permissionChecker = permissionChecker;
  }

  public void requestSynchronization() {
    gtaskSyncAdapterHelper.requestSynchronization();
    caldavAccountManager.requestSynchronization();
  }

  public boolean initiateManualSync() {
    return gtaskSyncAdapterHelper.initiateManualSync() | caldavAccountManager.initiateManualSync();
  }

  public boolean isMasterSyncEnabled() {
    return ContentResolver.getMasterSyncAutomatically();
  }

  public boolean isSyncEnabled() {
    return isGoogleTaskSyncEnabled() || isCaldavSyncEnabled();
  }

  public boolean isGoogleTaskSyncEnabled() {
    return gtaskSyncAdapterHelper.isEnabled();
  }

  public boolean isCaldavSyncEnabled() {
    return preferences.getBoolean(R.string.p_sync_caldav, false) && permissionChecker
        .canAccessAccounts();
  }

  public void checkPlayServices(Activity activity) {
    gtaskSyncAdapterHelper.checkPlayServices(activity);
  }
}
