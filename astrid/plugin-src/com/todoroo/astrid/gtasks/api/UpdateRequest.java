package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.v1.model.Task;

/**
 * Encapsulates a request to the api to update a task on the remote server
 * @author Sam Bosley
 *
 */
public class UpdateRequest extends PushRequest {

    public UpdateRequest(GtasksService service, String listId, Task toUpdate) {
        super(service, listId, toUpdate);
    }

    @Override
    public Task executePush() throws IOException {
        return service.updateGtask(listId, toPush);
    }

}
