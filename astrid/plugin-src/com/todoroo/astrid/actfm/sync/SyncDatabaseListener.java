package com.todoroo.astrid.actfm.sync;

import java.util.List;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;

public class SyncDatabaseListener<MTYPE extends AbstractModel> implements ModelUpdateListener<MTYPE> {

    private final List<ClientToServerMessage<?>> queue;
    private final Object monitor;
    private final ModelType modelType;

    public SyncDatabaseListener(List<ClientToServerMessage<?>> queue, Object syncMonitor, ModelType modelType) {
        this.queue = queue;
        this.monitor = syncMonitor;
        this.modelType = modelType;
    }

    @Override
    public void onModelUpdated(MTYPE model) {
        ChangesHappened<?, ?> ch = ChangesHappened.instantiateChangesHappened(model.getId(), modelType);
        if (!queue.contains(ch)) {
            queue.add(ch);
            synchronized(monitor) {
                monitor.notifyAll();
            }
        }
    }

}
