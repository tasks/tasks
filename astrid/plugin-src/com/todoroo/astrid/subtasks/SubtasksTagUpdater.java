package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.TagData;

public class SubtasksTagUpdater extends SubtasksUpdater<TagData> {

    @Override
    protected String getSerializedTree(TagData list, Filter filter) {
        String order = list.getValue(TagData.TAG_ORDERING);
        if (order == null || "null".equals(order)) //$NON-NLS-1$
            order = "[]"; //$NON-NLS-1$

        return order;
    }

    @Override
    protected void writeSerialization(TagData list, String serialized) {
        list.setValue(TagData.TAG_ORDERING, serialized);
        tagDataService.save(list);
        actFmSyncService.pushTagOrderingOnSave(list.getId());
    }

}
