package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

public class GtasksTaskListUpdater {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;

    private final HashMap<Long, Long> parents = new HashMap<Long, Long>();
    private final HashMap<Long, Long> siblings = new HashMap<Long, Long>();
    private final HashMap<Long, String> localToRemoteIdMap =
        new HashMap<Long, String>();

    public GtasksTaskListUpdater() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Update order and parent fields for all tasks in the given list
     * @param listId
     */
    public void updateMetadataForList(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        final ArrayList<Long> ids = new ArrayList<Long>();
        final Stack<Long> taskHierarchyStack = new Stack<Long>();

        final AtomicInteger order = new AtomicInteger(0);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId) {
                ids.add(taskId);
                Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
                if(metadata == null)
                    return;

                metadata.setValue(GtasksMetadata.ORDER, order.getAndAdd(1));
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                for(int i = indent; i <= previousIndent.get(); i++) {
                    if(!taskHierarchyStack.isEmpty())
                        taskHierarchyStack.pop();
                }

                if(indent > 0) {
                    if(taskHierarchyStack.isEmpty()) {
                        metadata.setValue(GtasksMetadata.PARENT_TASK, 0L);
                        metadata.setValue(GtasksMetadata.INDENT, 0);
                    } else
                        metadata.setValue(GtasksMetadata.PARENT_TASK, taskHierarchyStack.peek());
                } else {
                    metadata.setValue(GtasksMetadata.PARENT_TASK, 0L);
                }

                PluginServices.getMetadataService().save(metadata);
                taskHierarchyStack.push(taskId);
                previousIndent.set(indent);
            }
        });

        PluginServices.getTaskService().clearDetails(Task.ID.in(ids));
    }

    /**
     * Create a local tree of tasks to expedite sibling and parent lookups
     */
    public void createParentSiblingMaps() {
        for(StoreObject list : gtasksListService.getLists()) {
            final AtomicLong previousTask = new AtomicLong(-1L);
            final AtomicInteger previousIndent = new AtomicInteger(-1);

            iterateThroughList(list, new ListIterator() {
                @Override
                public void processTask(long taskId) {
                    Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
                    if(metadata == null)
                        return;

                    int indent = metadata.getValue(GtasksMetadata.INDENT);

                    final long parent, sibling;
                    if(indent > previousIndent.get()) {
                        parent = previousTask.get();
                        sibling = -1L;
                    } else if(indent == previousIndent.get()) {
                        sibling = previousTask.get();
                        parent = parents.get(sibling);
                    } else {
                        sibling = parents.get(previousTask.get());
                        parent = parents.get(sibling);
                    }
                    parents.put(taskId, parent);
                    siblings.put(taskId, sibling);

                    previousTask.set(taskId);
                    previousIndent.set(indent);
                    if(!TextUtils.isEmpty(metadata.getValue(GtasksMetadata.ID)))
                        localToRemoteIdMap.put(taskId, metadata.getValue(GtasksMetadata.ID));
                }
            });
        }
    }

    private interface ListIterator {
        public void processTask(long taskId);
    }

    private void iterateThroughList(StoreObject list, ListIterator iterator) {
        Filter filter = GtasksFilterExposer.filterFromList(list);
        TodorooCursor<Task> cursor = PluginServices.getTaskService().fetchFiltered(filter.sqlQuery, null, Task.ID);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long taskId = cursor.getLong(0);
                iterator.processTask(taskId);
            }

        } finally {
            cursor.close();
        }
    }
}

