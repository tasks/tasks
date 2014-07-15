/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.sync.SyncContainer;

abstract public class SyncMetadataService<TYPE extends SyncContainer> {

    protected final TaskDao taskDao;
    protected final MetadataDao metadataDao;

    // --- abstract methods

    /** @return metadata key identifying this sync provider's metadata */
    abstract public String getMetadataKey();

    // --- implementation

    public SyncMetadataService(TaskDao taskDao, MetadataDao metadataDao) {
        this.taskDao = taskDao;
        this.metadataDao = metadataDao;
    }

    /**
     * Clears metadata information. Used when user logs out of sync provider
     */
    public void clearMetadata() {
        metadataDao.deleteWhere(Metadata.KEY.eq(getMetadataKey()));
    }

    /**
     * Saves a task and its metadata
     */
    public void saveTaskAndMetadata(TYPE task) {
        task.prepareForSaving();
        taskDao.save(task.task);
        metadataDao.synchronizeMetadata(task.task.getId(), task.metadata, getMetadataKey());
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).where(
                MetadataCriteria.byTaskAndwithKey(taskId, getMetadataKey())));
        try {
            if(cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }
}
