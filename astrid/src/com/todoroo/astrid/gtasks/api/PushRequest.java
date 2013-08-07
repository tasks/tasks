/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.services.tasks.model.Task;

import java.io.IOException;

/**
 * Abstract class that encapsulates some push request to the server
 *
 * @author Sam Bosley
 */
public abstract class PushRequest {
    protected String listId;
    protected Task toPush;
    protected GtasksInvoker service;

    public PushRequest(GtasksInvoker service, String listId, Task toPush) {
        this.service = service;
        this.listId = listId;
        this.toPush = toPush;
    }

    public String getListId() {
        return listId;
    }

    public GtasksInvoker getService() {
        return service;
    }

    public Task push() throws IOException {
        try {
            return executePush();
        } catch (IOException e) {
            recover();
            return executePush();
        }
    }

    protected abstract Task executePush() throws IOException;

    protected abstract void recover();
}
