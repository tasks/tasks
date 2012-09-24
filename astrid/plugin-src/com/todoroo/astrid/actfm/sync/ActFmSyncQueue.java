package com.todoroo.astrid.actfm.sync;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.todoroo.astrid.data.RemoteModel;

public class ActFmSyncQueue {

    private static final ActFmSyncQueue INSTANCE = new ActFmSyncQueue();

    public static class SyncQueueEntry {
        public Class<? extends RemoteModel> modelType;
        public Long id;

        public SyncQueueEntry(Class<? extends RemoteModel> modelType, Long id) {
            this.modelType = modelType;
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((modelType == null) ? 0 : modelType.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SyncQueueEntry other = (SyncQueueEntry) obj;
            if (modelType == null) {
                if (other.modelType != null)
                    return false;
            } else if (!modelType.equals(other.modelType))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

    }

    private final List<SyncQueueEntry> queue;
    private final HashSet<SyncQueueEntry> elements;

    private ActFmSyncQueue() {
        queue = new LinkedList<SyncQueueEntry>();
        elements = new HashSet<SyncQueueEntry>();
    }

    public synchronized void enqueue(SyncQueueEntry entry) {
        if (!elements.contains(entry)) {
            queue.add(entry);
            elements.add(entry);
        }
    }

    public synchronized SyncQueueEntry dequeue() {
        if (queue.isEmpty())
            return null;
        SyncQueueEntry entry = queue.remove(0);
        elements.remove(entry);
        return entry;
    }

    public static ActFmSyncQueue getInstance() {
        return INSTANCE;
    }

}
