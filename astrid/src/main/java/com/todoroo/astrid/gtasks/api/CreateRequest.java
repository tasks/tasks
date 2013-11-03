/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.services.tasks.model.Task;

import java.io.IOException;
/**
 * Encapsulates a request to the api to create a task on the remote server
 * @author Sam Bosley
 *
 */
public class CreateRequest extends PushRequest {

    private String parent;
    private String priorSiblingId;

    public CreateRequest(GtasksInvoker service, String listId, Task toUpdate, String parent, String priorSiblingId) {
        super(service, listId, toUpdate);
        this.parent = parent;
        this.priorSiblingId  = priorSiblingId;
    }

    @Override
    public Task executePush() throws IOException {
        return service.createGtask(listId, toPush, parent, priorSiblingId);
    }

    @Override
    protected void recover() {
        parent = null;
        priorSiblingId = null;
    }
}
