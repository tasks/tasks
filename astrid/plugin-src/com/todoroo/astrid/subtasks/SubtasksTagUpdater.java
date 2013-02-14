package com.todoroo.astrid.subtasks;

import java.util.concurrent.atomic.AtomicBoolean;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.TaskListMetadata;

public class SubtasksTagUpdater extends SubtasksFilterUpdater {

    private final AtomicBoolean isBeingFiltered;

    public SubtasksTagUpdater(AtomicBoolean isBeingFiltered) {
        this.isBeingFiltered = isBeingFiltered;
    }

    @Override
    protected String getSerializedTree(TaskListMetadata list, Filter filter) {
        if (isBeingFiltered.get())
            return "[]"; //$NON-NLS-1$
        return super.getSerializedTree(list, filter);
    }

    @Override
    protected void writeSerialization(TaskListMetadata list, String serialized, boolean shouldQueueSync) {
        if (!isBeingFiltered.get()) {
            super.writeSerialization(list, serialized, shouldQueueSync);
        }
    }

    @Override
    public int getIndentForTask(String targetTaskId) {
        if (isBeingFiltered.get())
            return 0;
        return super.getIndentForTask(targetTaskId);
    }

}
