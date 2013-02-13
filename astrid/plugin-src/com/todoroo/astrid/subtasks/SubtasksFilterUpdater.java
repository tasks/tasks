package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.TaskListMetadata;

public class SubtasksFilterUpdater extends SubtasksUpdater<TaskListMetadata> {

    @Override
    protected String getSerializedTree(TaskListMetadata list, Filter filter) {
        if (list == null)
            return "[]"; //$NON-NLS-1$
        String order = list.getValue(TaskListMetadata.TASK_IDS);
        if (order == null || "null".equals(order)) //$NON-NLS-1$
            order = "[]"; //$NON-NLS-1$

        return order;
    }

    @Override
    protected void writeSerialization(TaskListMetadata list, String serialized, boolean shouldQueueSync) {
        if (list != null) {
            list.setValue(TaskListMetadata.TASK_IDS, serialized);
            taskListMetadataDao.saveExisting(list);
        }
    }

}
