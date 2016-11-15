package org.tasks.gtasks;

import com.todoroo.astrid.activity.TaskListFragment;

import javax.inject.Inject;

public class SyncAdapterHelper {
    @Inject
    public SyncAdapterHelper() {

    }

    public boolean shouldShowBackgroundSyncWarning() {
        return false;
    }

    public void checkPlayServices(TaskListFragment taskListFragment) {

    }

    public boolean initiateManualSync() {
        return false;
    }

    public boolean isEnabled() {
        return false;
    }

    public void requestSynchronization() {

    }
}
