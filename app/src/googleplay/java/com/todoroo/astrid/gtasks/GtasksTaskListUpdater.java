/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;

import org.tasks.injection.ApplicationScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

@ApplicationScope
public class GtasksTaskListUpdater {

    /** map of task -> parent task */
    final HashMap<Long, Long> parents = new HashMap<>();

    /** map of task -> prior sibling */
    final HashMap<Long, Long> siblings = new HashMap<>();

    private final GtasksSyncService gtasksSyncService;
    private final MetadataDao metadataDao;

    @Inject
    public GtasksTaskListUpdater(GtasksSyncService gtasksSyncService, MetadataDao metadataDao) {
        this.gtasksSyncService = gtasksSyncService;
        this.metadataDao = metadataDao;
    }

    public void initialize(Filter filter) {
        String query = GtasksFilter.toManualOrder(filter.getSqlQuery());
        filter.setFilterQueryOverride(query);
    }

    // --- overrides

    Metadata getTaskMetadata(long taskId) {
        return metadataDao.getFirstActiveByTaskAndKey(taskId, GtasksMetadata.METADATA_KEY);
    }

    private Metadata createEmptyMetadata(GtasksList list, long taskId) {
        Metadata metadata = GtasksMetadata.createEmptyMetadataWithoutList(taskId);
        metadata.setValue(GtasksMetadata.LIST_ID, list.getRemoteId());
        return metadata;
    }

    private void iterateThroughList(GtasksList list, OrderedListIterator iterator) {
        String listId = list.getRemoteId();
        gtasksSyncService.iterateThroughList(listId, iterator, 0, false);
    }

    private void onMovedOrIndented(Metadata metadata) {
        gtasksSyncService.triggerMoveForMetadata(metadata);
    }

    // --- used during synchronization

    public void correctOrderAndIndentForList(String listId) {
        orderAndIndentHelper(listId, new AtomicLong(0L), Task.NO_ID, 0,
                new HashSet<>());
    }

    private void orderAndIndentHelper(final String listId, final AtomicLong order, final long parent, final int indentLevel, final Set<Long> alreadyChecked) {
        Query query = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.LIST_ID.eq(listId),
                GtasksMetadata.PARENT_TASK.eq(parent)))
                .orderBy(Order.asc(Functions.cast(GtasksMetadata.GTASKS_ORDER, "INTEGER")));
        metadataDao.query(query, curr -> {
            if (!alreadyChecked.contains(curr.getTask())) {
                curr.setValue(GtasksMetadata.INDENT, indentLevel);
                curr.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
                metadataDao.saveExisting(curr);
                alreadyChecked.add(curr.getTask());

                orderAndIndentHelper(listId, order, curr.getTask(), indentLevel + 1, alreadyChecked);
            }
        }
        );
    }

    void updateParentSiblingMapsFor(GtasksList list) {
        final AtomicLong previousTask = new AtomicLong(Task.NO_ID);
        final AtomicInteger previousIndent = new AtomicInteger(-1);

        iterateThroughList(list, (taskId, metadata) -> {
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
                Timber.e(e, e.getMessage());
            }

            previousTask.set(taskId);
            previousIndent.set(indent);
        });
    }

    public interface OrderedListIterator {
        void processTask(long taskId, Metadata metadata);
    }

    /**
     * Indent a task and all its children
     */
    public void indent(final GtasksList list, final long targetTaskId, final int delta) {
        if(list == null) {
            return;
        }

        updateParentSiblingMapsFor(list);

        final AtomicInteger targetTaskIndent = new AtomicInteger(-1);
        final AtomicInteger previousIndent = new AtomicInteger(-1);
        final AtomicLong previousTask = new AtomicLong(Task.NO_ID);
        final AtomicLong globalOrder = new AtomicLong(-1);

        iterateThroughList(list, (taskId, metadata) -> {
            if(!metadata.isSaved()) {
                metadata = createEmptyMetadata(list, taskId);
            }
            int indent = metadata.containsNonNullValue(GtasksMetadata.INDENT) ?
                    metadata.getValue(GtasksMetadata.INDENT) : 0;

            long order = globalOrder.incrementAndGet();
            metadata.setValue(GtasksMetadata.ORDER, order);

            if(targetTaskId == taskId) {
                // if indenting is warranted, indent me and my children
                if(indent + delta <= previousIndent.get() + 1 && indent + delta >= 0) {
                    targetTaskIndent.set(indent);
                    metadata.setValue(GtasksMetadata.INDENT, indent + delta);

                    if(GtasksMetadata.PARENT_TASK != null) {
                        long newParent = computeNewParent(list,
                                taskId, indent + delta - 1);
                        if (newParent == taskId) {
                            metadata.setValue(GtasksMetadata.PARENT_TASK, Task.NO_ID);
                        } else {
                            metadata.setValue(GtasksMetadata.PARENT_TASK, newParent);
                        }
                    }
                    saveAndUpdateModifiedDate(metadata);
                }
            } else if(targetTaskIndent.get() > -1) {
                // found first task that is not beneath target
                if(indent <= targetTaskIndent.get()) {
                    targetTaskIndent.set(-1);
                } else {
                    metadata.setValue(GtasksMetadata.INDENT, indent + delta);
                    saveAndUpdateModifiedDate(metadata);
                }
            } else {
                previousIndent.set(indent);
                previousTask.set(taskId);
            }

            if(!metadata.isSaved()) {
                saveAndUpdateModifiedDate(metadata);
            }
        });
        onMovedOrIndented(getTaskMetadata(targetTaskId));
    }

    /**
     * Helper function to iterate through a list and compute a new parent for the target task
     * based on the target parent's indent
     */
    private long computeNewParent(GtasksList list, long targetTaskId, int targetParentIndent) {
        final AtomicInteger desiredParentIndent = new AtomicInteger(targetParentIndent);
        final AtomicLong targetTask = new AtomicLong(targetTaskId);
        final AtomicLong lastPotentialParent = new AtomicLong(Task.NO_ID);
        final AtomicBoolean computedParent = new AtomicBoolean(false);

        iterateThroughList(list, (taskId, metadata) -> {
            if (targetTask.get() == taskId) {
                computedParent.set(true);
            }

            int indent = metadata.getValue(GtasksMetadata.INDENT);
            if (!computedParent.get() && indent == desiredParentIndent.get()) {
                lastPotentialParent.set(taskId);
            }
        });

        if (lastPotentialParent.get() == Task.NO_ID) {
            return Task.NO_ID;
        }
        return lastPotentialParent.get();
    }

    // --- task moving

    /**
     * Move a task and all its children to the position right above
     * taskIdToMoveto. Will change the indent level to match taskIdToMoveTo.
     */
    void moveTo(GtasksList list, final long targetTaskId,
                       final long moveBeforeTaskId) {
        if(list == null) {
            return;
        }

        Node root = buildTreeModel(list);
        Node target = findNode(root, targetTaskId);

        if(target != null && target.parent != null) {
            if(moveBeforeTaskId == -1) {
                target.parent.children.remove(target);
                root.children.add(target);
                target.parent = root;
            } else {
                Node sibling = findNode(root, moveBeforeTaskId);
                if(sibling != null && !ancestorOf(target, sibling)) {
                    int index = sibling.parent.children.indexOf(sibling);

                    if(target.parent == sibling.parent &&
                            target.parent.children.indexOf(target) < index) {
                        index--;
                    }

                    target.parent.children.remove(target);
                    sibling.parent.children.add(index, target);
                    target.parent = sibling.parent;
                }
            }
        }

        traverseTreeAndWriteValues(list, root, new AtomicLong(0), -1);
        onMovedOrIndented(getTaskMetadata(targetTaskId));
    }

    private boolean ancestorOf(Node ancestor, Node descendant) {
        if(descendant.parent == ancestor) {
            return true;
        }
        if(descendant.parent == null) {
            return false;
        }
        return ancestorOf(ancestor, descendant.parent);
    }

    static class Node {
        public final long taskId;
        public Node parent;
        final ArrayList<Node> children = new ArrayList<>();

        Node(long taskId, Node parent) {
            this.taskId = taskId;
            this.parent = parent;
        }
    }

    private void traverseTreeAndWriteValues(GtasksList list, Node node, AtomicLong order, int indent) {
        if(node.taskId != Task.NO_ID) {
            Metadata metadata = getTaskMetadata(node.taskId);
            if(metadata == null) {
                metadata = createEmptyMetadata(list, node.taskId);
            }
            metadata.setValue(GtasksMetadata.ORDER, order.getAndIncrement());
            metadata.setValue(GtasksMetadata.INDENT, indent);
            boolean parentChanged = false;
            if(GtasksMetadata.PARENT_TASK != null && metadata.getValue(GtasksMetadata.PARENT_TASK) !=
                    node.parent.taskId) {
                parentChanged = true;
                metadata.setValue(GtasksMetadata.PARENT_TASK, node.parent.taskId);
            }
            saveAndUpdateModifiedDate(metadata);
            if(parentChanged) {
                onMovedOrIndented(metadata);
            }
        }

        for(Node child : node.children) {
            traverseTreeAndWriteValues(list, child, order, indent + 1);
        }
    }

    private Node findNode(Node node, long taskId) {
        if(node.taskId == taskId) {
            return node;
        }
        for(Node child : node.children) {
            Node found = findNode(child, taskId);
            if(found != null) {
                return found;
            }
        }
        return null;
    }

    private Node buildTreeModel(GtasksList list) {
        final Node root = new Node(Task.NO_ID, null);
        final AtomicInteger previoustIndent = new AtomicInteger(-1);
        final AtomicReference<Node> currentNode = new AtomicReference<>(root);

        iterateThroughList(list, (taskId, metadata) -> {
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
                for(int i = indent; i < previousIndentValue; i++) {
                    node = node.parent;
                    if(node == null) {
                        node = root;
                        break;
                    }
                }
                currentNode.set(new Node(taskId, node));
                node.children.add(currentNode.get());
            }

            previoustIndent.set(indent);
        });
        return root;
    }

    private void saveAndUpdateModifiedDate(Metadata metadata) {
        if(metadata.getSetValues().size() == 0) {
            return;
        }
        metadataDao.persist(metadata);
    }

    // --- task cascading operations

    interface OrderedListNodeVisitor {
        void visitNode(Node node);
    }

    /**
     * Apply an operation only to the children of the task
     */
    void applyToChildren(GtasksList list, long targetTaskId,
                                OrderedListNodeVisitor visitor) {

        Node root = buildTreeModel(list);
        Node target = findNode(root, targetTaskId);

        if(target != null) {
            for (Node child : target.children) {
                applyVisitor(child, visitor);
            }
        }
    }

    private void applyVisitor(Node node, OrderedListNodeVisitor visitor) {
        visitor.visitNode(node);
        for(Node child : node.children) {
            applyVisitor(child, visitor);
        }
    }

    /**
     * Removes a task from the order hierarchy and un-indent children
     */
    void onDeleteTask(GtasksList list, final long targetTaskId) {
        if(list == null) {
            return;
        }

        Node root = buildTreeModel(list);
        Node target = findNode(root, targetTaskId);

        if(target != null && target.parent != null) {
            int targetIndex = target.parent.children.indexOf(target);
            target.parent.children.remove(targetIndex);
            for(Node node : target.children) {
                node.parent = target.parent;
                target.parent.children.add(targetIndex++, node);
            }
        }

        traverseTreeAndWriteValues(list, root, new AtomicLong(0), -1);
    }
}

