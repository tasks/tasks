/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.model.Task;

/**
 * Encapsulates a request to the api to update a task on the remote server
 * @author Sam Bosley
 *
 */
public class UpdateRequest extends PushRequest {

    public UpdateRequest(GtasksInvoker service, String listId, Task toUpdate) {
        super(service, listId, toUpdate);
    }

    @Override
    public Task executePush() throws IOException {
        return service.updateGtask(listId, toPush);
    }

    @Override
    protected void recover() {
        //Figure out a good way to recover!
    }

}
