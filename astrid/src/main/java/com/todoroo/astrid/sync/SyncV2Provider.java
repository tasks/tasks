/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract public class SyncV2Provider {

    private static final Logger log = LoggerFactory.getLogger(SyncV2Provider.class);

    public class SyncExceptionHandler {
        public void handleException(String tag, Exception e) {
            getUtilities().setLastError(e.toString());
            log.error("{}: {}", tag, e.getMessage(), e);
        }
    }

    protected final SyncExceptionHandler handler = new SyncExceptionHandler();

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
     * @return sync utility instance
     */
    abstract protected SyncProviderUtilities getUtilities();

    protected void finishSync(SyncResultCallback callback) {
        SyncProviderUtilities utilities = getUtilities();
        utilities.recordSuccessfulSync();
        utilities.stopOngoing();
        if (callback != null) {
            callback.finished();
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
