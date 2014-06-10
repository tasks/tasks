/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.services.tasks.model.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Abstract class that encapsulates some push request to the server
 * @author Sam Bosley
 *
 */
public abstract class PushRequest {

    private static final Logger log = LoggerFactory.getLogger(PushRequest.class);

    protected String listId;
    protected Task toPush;
    protected GtasksInvoker service;

    public PushRequest(GtasksInvoker service, String listId, Task toPush) {
        this.service = service;
        this.listId = listId;
        this.toPush = toPush;
    }

    public Task push() throws IOException {
        try {
            return executePush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            recover();
            return executePush();
        }
    }

    protected abstract Task executePush() throws IOException;

    protected abstract void recover();
}
