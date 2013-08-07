/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.services.tasks.model.Task;

import java.io.IOException;

/**
 * Encapsulates a request to the api to change the ordering on the given task
 *
 * @author Sam Bosley
 */
public class MoveRequest extends PushRequest {

    private String taskId;
    private String parentId;
    private String priorSiblingId;

    public MoveRequest(GtasksInvoker service, String taskId, String destinationList, String parentId, String priorSiblingId) {
        super(service, destinationList, null);
        this.taskId = taskId;
        this.parentId = parentId;
        this.priorSiblingId = priorSiblingId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public Task executePush() throws IOException {
        return service.moveGtask(listId, taskId, parentId, priorSiblingId);
    }

    @Override
    protected void recover() {
        parentId = null;
        priorSiblingId = null;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getPriorSiblingId() {
        return priorSiblingId;
    }

    public void setPriorSiblingId(String priorSiblingId) {
        this.priorSiblingId = priorSiblingId;
    }
}
