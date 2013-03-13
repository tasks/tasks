package com.todoroo.astrid.actfm.sync;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;

public class SyncDatabaseListener<MTYPE extends AbstractModel> implements ModelUpdateListener<MTYPE> {

    private final ModelType modelType;
    protected final ActFmSyncThread actFmSyncThread;
    public SyncDatabaseListener(ActFmSyncThread actFmSyncThread, ModelType modelType) {
        this.actFmSyncThread = actFmSyncThread;
        this.modelType = modelType;
    }

    @Override
    public void onModelUpdated(MTYPE model, boolean outstandingEntries) {
        if (outstandingEntries) {
            ChangesHappened<?, ?> ch = ChangesHappened.instantiateChangesHappened(model.getId(), modelType);
            enqueueMessage(model, ch);
        }
    }

    protected void enqueueMessage(MTYPE model, ClientToServerMessage<?> message) {
        actFmSyncThread.enqueueMessage(message, null);
    }

}
