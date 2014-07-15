/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.text.TextUtils;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GtasksTaskListUpdater extends OrderedMetadataListUpdater<StoreObject> {

    private static final Logger log = LoggerFactory.getLogger(GtasksTaskListUpdater.class);

    /** map of task -> parent task */
    final HashMap<Long, Long> parents = new HashMap<>();

    /** map of task -> prior sibling */
    final HashMap<Long, Long> siblings = new HashMap<>();

    final HashMap<Long, String> localToRemoteIdMap =
        new HashMap<>();

    private final GtasksListService gtasksListService;
    private final GtasksMetadataService gtasksMetadataService;
    private final GtasksSyncService gtasksSyncService;
    private final MetadataDao metadataDao;
    private final GtasksMetadata gtasksMetadata;

    @Inject
    public GtasksTaskListUpdater(GtasksListService gtasksListService, GtasksMetadataService gtasksMetadataService,
                                 GtasksSyncService gtasksSyncService, MetadataDao metadataDao, GtasksMetadata gtasksMetadata) {
        super(metadataDao);
        this.gtasksListService = gtasksListService;
        this.gtasksMetadataService = gtasksMetadataService;
        this.gtasksSyncService = gtasksSyncService;
        this.metadataDao = metadataDao;
        this.gtasksMetadata = gtasksMetadata;
    }

    // --- overrides

    @Override
    protected IntegerProperty indentProperty() {
        return GtasksMetadata.INDENT;
    }

    @Override
    protected LongProperty orderProperty() {
        return GtasksMetadata.ORDER;
    }

    @Override
    protected LongProperty parentProperty() {
        return GtasksMetadata.PARENT_TASK;
    }

    @Override
    protected Metadata getTaskMetadata(long taskId) {
        return gtasksMetadataService.getTaskMetadata(taskId);
    }
    @Override
    protected Metadata createEmptyMetadata(StoreObject list, long taskId) {
        Metadata metadata = gtasksMetadata.createEmptyMetadata(taskId);
        metadata.setValue(GtasksMetadata.LIST_ID, list.getValue(GtasksList.REMOTE_ID));
        return metadata;
    }

    @Override
    protected void beforeIndent(StoreObject list) {
        updateParentSiblingMapsFor(list);
    }

    @Override
    protected void iterateThroughList(StoreObject list, OrderedListIterator iterator) {
        gtasksMetadataService.iterateThroughList(list, iterator);
    }

    @Override
    protected void onMovedOrIndented(Metadata metadata) {
        gtasksSyncService.triggerMoveForMetadata(metadata);
    }

    // --- used during synchronization

    /**
     * Update order, parent, and indentation fields for all tasks in the given list
     */
    public void correctMetadataForList(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT) {
            return;
        }

        updateParentSiblingMapsFor(list);

        final AtomicLong order = new AtomicLong(0);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        gtasksMetadataService.iterateThroughList(list, new OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                metadata.setValue(GtasksMetadata.ORDER, order.getAndAdd(1));
                int indent = metadata.getValue(GtasksMetadata.INDENT);
                if(indent > previousIndent.get() + 1) {
                    indent = previousIndent.get() + 1;
                }
                metadata.setValue(GtasksMetadata.INDENT, indent);

                Long parent = parents.get(taskId);
                if(parent == null || parent < 0) {
                    parent = Task.NO_ID;
                }
                metadata.setValue(GtasksMetadata.PARENT_TASK, parent);

                metadataDao.persist(metadata);
                previousIndent.set(indent);
            }
        });
    }

    public void correctOrderAndIndentForList(String listId) {
        orderAndIndentHelper(listId, new AtomicLong(0L), Task.NO_ID, 0,
                new HashSet<Long>());
    }

    private void orderAndIndentHelper(String listId, AtomicLong order, long parent, int indentLevel,
            HashSet<Long> alreadyChecked) {
        TodorooCursor<Metadata> metadata = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                        GtasksMetadata.LIST_ID.eq(listId), GtasksMetadata.PARENT_TASK.eq(parent)))
                .orderBy(Order.asc(Functions.cast(GtasksMetadata.GTASKS_ORDER, "INTEGER")))); //$NON-NLS-1$
        try {
            if (metadata.getCount() > 0) {
                for (metadata.moveToFirst(); !metadata.isAfterLast(); metadata.moveToNext()) {
                    Metadata curr = new Metadata(metadata);
                    if(alreadyChecked.contains(curr.getTask())) {
                        continue;
                    }

                    curr.setValue(GtasksMetadata.INDENT, indentLevel);
                    curr.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
                    metadataDao.saveExisting(curr);
                    alreadyChecked.add(curr.getTask());

                    orderAndIndentHelper(listId, order, curr.getTask(), indentLevel + 1, alreadyChecked);
                }
            }
        } finally {
            metadata.close();
        }
    }

    void updateParentSiblingMapsFor(StoreObject list) {
        final AtomicLong previousTask = new AtomicLong(Task.NO_ID);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        gtasksMetadataService.iterateThroughList(list, new OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                try {
                    long parent, sibling;
                    if(indent > previousIndent.get()) {
                        parent = previousTask.get();
                        sibling = Task.NO_ID;
                    } else if(indent == previousIndent.get()) {
                        sibling = previousTask.get();
                        parent = parents.get(sibling);
                    } else {
                        // move up once for each indent
                        sibling = previousTask.get();
                        for(int i = indent; i < previousIndent.get(); i++) {
                            sibling = parents.get(sibling);
                        }
                        if(parents.containsKey(sibling)) {
                            parent = parents.get(sibling);
                        } else {
                            parent = Task.NO_ID;
                        }
                    }
                    parents.put(taskId, parent);
                    siblings.put(taskId, sibling);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

                previousTask.set(taskId);
                previousIndent.set(indent);
                if(!TextUtils.isEmpty(metadata.getValue(GtasksMetadata.ID))) {
                    localToRemoteIdMap.put(taskId, metadata.getValue(GtasksMetadata.ID));
                }
            }
        });
    }
}

