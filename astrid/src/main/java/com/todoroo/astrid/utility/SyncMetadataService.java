/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProviderUtilities;

import java.util.ArrayList;

abstract public class SyncMetadataService<TYPE extends SyncContainer> {

    protected final TaskDao taskDao;
    protected final MetadataDao metadataDao;

    // --- abstract methods

    /** @return metadata key identifying this sync provider's metadata */
    abstract public String getMetadataKey();

    /** @return sync provider utilities */
    abstract public SyncProviderUtilities getUtilities();

    /** @return criterion for matching all metadata keys that your provider synchronizes */
    abstract public Criterion getMetadataCriteria();

    /** @return criterion for matching metadata that indicate remote task exists */
    abstract public Criterion getMetadataWithRemoteId();

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
     * Gets cursor across all task metadata for joining
     *
     * @return cursor
     */
    private TodorooCursor<Metadata> getRemoteTaskMetadata() {
        return metadataDao.query(Query.select(Metadata.TASK).where(
                Criterion.and(MetadataCriteria.withKey(getMetadataKey()),
                getMetadataWithRemoteId())).orderBy(Order.asc(Metadata.TASK)));
    }

    /**
     * Gets tasks that were created since last sync
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>... properties) {
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.ID).where(
                Criterion.and(TaskCriteria.isActive(), TaskCriteria.ownedByMe())).orderBy(Order.asc(Task.ID)));

        return joinWithMetadata(tasks, false, properties);
    }

    /**
     * Gets tasks that were modified since last sync
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>... properties) {
        TodorooCursor<Task> tasks;
        long lastSyncDate = getUtilities().getLastSyncDate();
        if(lastSyncDate == 0) {
            tasks = taskDao.query(Query.select(Task.ID).where(Criterion.none));
        } else {
            tasks = taskDao.query(Query.select(Task.ID).where(Criterion.and(TaskCriteria.ownedByMe(), Task.MODIFICATION_DATE.gt(lastSyncDate)))
                    .orderBy(Order.asc(Task.ID)));
        }
        tasks = filterLocallyUpdated(tasks, lastSyncDate);

        return joinWithMetadata(tasks, true, properties);
    }

    protected TodorooCursor<Task> filterLocallyUpdated(TodorooCursor<Task> tasks, long lastSyncDate) {
        // override hook
        return tasks;
    }

    private TodorooCursor<Task> joinWithMetadata(TodorooCursor<Task> tasks,
            boolean both, Property<?>... properties) {
        try {
            TodorooCursor<Metadata> metadata = getRemoteTaskMetadata();
            try {
                ArrayList<Long> matchingRows = new ArrayList<>();
                joinRows(tasks, metadata, matchingRows, both);
                return taskDao.query(Query.select(properties).where(Task.ID.in(matchingRows)));
            } finally {
                metadata.close();
            }
        } finally {
            tasks.close();
        }
    }

    /**
     * Join rows from two cursors on the first column, assuming its an id column
     * @param both - if false, returns rows no right row exists, if true,
     *        returns rows where both exist
     */
    private static void joinRows(TodorooCursor<?> left,
            TodorooCursor<?> right, ArrayList<Long> matchingRows,
            boolean both) {

        left.moveToPosition(-1);
        right.moveToFirst();

        while(true) {
            left.moveToNext();
            if(left.isAfterLast()) {
                break;
            }
            long leftValue = left.getLong(0);

            // advance right until it is equal or bigger
            while(!right.isAfterLast() && right.getLong(0) < leftValue) {
                right.moveToNext();
            }

            if(right.isAfterLast()) {
                if(!both) {
                    matchingRows.add(leftValue);
                }
                continue;
            }

            if((right.getLong(0) == leftValue) == both) {
                matchingRows.add(leftValue);
            }
        }
    }

    /**
     * Saves a task and its metadata
     */
    public void saveTaskAndMetadata(TYPE task) {
        task.prepareForSaving();
        taskDao.save(task.task);
        metadataDao.synchronizeMetadata(task.task.getId(), task.metadata,
                getMetadataCriteria());
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
