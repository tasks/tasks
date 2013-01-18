package com.todoroo.astrid.subtasks;

import java.util.concurrent.atomic.AtomicBoolean;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.TagData;

public class SubtasksTagUpdater extends SubtasksUpdater<TagData> {

    private final AtomicBoolean isBeingFiltered;

    public SubtasksTagUpdater(AtomicBoolean isBeingFiltered) {
        this.isBeingFiltered = isBeingFiltered;
    }

    @Override
    protected String getSerializedTree(TagData list, Filter filter) {
        if (list == null || isBeingFiltered.get())
            return "[]"; //$NON-NLS-1$
        String order = list.getValue(TagData.TAG_ORDERING);
        if (order == null || "null".equals(order)) //$NON-NLS-1$
            order = "[]"; //$NON-NLS-1$

        return order;
    }

    @Override
    protected void writeSerialization(TagData list, String serialized, boolean shouldQueueSync) {
        if (!isBeingFiltered.get()) {
            list.setValue(TagData.TAG_ORDERING, serialized);
            tagDataService.save(list);
            if (shouldQueueSync)
                actFmSyncService.pushTagOrderingOnSave(list.getId());
        }
    }

    @Override
    public int getIndentForTask(long targetTaskId) {
        if (isBeingFiltered.get())
            return 0;
        return super.getIndentForTask(targetTaskId);
    }

}
