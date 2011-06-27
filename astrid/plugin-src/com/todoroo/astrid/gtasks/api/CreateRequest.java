package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.v1.model.Task;
/**
 * Encapsulates a request to the api to create a task on the remote server
 * @author Sam Bosley
 *
 */
public class CreateRequest extends PushRequest {

    public CreateRequest(GtasksService service, String listId, Task toUpdate) {
        super(service, listId, toUpdate);
    }

    @Override
    public Task executePush() throws IOException {
        return service.createGtask(listId, toPush);
    }

}
