package com.todoroo.astrid.actfm.sync;

import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.data.TaskListMetadata;

public class TaskListMetadataSyncDatabaseListener extends SyncDatabaseListener<TaskListMetadata> {

    private final ActFmSyncWaitingPool waitingPool;

    public TaskListMetadataSyncDatabaseListener(ActFmSyncThread actFmSyncThread, ActFmSyncWaitingPool waitingPool, ModelType modelType) {
        super(actFmSyncThread, modelType);
        this.waitingPool = waitingPool;
    }

    @Override
    protected void enqueueMessage(TaskListMetadata model, ClientToServerMessage<?> message) {
        if (model.getSetValues().containsKey(TaskListMetadata.TASK_IDS.name))
            waitingPool.enqueueMessage(message);
        else
            actFmSyncThread.enqueueMessage(message, ActFmSyncThread.DEFAULT_REFRESH_RUNNABLE);
    }

}
