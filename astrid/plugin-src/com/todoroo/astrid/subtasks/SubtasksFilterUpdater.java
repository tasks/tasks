package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.Filter;

public class SubtasksFilterUpdater extends SubtasksUpdater<String> {

    @Override
    protected String getSerializedTree(String list, Filter filter) {
        String order = Preferences.getStringValue(list);
        if (order == null || "null".equals(order)) //$NON-NLS-1$
            order = "[]"; //$NON-NLS-1$

        return order;
    }

    @Override
    protected void writeSerialization(String list, String serialized, boolean shouldQueueSync) {
        Preferences.setString(list, serialized);
        if (shouldQueueSync)
            actFmSyncService.pushFilterOrderingOnSave(list);
    }

}
