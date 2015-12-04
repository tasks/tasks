/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.gtasks.OrderedMetadataListUpdater.OrderedListIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for working with GTasks metadata
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public final class GtasksMetadataService {

    private final TaskDao taskDao;
    private final MetadataDao metadataDao;

    @Inject
    public GtasksMetadataService(TaskDao taskDao, MetadataDao metadataDao) {
        this.taskDao = taskDao;
        this.metadataDao = metadataDao;
    }

    /**
     * Clears metadata information. Used when user logs out of sync provider
     */
    public void clearMetadata() {
        metadataDao.deleteWhere(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY));
    }

    /**
     * Saves a task and its metadata
     */
    public void saveTaskAndMetadata(GtasksTaskContainer task) {
        task.prepareForSaving();
        taskDao.save(task.task);
        synchronizeMetadata(task.task.getId(), task.metadata, GtasksMetadata.METADATA_KEY);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        return metadataDao.getFirst(Query.select(Metadata.PROPERTIES).where(
                MetadataCriteria.byTaskAndwithKey(taskId, GtasksMetadata.METADATA_KEY)));
    }

    /**
     * Synchronize metadata for given task id. Deletes rows in database that
     * are not identical to those in the metadata list, creates rows that
     * have no match.
     *
     * @param taskId id of task to perform synchronization on
     * @param metadata list of new metadata items to save
     * @param metadataKey metadata key
     */
    private void synchronizeMetadata(long taskId, ArrayList<Metadata> metadata, String metadataKey) {
        final Set<ContentValues> newMetadataValues = new HashSet<>();
        for(Metadata metadatum : metadata) {
            metadatum.setTask(taskId);
            metadatum.clearValue(Metadata.ID);
            newMetadataValues.add(metadatum.getMergedValues());
        }

        metadataDao.byTaskAndKey(taskId, metadataKey, new Callback<Metadata>() {
            @Override
            public void apply(Metadata item) {
                long id = item.getId();

                // clear item id when matching with incoming values
                item.clearValue(Metadata.ID);
                ContentValues itemMergedValues = item.getMergedValues();
                if(newMetadataValues.contains(itemMergedValues)) {
                    newMetadataValues.remove(itemMergedValues);
                } else {
                    // not matched. cut it
                    metadataDao.delete(id);
                }
            }
        });

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            Metadata item = new Metadata();
            item.mergeWith(values);
            metadataDao.persist(item);
        }
    }

    public synchronized void findLocalMatch(GtasksTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID) {
            return;
        }
        Metadata metadata = getMetadataByGtaskId(remoteTask.gtaskMetadata.getValue(GtasksMetadata.ID));
        if (metadata != null) {
            remoteTask.task.setId(metadata.getValue(Metadata.TASK));
            remoteTask.task.setUuid(taskDao.uuidFromLocalId(remoteTask.task.getId()));
            remoteTask.gtaskMetadata = metadata;
        }
    }

    public long localIdForGtasksId(String gtasksId) {
        Metadata metadata = getMetadataByGtaskId(gtasksId);
        return metadata == null ? AbstractModel.NO_ID : metadata.getTask();
    }

    private Metadata getMetadataByGtaskId(String gtaskId) {
        return metadataDao.getFirst(Query.select(Metadata.PROPERTIES).where(Criterion.and(
                Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.ID.eq(gtaskId))));
    }

    // --- list iterating helpers

    public void iterateThroughList(GtasksList list, OrderedListIterator iterator) {
        String listId = list.getRemoteId();
        iterateThroughList(listId, iterator, 0, false);
    }

    private void iterateThroughList(String listId, final OrderedListIterator iterator, long startAtOrder, boolean reverse) {
        Field orderField = Functions.cast(GtasksMetadata.ORDER, "LONG");
        Order order = reverse ? Order.desc(orderField) : Order.asc(orderField);
        Criterion startAtCriterion = reverse ?  Functions.cast(GtasksMetadata.ORDER, "LONG").lt(startAtOrder) :
            Functions.cast(GtasksMetadata.ORDER, "LONG").gt(startAtOrder - 1);

        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq(listId),
                        startAtCriterion)).
                        orderBy(order);

        metadataDao.query(query, new Callback<Metadata>() {
            @Override
            public void apply(Metadata entry) {
                long taskId = entry.getValue(Metadata.TASK);
                Metadata metadata = getTaskMetadata(taskId);
                if(metadata != null) {
                    iterator.processTask(taskId, metadata);
                }
            }
        });
    }

    /**
     * Gets the remote id string of the parent task
     */
    public String getRemoteParentId(Metadata gtasksMetadata) {
        String parent = null;
        if (gtasksMetadata.containsNonNullValue(GtasksMetadata.PARENT_TASK)) {
            long parentId = gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK);
            Metadata parentMetadata = getTaskMetadata(parentId);
            if (parentMetadata != null && parentMetadata.containsNonNullValue(GtasksMetadata.ID)) {
                parent = parentMetadata.getValue(GtasksMetadata.ID);
                if (TextUtils.isEmpty(parent)) {
                    parent = null;
                }
            }
        }
        return parent;
    }

    /**
     * Gets the remote id string of the previous sibling task
     */
    public String getRemoteSiblingId(String listId, Metadata gtasksMetadata) {
        final AtomicInteger indentToMatch = new AtomicInteger(gtasksMetadata.getValue(GtasksMetadata.INDENT));
        final AtomicLong parentToMatch = new AtomicLong(gtasksMetadata.getValue(GtasksMetadata.PARENT_TASK));
        final AtomicReference<String> sibling = new AtomicReference<>();
        OrderedListIterator iterator = new OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                Task t = taskDao.fetch(taskId, Task.TITLE, Task.DELETION_DATE);
                if (t == null || t.isDeleted()) {
                    return;
                }
                int currIndent = metadata.getValue(GtasksMetadata.INDENT);
                long currParent = metadata.getValue(GtasksMetadata.PARENT_TASK);

                if (currIndent == indentToMatch.get() && currParent == parentToMatch.get()) {
                    if (sibling.get() == null) {
                        sibling.set(metadata.getValue(GtasksMetadata.ID));
                    }
                }
            }
        };

        this.iterateThroughList(listId, iterator, gtasksMetadata.getValue(GtasksMetadata.ORDER), true);
        return sibling.get();
    }
}
