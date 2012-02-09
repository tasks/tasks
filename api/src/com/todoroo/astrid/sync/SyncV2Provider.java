package com.todoroo.astrid.sync;


abstract public class SyncV2Provider {
    /**
     * @return sync provider name (displayed in sync menu)
     */
    abstract public String getName();

    /**
     * @return true if this provider is logged in
     */
    abstract public boolean isActive();

    /**
     * Synchronize all of user's active tasks
     * @param manual whether manually triggered
     * @param callback callback object
     */
    abstract public void synchronizeActiveTasks(boolean manual, SyncResultCallback callback);

    /**
     * Synchronize a single list
     * @param list object representing list (TaskListActivity-dependent)
     * @param manual whether was manually triggered
     * @param callback callback object
     */
    abstract public void synchronizeList(Object list, boolean manual, SyncResultCallback callback);

    /**
     * Sign out of service, deleting all synchronization metadata
     */
    abstract public void signOut();

    @Override
    public String toString() {
        return getName();
    }
}
