package com.todoroo.astrid.actfm.sync.messages;

import java.util.HashSet;
import java.util.Set;

import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.TaskListMetadataOutstandingDao;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskListMetadataOutstanding;

public class TaskListMetadataChangesHappened extends ChangesHappened<TaskListMetadata, TaskListMetadataOutstanding> {

    public TaskListMetadataChangesHappened(long id, Class<TaskListMetadata> modelClass, TaskListMetadataDao modelDao, TaskListMetadataOutstandingDao outstandingDao) {
        super(id, modelClass, modelDao, outstandingDao);
    }

    @Override
    protected void populateChanges() {
        super.populateChanges();

        // Collapses/removes redundant task list orders from the list--only send the most recent ordering
        Set<Long> removedChanges = new HashSet<Long>();
        boolean foundOrderChange = false;
        for (int i = changes.size() - 1; i >= 0; i--) {
            TaskListMetadataOutstanding oe = changes.get(i);
            if (TaskListMetadata.TASK_IDS.name.equals(oe.getValue(TaskListMetadataOutstanding.COLUMN_STRING))) {
                if (foundOrderChange) {
                    changes.remove(i);
                    removedChanges.add(oe.getId());
                } else {
                    foundOrderChange = true;
                }
            }
        }

        outstandingDao.deleteWhere(TaskListMetadataOutstanding.ID.in(removedChanges.toArray(new Long[removedChanges.size()])));
    }

}
