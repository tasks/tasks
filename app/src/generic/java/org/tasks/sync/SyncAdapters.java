package org.tasks.sync;

import android.content.ContentResolver;

import com.todoroo.astrid.activity.TaskListFragment;

import org.tasks.caldav.CaldavAccountManager;

import javax.inject.Inject;

public class SyncAdapters {
    private CaldavAccountManager caldavAccountManager;

    @Inject
    public SyncAdapters(CaldavAccountManager caldavAccountManager) {
        this.caldavAccountManager = caldavAccountManager;
    }

    public boolean initiateManualSync() {
        return caldavAccountManager.initiateManualSync();
    }

    public void requestSynchronization() {
        caldavAccountManager.requestSynchronization();
    }

    public boolean isGoogleTaskSyncEnabled() {
        return false;
    }

    public void checkPlayServices(TaskListFragment taskListFragment) {

    }

    public boolean isMasterSyncEnabled() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    public boolean isSyncEnabled() {
        return caldavAccountManager.getAccounts().size() > 0;
    }
}
