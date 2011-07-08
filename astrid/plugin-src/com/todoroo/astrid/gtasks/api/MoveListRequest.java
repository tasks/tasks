package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.services.tasks.v1.model.Task;
/**
 * Encapsulates a request to the api to move a task from one list to another
 * @author Sam Bosley
 *
 */
public class MoveListRequest extends PushRequest {

    private String idTaskToMove;
    private String dstList;
    private final String newParent;

    public MoveListRequest(GtasksService service, String idTask, String srcList, String dstList, String newParent) {
        super(service, srcList, new Task());
        this.idTaskToMove = idTask;
        this.dstList = dstList;
        this.newParent = newParent;
    }

    @Override
    public Task executePush() throws IOException {
        Task localSave = service.getGtask(super.getListId(), idTaskToMove);
        service.deleteGtask(super.getListId(), idTaskToMove);
        transferProperties(localSave);
        return service.createGtask(dstList, toPush);
    }

    private void transferProperties(Task local) {
        toPush.completed = local.completed;
        toPush.deleted = local.deleted;
        toPush.due = local.due;
        toPush.hidden = local.hidden;
        toPush.notes = local.notes;
        toPush.status = local.status;
        toPush.title = local.title;

        toPush.parent = newParent;
    }

    public String getIdTaskToMove() {
        return idTaskToMove;
    }

    public void setIdTaskToMove(String idTaskToMove) {
        this.idTaskToMove = idTaskToMove;
    }

    public String getDstList() {
        return dstList;
    }

    public void setDstList(String dstList) {
        this.dstList = dstList;
    }


}
