package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;

import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.TaskListMetadataOutstandingDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskListMetadataOutstanding;

public class TaskListMetadataChangesHappened extends ChangesHappened<TaskListMetadata, TaskListMetadataOutstanding> {

    private final TaskListMetadataDao dao;

    public TaskListMetadataChangesHappened(long id, Class<TaskListMetadata> modelClass, TaskListMetadataDao modelDao, TaskListMetadataOutstandingDao outstandingDao) {
        super(id, modelClass, modelDao, outstandingDao);
        this.dao = modelDao;
    }

    @Override
    protected void populateChanges() {
        super.populateChanges();

        // Collapses/removes redundant task list orders from the list--only send the most recent ordering
        Set<Long> removedChanges = new HashSet<Long>();
        boolean foundOrderChange = false;
        boolean foundTagOrFilterId = false;
        for (int i = changes.size() - 1; i >= 0; i--) {
            TaskListMetadataOutstanding oe = changes.get(i);
            String column = oe.getValue(TaskListMetadataOutstanding.COLUMN_STRING);
            if (TaskListMetadata.TASK_IDS.name.equals(column)) {
                if (foundOrderChange || TaskListMetadata.taskIdsIsEmpty(oe.getValue(TaskListMetadataOutstanding.VALUE_STRING))) {
                    changes.remove(i);
                    removedChanges.add(oe.getId());
                } else {
                    foundOrderChange = true;
                }
            } else if (TaskListMetadata.FILTER.name.equals(column) || TaskListMetadata.TAG_UUID.name.equals(column)) {
                if (RemoteModel.isUuidEmpty(oe.getValue(TaskListMetadataOutstanding.VALUE_STRING))) {
                    changes.remove(i);
                    removedChanges.add(oe.getId());
                } else {
                    foundTagOrFilterId = true;
                }
            }
        }

        if (pushedAt == 0 && !foundTagOrFilterId) { // Try to validate message
            TaskListMetadata tlm = dao.fetch(id, TaskListMetadata.FILTER, TaskListMetadata.TAG_UUID);
            if (tlm != null) {
                String filterId = tlm.getValue(TaskListMetadata.FILTER);
                String tagUuid = tlm.getValue(TaskListMetadata.TAG_UUID);

                TaskListMetadataOutstanding tlmo = new TaskListMetadataOutstanding();
                boolean validChange = false;

                if (!TextUtils.isEmpty(filterId)) {
                    validChange = true;
                    tlmo.setValue(TaskListMetadataOutstanding.ENTITY_ID_PROPERTY, id);
                    tlmo.setValue(TaskListMetadataOutstanding.COLUMN_STRING, TaskListMetadata.FILTER.name);
                    tlmo.setValue(TaskListMetadataOutstanding.VALUE_STRING, filterId);
                    tlmo.setValue(TaskListMetadataOutstanding.CREATED_AT, 0L);
                } else if (!RemoteModel.isUuidEmpty(tagUuid)) {
                    validChange = true;
                    tlmo.setValue(TaskListMetadataOutstanding.ENTITY_ID_PROPERTY, id);
                    tlmo.setValue(TaskListMetadataOutstanding.COLUMN_STRING, TaskListMetadata.TAG_UUID.name);
                    tlmo.setValue(TaskListMetadataOutstanding.VALUE_STRING, tagUuid);
                    tlmo.setValue(TaskListMetadataOutstanding.CREATED_AT, 0L);
                }

                if (validChange) {
                    changes.add(tlmo);
                }
            }
        }

        outstandingDao.deleteWhere(TaskListMetadataOutstanding.ID.in(removedChanges.toArray(new Long[removedChanges.size()])));
    }

}
