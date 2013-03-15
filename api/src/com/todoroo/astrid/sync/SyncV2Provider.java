/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import java.io.IOException;

import android.app.Activity;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;


abstract public class SyncV2Provider {

    public class SyncExceptionHandler {
        public void handleException(String tag, Exception e, String type) {
            //TODO: When Crittercism supports it, report error to them
            getUtilities().setLastError(e.toString(), type);

            // occurs when application was closed
            if(e instanceof IllegalStateException) {
                exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$
            }

            // occurs when network error
            else if(e instanceof IOException) {
                exceptionService.reportError(tag + "-io", e); //$NON-NLS-1$
            }

            // unhandled error
            else {
                exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
            }
        }
    }

    @Autowired
    protected ExceptionService exceptionService;

    protected final SyncExceptionHandler handler;

    public SyncV2Provider() {
        DependencyInjectionService.getInstance().inject(this);
        handler = new SyncExceptionHandler();
    }

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
    abstract public void signOut(Activity activity);

    /**
     * @return sync utility instance
     */
    abstract protected SyncProviderUtilities getUtilities();

    protected void finishSync(SyncResultCallback callback) {
        SyncProviderUtilities utilities = getUtilities();
        utilities.recordSuccessfulSync();
        utilities.reportLastError();
        utilities.stopOngoing();
        if (callback != null)
            callback.finished();
    }

    @Override
    public String toString() {
        return getName();
    }
}
