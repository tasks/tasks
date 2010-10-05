package com.todoroo.astrid.gtasks;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;

public class GtasksTaskListUpdater {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;

    /** map of task -> parent task */
    final HashMap<Long, Long> parents = new HashMap<Long, Long>();

    /** map of task -> prior sibling */
    final HashMap<Long, Long> siblings = new HashMap<Long, Long>();

    final HashMap<Long, String> localToRemoteIdMap =
        new HashMap<Long, String>();

    public GtasksTaskListUpdater() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- used during normal ui operations

    public void debugPrint(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        iterateThroughList(list, new ListIterator() {
            public void processTask(long taskId, Metadata metadata) {
                System.err.format("%d: %d, indent:%d, parent:%d\n", taskId, //$NON-NLS-1$
                        metadata.getValue(GtasksMetadata.ORDER),
                        metadata.getValue(GtasksMetadata.INDENT),
                        metadata.getValue(GtasksMetadata.PARENT_TASK));
            }
        });
    }

    /**
     * Indent a task and all its children
     */
    public void indent(String listId, final long targetTaskId, final int delta) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        updateParentSiblingMapsFor(list);

        final AtomicInteger targetTaskIndent = new AtomicInteger(-1);
        final AtomicInteger previousIndent = new AtomicInteger(-1);
        final AtomicLong previousTask = new AtomicLong(-1);
        final Task taskContainer = new Task();

        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                if(targetTaskId == taskId) {
                    // if indenting is warranted, indent me and my children
                    if(indent + delta <= previousIndent.get() + 1 && indent + delta >= 0) {
                        targetTaskIndent.set(indent);
                        metadata.setValue(GtasksMetadata.INDENT, indent + delta);
                        if(delta > 0)
                            metadata.setValue(GtasksMetadata.PARENT_TASK, previousTask.get());
                        else if(parents.containsKey(taskId))
                            metadata.setValue(GtasksMetadata.PARENT_TASK,
                                    parents.get(parents.get(taskId)));
                        else
                            metadata.setValue(GtasksMetadata.PARENT_TASK, Task.NO_ID);
                        if(PluginServices.getMetadataService().save(metadata))
                            updateModifiedDate(taskContainer, taskId);
                    }
                } else if(targetTaskIndent.get() > -1) {
                    // found first task that is not beneath target
                    if(indent <= targetTaskIndent.get())
                        targetTaskIndent.set(-1);
                    else {
                        metadata.setValue(GtasksMetadata.INDENT, indent + delta);
                        PluginServices.getMetadataService().save(metadata);
                        updateModifiedDate(taskContainer, taskId);
                    }
                } else {
                    previousIndent.set(indent);
                    previousTask.set(taskId);
                }
            }

        });
    }

    /**
     * Move a task and all its children.
     * <p>
     * if moving up and first task in list or moving down and last,
     * indents to same as task that we swapped with.
     *
     * @param delta # of positions to move
     *
     */
    public void move(String listId, final long targetTaskId, final int delta) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        long taskToSwap = -1;
        if(delta == -1) {
            // use sibling / parent map to figure out prior task
            updateParentSiblingMapsFor(list);
            if(siblings.containsKey(targetTaskId) && siblings.get(targetTaskId) != -1L)
                taskToSwap = siblings.get(targetTaskId);
            else if(parents.containsKey(targetTaskId) && parents.get(targetTaskId) != -1L)
                taskToSwap = parents.get(targetTaskId);
        } else {
            // walk through to find the next task
            Filter filter = GtasksFilterExposer.filterFromList(list);
            TodorooCursor<Task> cursor = PluginServices.getTaskService().fetchFiltered(filter.sqlQuery, null, Task.ID);
            try {
                int targetIndent = -1;
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    long taskId = cursor.getLong(0);

                    if(targetIndent != -1) {
                        Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
                        if(metadata.getValue(GtasksMetadata.INDENT) <= targetIndent) {
                            taskToSwap = taskId;
                            break;
                        }
                    } else if(taskId == targetTaskId) {
                        Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
                        targetIndent = metadata.getValue(GtasksMetadata.INDENT);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if(taskToSwap == -1L)
            return;

        if(delta == -1) {
            moveUp(list, targetTaskId, taskToSwap);
        } else {
            // adjust indent of target task to task to swap
            Metadata targetTask = gtasksMetadataService.getTaskMetadata(targetTaskId);
            Metadata nextTask = gtasksMetadataService.getTaskMetadata(taskToSwap);
            int targetIndent = targetTask.getValue(GtasksMetadata.INDENT);
            int nextIndent = nextTask.getValue(GtasksMetadata.INDENT);
            if(targetIndent != nextIndent)
                indent(listId, targetTaskId, nextIndent - targetIndent);
            moveUp(list, taskToSwap, targetTaskId);
        }
    }

    private void moveUp(StoreObject list, final long targetTaskId, final long priorTaskId) {
        final AtomicInteger priorTaskOrder = new AtomicInteger(-1);
        final AtomicInteger priorTaskIndent = new AtomicInteger(-1);
        final AtomicInteger targetTaskOrder = new AtomicInteger(0);
        final AtomicInteger targetTaskIndent = new AtomicInteger(-1);
        final AtomicInteger tasksToMove = new AtomicInteger(1);
        final AtomicBoolean finished = new AtomicBoolean(false);

        // step 1. calculate tasks to move
        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                if(finished.get() && priorTaskOrder.get() != -1)
                    return;

                if(taskId == priorTaskId) {
                    priorTaskIndent.set(metadata.getValue(GtasksMetadata.INDENT));
                    priorTaskOrder.set(metadata.getValue(GtasksMetadata.ORDER));
                } else if(targetTaskId == taskId) {
                    targetTaskIndent.set(metadata.getValue(GtasksMetadata.INDENT));
                    targetTaskOrder.set(metadata.getValue(GtasksMetadata.ORDER));
                } else if(targetTaskIndent.get() > -1) {
                    // found first task that is not beneath target
                    if(metadata.getValue(GtasksMetadata.INDENT) <= targetTaskIndent.get())
                        finished.set(true);
                    else
                        tasksToMove.incrementAndGet();
                }
            }
        });

        final AtomicBoolean priorFound = new AtomicBoolean(false);
        final AtomicBoolean targetFound = new AtomicBoolean(false);
        final Task taskContainer = new Task();
        finished.set(false);

        // step 2. swap the order of prior and our tasks
        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                if(finished.get())
                    return;

                if(targetTaskId == taskId)
                    targetFound.set(true);
                else if(taskId == priorTaskId)
                    priorFound.set(true);

                if(targetFound.get()) {
                    if(targetTaskId != taskId && metadata.getValue(GtasksMetadata.INDENT) <= targetTaskIndent.get())
                        finished.set(true);
                    else {
                        int newOrder = metadata.getValue(GtasksMetadata.ORDER) -
                            targetTaskOrder.get() + priorTaskOrder.get();
                        int newIndent = metadata.getValue(GtasksMetadata.INDENT) -
                            targetTaskIndent.get() + priorTaskIndent.get();

                        metadata.setValue(GtasksMetadata.ORDER, newOrder);
                        metadata.setValue(GtasksMetadata.INDENT, newIndent);
                        PluginServices.getMetadataService().save(metadata);
                        updateModifiedDate(taskContainer, taskId);
                    }
                } else if(priorFound.get()) {
                    int newOrder = metadata.getValue(GtasksMetadata.ORDER) +
                            tasksToMove.get();
                    metadata.setValue(GtasksMetadata.ORDER, newOrder);
                    PluginServices.getMetadataService().save(metadata);
                    updateModifiedDate(taskContainer, taskId);
                }
            }
        });

    }

    private void updateModifiedDate(Task taskContainer, long taskId) {
        taskContainer.setId(taskId);
        taskContainer.setValue(Task.DETAILS_DATE, DateUtilities.now());
        PluginServices.getTaskService().save(taskContainer);
    }

    // --- used during synchronization

    /**
     * Update order, parent, and indentation fields for all tasks in all lists
     */
    public void updateAllMetadata() {
        for(StoreObject list : gtasksListService.getLists()) {
            correctMetadataForList(list.getValue(GtasksList.REMOTE_ID));
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

        final AtomicInteger order = new AtomicInteger(0);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                metadata.setValue(GtasksMetadata.ORDER, order.getAndAdd(1));
                int indent = metadata.getValue(GtasksMetadata.INDENT);
                if(indent > previousIndent.get() + 1)
                    indent = previousIndent.get() + 1;
                metadata.setValue(GtasksMetadata.INDENT, indent);

                long parent = parents.get(taskId);
                if(parent < 0)
                    parent = Task.NO_ID;
                metadata.setValue(GtasksMetadata.PARENT_TASK, parent);

                PluginServices.getMetadataService().save(metadata);
                previousIndent.set(indent);
            }
        });
    }

    /**
     * Create a local tree of tasks to expedite sibling and parent lookups
     */
    public void createParentSiblingMaps() {
        for(StoreObject list : gtasksListService.getLists()) {
            updateParentSiblingMapsFor(list);
        }
    }

    private void updateParentSiblingMapsFor(StoreObject list) {
        final AtomicLong previousTask = new AtomicLong(-1L);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        iterateThroughList(list, new ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                long parent, sibling;
                if(indent > previousIndent.get()) {
                    parent = previousTask.get();
                    sibling = -1L;
                } else if(indent == previousIndent.get()) {
                    sibling = previousTask.get();
                    parent = parents.get(sibling);
                } else {
                    // move up once for each indent
                    sibling = previousTask.get();
                    for(int i = indent; i < previousIndent.get(); i++)
                        sibling = parents.get(sibling);
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

    /**
     * Must be called after creating parent and sibling maps. Updates a
     * task container's parent and sibling fields.
     *
     * @param container
     */
    public void updateParentAndSibling(GtasksTaskContainer container) {
        long taskId = container.task.getId();
        if(parents.containsKey(taskId)) {
            long parentId = parents.get(taskId);
            if(localToRemoteIdMap.containsKey(parentId))
                container.parentId = localToRemoteIdMap.get(parentId);
        }
        if(siblings.containsKey(taskId)) {
            long siblingId = siblings.get(taskId);
            if(localToRemoteIdMap.containsKey(siblingId))
                container.priorSiblingId = localToRemoteIdMap.get(siblingId);
        }
    }

    public void addRemoteTaskMapping(long id, String remoteId) {
        localToRemoteIdMap.put(id, remoteId);
    }

    // --- private helpers

    private interface ListIterator {
        public void processTask(long taskId, Metadata metadata);
    }

    private void iterateThroughList(StoreObject list, ListIterator iterator) {
        Filter filter = GtasksFilterExposer.filterFromList(list);
        TodorooCursor<Task> cursor = PluginServices.getTaskService().fetchFiltered(filter.sqlQuery, null, Task.ID);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long taskId = cursor.getLong(0);
                Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
                if(metadata == null)
                    continue;
                iterator.processTask(taskId, metadata);
            }

        } finally {
            cursor.close();
        }
    }

}

