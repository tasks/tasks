/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.model.Task;

/**
 * Abstract class that encapsulates some push request to the server
 * @author Sam Bosley
 *
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
    public void setListId(String listId) {
        this.listId = listId;
    }

    public Task getToPush() {
        return toPush;
    }
    public void setToPush(Task toPush) {
        this.toPush = toPush;
    }

    public GtasksInvoker getService() {
        return service;
    }
    public void setService(GtasksInvoker service) {
        this.service = service;
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
