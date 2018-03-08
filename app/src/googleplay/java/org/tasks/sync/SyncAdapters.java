package org.tasks.sync;

import android.content.ContentResolver;

import com.todoroo.astrid.activity.TaskListFragment;

import org.tasks.caldav.CaldavAccountManager;
import org.tasks.gtasks.GtaskSyncAdapterHelper;

import javax.inject.Inject;

public class SyncAdapters {

    private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
    private final CaldavAccountManager caldavAccountManager;

    @Inject
    public SyncAdapters(GtaskSyncAdapterHelper gtaskSyncAdapterHelper, CaldavAccountManager caldavAccountManager) {
        this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
        this.caldavAccountManager = caldavAccountManager;
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
        return gtaskSyncAdapterHelper.isSyncEnabled();
    }

    public boolean isCaldavSyncEnabled() {
        return caldavAccountManager.getAccounts().size() > 0;
    }

    public void checkPlayServices(TaskListFragment taskListFragment) {
        gtaskSyncAdapterHelper.checkPlayServices(taskListFragment);
    }
}
