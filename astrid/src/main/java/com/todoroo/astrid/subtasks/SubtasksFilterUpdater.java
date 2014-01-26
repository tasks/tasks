package com.todoroo.astrid.subtasks;

import android.text.TextUtils;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TaskListMetadata;

public class SubtasksFilterUpdater extends SubtasksUpdater<TaskListMetadata> {

    public SubtasksFilterUpdater() {
    }

    @Override
    protected String getSerializedTree(TaskListMetadata list, Filter filter) {
        if (list == null) {
            return "[]"; //$NON-NLS-1$
        }
        String order = list.getTaskIDs();
        if (TextUtils.isEmpty(order) || "null".equals(order)) //$NON-NLS-1$
        {
            order = "[]"; //$NON-NLS-1$
        }

        return order;
    }

    @Override
    protected void writeSerialization(TaskListMetadata list, String serialized, boolean shouldQueueSync) {
        if (list != null) {
            list.setValue(TaskListMetadata.TASK_IDS, serialized);
            if (!shouldQueueSync) {
                list.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            }
            taskListMetadataDao.saveExisting(list);
        }
    }
}
