/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;




/**
 * Convenience class for implementing sync result callbacks--if we need a sync
 * result callback that only implements a subset of the callback methods, it
 * can extend this empty implementation
 * @author Sam
 *
 */
public abstract class SyncResultCallbackAdapter implements SyncResultCallback {

    @Override
    public void incrementMax(int incrementBy) {
        // Empty implementation
    }

    @Override
    public void incrementProgress(int incrementBy) {
        // Empty implementation
    }

    @Override
    public void started() {
        // Empty implementation
    }

    @Override
    public void finished() {
        // Empty implementation
    }

}
