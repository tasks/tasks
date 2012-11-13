/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.OrderedListIterator;

public class GtasksTaskListUpdater extends OrderedMetadataListUpdater<StoreObject> {

    /** map of task -> parent task */
    final HashMap<Long, Long> parents = new HashMap<Long, Long>();

    /** map of task -> prior sibling */
    final HashMap<Long, Long> siblings = new HashMap<Long, Long>();

    final HashMap<Long, String> localToRemoteIdMap =
        new HashMap<Long, String>();

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksSyncService gtasksSyncService;
    @Autowired private MetadataDao metadataDao;

    public GtasksTaskListUpdater() {
        super();
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
    protected Metadata getTaskMetadata(StoreObject list, long taskId) {
        return gtasksMetadataService.getTaskMetadata(taskId);
    }
    @Override
    protected Metadata createEmptyMetadata(StoreObject list, long taskId) {
        Metadata metadata = GtasksMetadata.createEmptyMetadata(taskId);
        metadata.setValue(GtasksMetadata.LIST_ID, list.getValue(GtasksList.REMOTE_ID));
        return metadata;
    }

    @Override
    protected void beforeIndent(StoreObject list) {
        updateParentSiblingMapsFor(list);
    }

    @Override
    protected void iterateThroughList(Filter filter, StoreObject list, OrderedListIterator iterator) {
        gtasksMetadataService.iterateThroughList(list, iterator);
    }

    @Override
    protected void onMovedOrIndented(Metadata metadata) {
        gtasksSyncService.triggerMoveForMetadata(metadata);
    }

    // --- used during synchronization

    /**
     * Create a local tree of tasks to expedite sibling and parent lookups
     */
    public void createParentSiblingMaps() {
        for(StoreObject list : gtasksListService.getLists()) {
            updateParentSiblingMapsFor(list);
        }
    }

    /**
     * Update order, parent, and indentation fields for all tasks in the given list
     * @param listId
     */
    public void correctMetadataForList(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        updateParentSiblingMapsFor(list);

        final AtomicLong order = new AtomicLong(0);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        gtasksMetadataService.iterateThroughList(list, new OrderedListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                metadata.setValue(GtasksMetadata.ORDER, order.getAndAdd(1));
                int indent = metadata.getValue(GtasksMetadata.INDENT);
                if(indent > previousIndent.get() + 1)
                    indent = previousIndent.get() + 1;
                metadata.setValue(GtasksMetadata.INDENT, indent);

                Long parent = parents.get(taskId);
                if(parent == null || parent < 0)
                    parent = Task.NO_ID;
                metadata.setValue(GtasksMetadata.PARENT_TASK, parent);

                PluginServices.getMetadataService().save(metadata);
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
                Metadata curr = new Metadata();
                for (metadata.moveToFirst(); !metadata.isAfterLast(); metadata.moveToNext()) {
                    curr.readFromCursor(metadata);
                    if(alreadyChecked.contains(curr.getValue(Metadata.TASK)))
                        continue;

                    curr.setValue(GtasksMetadata.INDENT, indentLevel);
                    curr.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
                    metadataDao.saveExisting(curr);
                    alreadyChecked.add(curr.getValue(Metadata.TASK));

                    orderAndIndentHelper(listId, order, curr.getValue(Metadata.TASK), indentLevel + 1, alreadyChecked);
                }
            }
        } finally {
            metadata.close();
        }
    }

    private void updateParentSiblingMapsFor(StoreObject list) {
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
                        for(int i = indent; i < previousIndent.get(); i++)
                            sibling = parents.get(sibling);
                        if(parents.containsKey(sibling))
                            parent = parents.get(sibling);
                        else
                            parent = Task.NO_ID;
                    }
                    parents.put(taskId, parent);
                    siblings.put(taskId, sibling);
                } catch (Exception e) {
                    Log.e("gtasks-task-updating", "Caught exception", e); //$NON-NLS-1$ //$NON-NLS-2$
                }

                previousTask.set(taskId);
                previousIndent.set(indent);
                if(!TextUtils.isEmpty(metadata.getValue(GtasksMetadata.ID)))
                    localToRemoteIdMap.put(taskId, metadata.getValue(GtasksMetadata.ID));
            }
        });
    }

}

