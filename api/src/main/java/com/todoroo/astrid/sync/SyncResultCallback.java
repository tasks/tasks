/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

public interface SyncResultCallback {
    /**
     * Provider started sync
     */
    public void started();

    /**
     * Provider finished sync
     */
    public void finished();
}
