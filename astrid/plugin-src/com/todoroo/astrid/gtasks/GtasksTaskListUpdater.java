package com.todoroo.astrid.gtasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;

public class GtasksTaskListUpdater {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private MetadataDao metadataDao;

    /** map of task -> parent task */
    final HashMap<Long, Long> parents = new HashMap<Long, Long>();

    /** map of task -> prior sibling */
    final HashMap<Long, Long> siblings = new HashMap<Long, Long>();

    final HashMap<Long, String> localToRemoteIdMap =
        new HashMap<Long, String>();

    public GtasksTaskListUpdater() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- task indenting

    /**
     * Indent a task and all its children
     */
    public void indent(final long targetTaskId, final int delta) {
        Metadata targetMetadata = gtasksMetadataService.getTaskMetadata(targetTaskId);
        if(targetMetadata == null)
            return;
        StoreObject list = gtasksListService.getList(targetMetadata.getValue(GtasksMetadata.LIST_ID));
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        updateParentSiblingMapsFor(list);

        final AtomicInteger targetTaskIndent = new AtomicInteger(-1);
        final AtomicInteger previousIndent = new AtomicInteger(-1);
        final AtomicLong previousTask = new AtomicLong(-1);
        final AtomicReference<StoreObject> listRef = new AtomicReference<StoreObject>(list);

        gtasksMetadataService.iterateThroughList(list, new GtasksMetadataService.ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                if(targetTaskId == taskId) {
                    // if indenting is warranted, indent me and my children
                    if(indent + delta <= previousIndent.get() + 1 && indent + delta >= 0) {
                        targetTaskIndent.set(indent);
                        metadata.setValue(GtasksMetadata.INDENT, indent + delta);

                        long newParent = computeNewParent(listRef.get(), taskId, indent + delta - 1);
                        if (newParent == taskId) {
                            metadata.setValue(GtasksMetadata.PARENT_TASK, Task.NO_ID);
                        } else {
                            metadata.setValue(GtasksMetadata.PARENT_TASK, newParent);
                        }
                        saveAndUpdateModifiedDate(metadata, taskId);
                    }
                } else if(targetTaskIndent.get() > -1) {
                    // found first task that is not beneath target
                    if(indent <= targetTaskIndent.get())
                        targetTaskIndent.set(-1);
                    else {
                        metadata.setValue(GtasksMetadata.INDENT, indent + delta);
                        saveAndUpdateModifiedDate(metadata, taskId);
                    }
                } else {
                    previousIndent.set(indent);
                    previousTask.set(taskId);
                }
            }

        });
    }

    /**
     * Helper function to iterate through a list and compute a new parent for the target task
     * based on the target parent's indent
     * @param list
     * @param targetTaskId
     * @param newIndent
     * @return
     */
    private long computeNewParent(StoreObject list, long targetTaskId, int targetParentIndent) {
        final AtomicInteger desiredParentIndent = new AtomicInteger(targetParentIndent);
        final AtomicLong targetTask = new AtomicLong(targetTaskId);
        final AtomicLong lastPotentialParent = new AtomicLong(-1);
        final AtomicBoolean computedParent = new AtomicBoolean(false);

        GtasksMetadataService.ListIterator iterator = new GtasksMetadataService.ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                if (targetTask.get() == taskId) {
                    computedParent.set(true);
                }

                int indent = metadata.getValue(GtasksMetadata.INDENT);
                if (!computedParent.get() && indent == desiredParentIndent.get()) {
                    lastPotentialParent.set(taskId);
                }
            }
        };

        gtasksMetadataService.iterateThroughList(list, iterator);
        if (lastPotentialParent.get() == -1) return Task.NO_ID;
        return lastPotentialParent.get();
    }

    // --- task moving

    private static class Node {
        public final long taskId;
        public Node parent;
        public final ArrayList<Node> children = new ArrayList<Node>();

        public Node(long taskId, Node parent) {
            this.taskId = taskId;
            this.parent = parent;
        }
    }

    /**
     * Move a task and all its children to the position right above
     * taskIdToMoveto. Will change the indent level to match taskIdToMoveTo.
     *
     * @param newTaskId task we will move above. if -1, moves to end of list
     */
    public void moveTo(final long targetTaskId, final long moveBeforeTaskId) {
        Metadata targetMetadata = gtasksMetadataService.getTaskMetadata(targetTaskId);
        if(targetMetadata == null)
            return;
        StoreObject list = gtasksListService.getList(targetMetadata.getValue(GtasksMetadata.LIST_ID));
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        Node root = buildTreeModel(list);
        Node target = findNode(root, targetTaskId);

        if(target != null && target.parent != null) {
            if(moveBeforeTaskId == -1) {
                target.parent.children.remove(target);
                root.children.add(target);
                target.parent = root;
            } else {
                Node sibling = findNode(root, moveBeforeTaskId);
                if(sibling != null) {
                    int index = sibling.parent.children.indexOf(sibling);
                    target.parent.children.remove(target);
                    sibling.parent.children.add(index, target);
                    target.parent = sibling.parent;
                }
            }
        }

        traverseTreeAndWriteValues(root, new AtomicLong(0), -1);
    }

    private void traverseTreeAndWriteValues(Node node, AtomicLong order, int indent) {
        if(node.taskId != -1) {
            Metadata metadata = gtasksMetadataService.getTaskMetadata(node.taskId);
            if(metadata == null)
                metadata = GtasksMetadata.createEmptyMetadata(node.taskId);
            metadata.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
            metadata.setValue(GtasksMetadata.INDENT, indent);
            metadata.setValue(GtasksMetadata.PARENT_TASK, node.parent.taskId);
            saveAndUpdateModifiedDate(metadata, node.taskId);
        }

        for(Node child : node.children) {
            traverseTreeAndWriteValues(child, order, indent + 1);
        }
    }

    private Node findNode(Node node, long taskId) {
        if(node.taskId == taskId)
            return node;
        for(Node child : node.children) {
            Node found = findNode(child, taskId);
            if(found != null)
                return found;
        }
        return null;
    }

    private Node buildTreeModel(StoreObject list) {
        final Node root = new Node(-1, null);
        final AtomicInteger previoustIndent = new AtomicInteger(-1);
        final AtomicReference<Node> currentNode = new AtomicReference<Node>(root);

        gtasksMetadataService.iterateThroughList(list, new GtasksMetadataService.ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                int previousIndentValue = previoustIndent.get();
                if(indent == previousIndentValue) { // sibling
                    Node parent = currentNode.get().parent;
                    currentNode.set(new Node(taskId, parent));
                    parent.children.add(currentNode.get());
                } else if(indent > previousIndentValue) { // child
                    Node parent = currentNode.get();
                    currentNode.set(new Node(taskId, parent));
                    parent.children.add(currentNode.get());
                } else { // in a different tree
                    Node node = currentNode.get().parent;
                    for(int i = indent; i < previousIndentValue; i++)
                        node = node.parent;
                    if(node == null)
                        node = root;
                    currentNode.set(new Node(taskId, node));
                    node.children.add(currentNode.get());
                }

                previoustIndent.set(indent);
            }
        });
        return root;
    }

    // --- utility

    public void debugPrint(String listId) {
        StoreObject list = gtasksListService.getList(listId);
        if(list == GtasksListService.LIST_NOT_FOUND_OBJECT)
            return;

        gtasksMetadataService.iterateThroughList(list, new GtasksMetadataService.ListIterator() {
            public void processTask(long taskId, Metadata metadata) {
                System.err.format("id %d: order %d, indent:%d, parent:%d\n", taskId, //$NON-NLS-1$
                        metadata.getValue(GtasksMetadata.ORDER),
                        metadata.getValue(GtasksMetadata.INDENT),
                        metadata.getValue(GtasksMetadata.PARENT_TASK));
            }
        });
    }

    @SuppressWarnings("nls")
    public void debugPrint(Node root, int depth) {
        for(int i = 0; i < depth; i++) System.err.print(" + ");
        System.err.format("%03d", root.taskId);
        System.err.print("\n");

        for(int i = 0; i < root.children.size(); i++)
            debugPrint(root.children.get(i), depth + 1);
    }

    private final Task taskContainer = new Task();

    private void saveAndUpdateModifiedDate(Metadata metadata, long taskId) {
        if(metadata.getSetValues().size() == 0)
            return;
        PluginServices.getMetadataService().save(metadata);
        taskContainer.setId(taskId);
        taskContainer.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
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

        final AtomicLong order = new AtomicLong(0);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        gtasksMetadataService.iterateThroughList(list, new GtasksMetadataService.ListIterator() {
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
        orderAndIndentHelper(listId, new AtomicLong(0L), Task.NO_ID, 0);
    }

    private void orderAndIndentHelper(String listId, AtomicLong order, long parent, int indentLevel) {
        TodorooCursor<Metadata> metadata = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY), GtasksMetadata.LIST_ID.eq(listId), GtasksMetadata.PARENT_TASK.eq(parent)))
                .orderBy(Order.asc(GtasksMetadata.GTASKS_ORDER)));

        if (metadata.getCount() > 0) {
            Metadata curr = new Metadata();
            for (metadata.moveToFirst(); !metadata.isAfterLast(); metadata.moveToNext()) {
                curr.readFromCursor(metadata);
                curr.setValue(GtasksMetadata.INDENT, indentLevel);
                curr.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
                metadataDao.saveExisting(curr);

                orderAndIndentHelper(listId, order, curr.getValue(Metadata.TASK), indentLevel + 1);
            }
        }
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

        gtasksMetadataService.iterateThroughList(list, new GtasksMetadataService.ListIterator() {
            @Override
            public void processTask(long taskId, Metadata metadata) {
                int indent = metadata.getValue(GtasksMetadata.INDENT);

                try {
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
                        if(parents.containsKey(sibling))
                            parent = parents.get(sibling);
                        else
                            parent = -1L;
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


}

