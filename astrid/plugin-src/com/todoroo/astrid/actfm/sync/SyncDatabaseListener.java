package com.todoroo.astrid.actfm.sync;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;

public class SyncDatabaseListener<MTYPE extends AbstractModel> implements ModelUpdateListener<MTYPE> {

    private final ModelType modelType;
    private final ActFmSyncThread actFmSyncThread;
    public SyncDatabaseListener(ActFmSyncThread actFmSyncThread, ModelType modelType) {
        this.actFmSyncThread = actFmSyncThread;
        this.modelType = modelType;
    }

    @Override
    public void onModelUpdated(MTYPE model, boolean outstandingEntries) {
        if (outstandingEntries) {
            ChangesHappened<?, ?> ch = ChangesHappened.instantiateChangesHappened(model.getId(), modelType);
            actFmSyncThread.enqueueMessage(ch, null);
        }
    }

}
