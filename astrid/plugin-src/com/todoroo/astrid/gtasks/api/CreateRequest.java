package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.v1.model.Task;
/**
 * Encapsulates a request to the api to create a task on the remote server
 * @author Sam Bosley
 *
 */
public class CreateRequest extends PushRequest {

    private String parent;
    private String priorSiblingId;

    public CreateRequest(GtasksService service, String listId, Task toUpdate, String parent, String priorSiblingId) {
        super(service, listId, toUpdate);
        this.parent = parent;
        this.priorSiblingId  = priorSiblingId;
    }

    @Override
    public Task executePush() throws IOException {
        return service.createGtask(listId, toPush, parent, priorSiblingId);
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getPriorSiblingId() {
        return priorSiblingId;
    }

    public void setPriorSiblingId(String priorSiblingId) {
        this.priorSiblingId = priorSiblingId;
    }


}
