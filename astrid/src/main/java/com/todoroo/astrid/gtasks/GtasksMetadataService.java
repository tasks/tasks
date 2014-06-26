/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.OrderedListIterator;
import com.todoroo.astrid.utility.SyncMetadataService;

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
public final class GtasksMetadataService extends SyncMetadataService<GtasksTaskContainer> {

    private MetadataService metadataService;

    @Inject
    public GtasksMetadataService(TaskDao taskDao, MetadataDao metadataDao, MetadataService metadataService) {
        super(taskDao, metadataDao);
        this.metadataService = metadataService;
    }

    public Criterion getLocalMatchCriteria(GtasksTaskContainer remoteTask) {
        return GtasksMetadata.ID.eq(remoteTask.gtaskMetadata.getValue(GtasksMetadata.ID));
    }

    @Override
    public Criterion getMetadataCriteria() {
        return MetadataCriteria.withKey(getMetadataKey());
    }

    @Override
    public String getMetadataKey() {
        return GtasksMetadata.METADATA_KEY;
    }

    public synchronized void findLocalMatch(GtasksTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID) {
            return;
        }
        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.withKey(getMetadataKey()),
                        getLocalMatchCriteria(remoteTask))));
        try {
            if(cursor.getCount() == 0) {
                return;
            }
            cursor.moveToFirst();
            remoteTask.task.setId(cursor.get(Metadata.TASK));
            remoteTask.task.setUuid(taskDao.uuidFromLocalId(remoteTask.task.getId()));
            remoteTask.gtaskMetadata = new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    public long localIdForGtasksId(String gtasksId) {
        TodorooCursor<Metadata> metadata = metadataDao.query(Query.select(Metadata.TASK).where(
                Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY), GtasksMetadata.ID.eq(gtasksId))));
        try {
            if (metadata.getCount() > 0) {
                metadata.moveToFirst();
                return (new Metadata(metadata).getTask());
            } else {
                return AbstractModel.NO_ID;
            }
        } finally {
            metadata.close();
        }
    }

    // --- list iterating helpers

    public void iterateThroughList(StoreObject list, OrderedListIterator iterator) {
        String listId = list.getValue(GtasksList.REMOTE_ID);
        iterateThroughList(listId, iterator, 0, false);
    }

    public void iterateThroughList(String listId, OrderedListIterator iterator, long startAtOrder, boolean reverse) {
        Field orderField = Functions.cast(GtasksMetadata.ORDER, "LONG");
        Order order = reverse ? Order.desc(orderField) : Order.asc(orderField);
        Criterion startAtCriterion = reverse ?  Functions.cast(GtasksMetadata.ORDER, "LONG").lt(startAtOrder) :
            Functions.cast(GtasksMetadata.ORDER, "LONG").gt(startAtOrder - 1);

        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                        MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq(listId),
                        startAtCriterion)).
                        orderBy(order);
        TodorooCursor<Metadata> cursor = metadataService.query(query);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long taskId = cursor.get(Metadata.TASK);
                Metadata metadata = getTaskMetadata(taskId);
                if(metadata == null) {
                    continue;
                }
                iterator.processTask(taskId, metadata);
            }

        } finally {
            cursor.close();
        }
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
