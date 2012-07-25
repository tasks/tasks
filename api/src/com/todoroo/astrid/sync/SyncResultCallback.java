/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

public interface SyncResultCallback {
    /**
     * Increment max sync progress
     * @param incrementBy
     */
    public void incrementMax(int incrementBy);

    /**
     * Increment current sync progress
     * @param incrementBy
     */
    public void incrementProgress(int incrementBy);

    /**
     * Provider started sync
     */
    public void started();

    /**
     * Provider finished sync
     */
    public void finished();
}