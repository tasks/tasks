package com.todoroo.astrid.actfm.sync;

import java.util.List;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.data.RemoteModel;

public class SyncDatabaseListener<MTYPE extends RemoteModel> implements ModelUpdateListener<MTYPE> {

    private final List<Pair<Long, ModelType>> queue;
    private final Object monitor;
    private final ModelType modelType;

    public SyncDatabaseListener(List<Pair<Long, ModelType>> queue, Object syncMonitor, ModelType modelType) {
        this.queue = queue;
        this.monitor = syncMonitor;
        this.modelType = modelType;
    }

    @Override
    public void onModelUpdated(MTYPE model) {
        queue.add(Pair.create(model.getId(), modelType));
        synchronized(monitor) {
            monitor.notifyAll();
        }
    }

}
