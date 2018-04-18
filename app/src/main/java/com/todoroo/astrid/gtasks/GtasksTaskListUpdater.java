/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.ApplicationScope;
import timber.log.Timber;

@ApplicationScope
public class GtasksTaskListUpdater {

  /** map of task -> parent task */
  final HashMap<Long, Long> parents = new HashMap<>();

  /** map of task -> prior sibling */
  final HashMap<Long, Long> siblings = new HashMap<>();

  private final GtasksSyncService gtasksSyncService;
  private final GoogleTaskDao googleTaskDao;

  @Inject
  public GtasksTaskListUpdater(GtasksSyncService gtasksSyncService, GoogleTaskDao googleTaskDao) {
    this.gtasksSyncService = gtasksSyncService;
    this.googleTaskDao = googleTaskDao;
  }

  public void initialize(Filter filter) {
    String query = GtasksFilter.toManualOrder(filter.getSqlQuery());
    filter.setFilterQueryOverride(query);
  }

  // --- overrides

  GoogleTask getTaskMetadata(long taskId) {
    return googleTaskDao.getByTaskId(taskId);
  }

  private void iterateThroughList(GoogleTaskList list, OrderedListIterator iterator) {
    String listId = list.getRemoteId();
    gtasksSyncService.iterateThroughList(listId, iterator, 0, false);
  }

  private void onMovedOrIndented(GoogleTaskList googleTaskList, GoogleTask googleTask) {
    gtasksSyncService.triggerMoveForMetadata(googleTaskList, googleTask);
  }

  // --- used during synchronization

  public void correctOrderAndIndentForList(String listId) {
    orderAndIndentHelper(listId, new AtomicLong(0L), Task.NO_ID, 0, new HashSet<>());
  }

  private void orderAndIndentHelper(
      final String listId,
      final AtomicLong order,
      final long parent,
      final int indentLevel,
      final Set<Long> alreadyChecked) {
    for (GoogleTask curr : googleTaskDao.byRemoteOrder(listId, parent)) {
      if (!alreadyChecked.contains(curr.getTask())) {
        curr.setIndent(indentLevel);
        curr.setOrder(order.getAndIncrement());
        googleTaskDao.update(curr);
        alreadyChecked.add(curr.getTask());

        orderAndIndentHelper(listId, order, curr.getTask(), indentLevel + 1, alreadyChecked);
      }
    }
  }

  void updateParentSiblingMapsFor(GoogleTaskList list) {
    final AtomicLong previousTask = new AtomicLong(Task.NO_ID);
    final AtomicInteger previousIndent = new AtomicInteger(-1);

    iterateThroughList(
        list,
        (taskId, metadata) -> {
          int indent = metadata.getIndent();

          try {
            long parent, sibling;
            if (indent > previousIndent.get()) {
              parent = previousTask.get();
              sibling = Task.NO_ID;
            } else if (indent == previousIndent.get()) {
              sibling = previousTask.get();
              parent = parents.get(sibling);
            } else {
              // move up once for each indent
              sibling = previousTask.get();
              for (int i = indent; i < previousIndent.get(); i++) {
                sibling = parents.get(sibling);
              }
              if (parents.containsKey(sibling)) {
                parent = parents.get(sibling);
              } else {
                parent = Task.NO_ID;
              }
            }
            parents.put(taskId, parent);
            siblings.put(taskId, sibling);
          } catch (Exception e) {
            Timber.e(e);
          }

          previousTask.set(taskId);
          previousIndent.set(indent);
        });
  }

  /** Indent a task and all its children */
  public void indent(final GoogleTaskList list, final long targetTaskId, final int delta) {
    if (list == null) {
      return;
    }

    updateParentSiblingMapsFor(list);

    final AtomicInteger targetTaskIndent = new AtomicInteger(-1);
    final AtomicInteger previousIndent = new AtomicInteger(-1);
    final AtomicLong previousTask = new AtomicLong(Task.NO_ID);
    final AtomicLong globalOrder = new AtomicLong(-1);

    iterateThroughList(
        list,
        (taskId, googleTask) -> {
          int indent = googleTask.getIndent();

          long order = globalOrder.incrementAndGet();
          googleTask.setOrder(order);

          if (targetTaskId == taskId) {
            // if indenting is warranted, indent me and my children
            if (indent + delta <= previousIndent.get() + 1 && indent + delta >= 0) {
              targetTaskIndent.set(indent);
              googleTask.setIndent(indent + delta);

              long newParent = computeNewParent(list, taskId, indent + delta - 1);
              if (newParent == taskId) {
                googleTask.setParent(Task.NO_ID);
              } else {
                googleTask.setParent(newParent);
              }
              saveAndUpdateModifiedDate(googleTask);
            }
          } else if (targetTaskIndent.get() > -1) {
            // found first task that is not beneath target
            if (indent <= targetTaskIndent.get()) {
              targetTaskIndent.set(-1);
            } else {
              googleTask.setIndent(indent + delta);
              saveAndUpdateModifiedDate(googleTask);
            }
          } else {
            previousIndent.set(indent);
            previousTask.set(taskId);
          }

          saveAndUpdateModifiedDate(googleTask);
        });
    onMovedOrIndented(list, getTaskMetadata(targetTaskId));
  }

  /**
   * Helper function to iterate through a list and compute a new parent for the target task based on
   * the target parent's indent
   */
  private long computeNewParent(GoogleTaskList list, long targetTaskId, int targetParentIndent) {
    final AtomicInteger desiredParentIndent = new AtomicInteger(targetParentIndent);
    final AtomicLong targetTask = new AtomicLong(targetTaskId);
    final AtomicLong lastPotentialParent = new AtomicLong(Task.NO_ID);
    final AtomicBoolean computedParent = new AtomicBoolean(false);

    iterateThroughList(
        list,
        (taskId, googleTask) -> {
          if (targetTask.get() == taskId) {
            computedParent.set(true);
          }

          int indent = googleTask.getIndent();
          if (!computedParent.get() && indent == desiredParentIndent.get()) {
            lastPotentialParent.set(taskId);
          }
        });

    if (lastPotentialParent.get() == Task.NO_ID) {
      return Task.NO_ID;
    }
    return lastPotentialParent.get();
  }

  /**
   * Move a task and all its children to the position right above taskIdToMoveto. Will change the
   * indent level to match taskIdToMoveTo.
   */
  void moveTo(GoogleTaskList list, final long targetTaskId, final long moveBeforeTaskId) {
    if (list == null) {
      return;
    }

    Node root = buildTreeModel(list);
    Node target = findNode(root, targetTaskId);

    if (target != null && target.parent != null) {
      if (moveBeforeTaskId == -1) {
        target.parent.children.remove(target);
        root.children.add(target);
        target.parent = root;
      } else {
        Node sibling = findNode(root, moveBeforeTaskId);
        if (sibling != null && !ancestorOf(target, sibling)) {
          int index = sibling.parent.children.indexOf(sibling);

          if (target.parent == sibling.parent && target.parent.children.indexOf(target) < index) {
            index--;
          }

          target.parent.children.remove(target);
          sibling.parent.children.add(index, target);
          target.parent = sibling.parent;
        }
      }
    }

    traverseTreeAndWriteValues(list, root, new AtomicLong(0), -1);
    onMovedOrIndented(list, getTaskMetadata(targetTaskId));
  }

  // --- task moving

  private boolean ancestorOf(Node ancestor, Node descendant) {
    if (descendant.parent == ancestor) {
      return true;
    }
    if (descendant.parent == null) {
      return false;
    }
    return ancestorOf(ancestor, descendant.parent);
  }

  private void traverseTreeAndWriteValues(
      GoogleTaskList list, Node node, AtomicLong order, int indent) {
    if (node.taskId != Task.NO_ID) {
      GoogleTask googleTask = getTaskMetadata(node.taskId);
      if (googleTask == null) {
        googleTask = new GoogleTask(node.taskId, list.getRemoteId());
      }
      googleTask.setOrder(order.getAndIncrement());
      googleTask.setIndent(indent);
      boolean parentChanged = false;
      if (googleTask.getParent() != node.parent.taskId) {
        parentChanged = true;
        googleTask.setParent(node.parent.taskId);
      }
      saveAndUpdateModifiedDate(googleTask);
      if (parentChanged) {
        onMovedOrIndented(list, googleTask);
      }
    }

    for (Node child : node.children) {
      traverseTreeAndWriteValues(list, child, order, indent + 1);
    }
  }

  private Node findNode(Node node, long taskId) {
    if (node.taskId == taskId) {
      return node;
    }
    for (Node child : node.children) {
      Node found = findNode(child, taskId);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private Node buildTreeModel(GoogleTaskList list) {
    final Node root = new Node(Task.NO_ID, null);
    final AtomicInteger previoustIndent = new AtomicInteger(-1);
    final AtomicReference<Node> currentNode = new AtomicReference<>(root);

    iterateThroughList(
        list,
        (taskId, googleTask) -> {
          int indent = googleTask.getIndent();

          int previousIndentValue = previoustIndent.get();
          if (indent == previousIndentValue) { // sibling
            Node parent = currentNode.get().parent;
            currentNode.set(new Node(taskId, parent));
            parent.children.add(currentNode.get());
          } else if (indent > previousIndentValue) { // child
            Node parent = currentNode.get();
            currentNode.set(new Node(taskId, parent));
            parent.children.add(currentNode.get());
          } else { // in a different tree
            Node node = currentNode.get().parent;
            for (int i = indent; i < previousIndentValue; i++) {
              node = node.parent;
              if (node == null) {
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

  private void saveAndUpdateModifiedDate(GoogleTask googleTask) {
    googleTaskDao.update(googleTask);
  }

  /** Apply an operation only to the children of the task */
  void applyToChildren(GoogleTaskList list, long targetTaskId, OrderedListNodeVisitor visitor) {

    Node root = buildTreeModel(list);
    Node target = findNode(root, targetTaskId);

    if (target != null) {
      for (Node child : target.children) {
        applyVisitor(child, visitor);
      }
    }
  }

  private void applyVisitor(Node node, OrderedListNodeVisitor visitor) {
    visitor.visitNode(node);
    for (Node child : node.children) {
      applyVisitor(child, visitor);
    }
  }

  // --- task cascading operations

  /** Removes a task from the order hierarchy and un-indent children */
  void onDeleteTask(GoogleTaskList list, final long targetTaskId) {
    if (list == null) {
      return;
    }

    Node root = buildTreeModel(list);
    Node target = findNode(root, targetTaskId);

    if (target != null && target.parent != null) {
      int targetIndex = target.parent.children.indexOf(target);
      target.parent.children.remove(targetIndex);
      for (Node node : target.children) {
        node.parent = target.parent;
        target.parent.children.add(targetIndex++, node);
      }
    }

    traverseTreeAndWriteValues(list, root, new AtomicLong(0), -1);
  }

  public interface OrderedListIterator {

    void processTask(long taskId, GoogleTask googleTask);
  }

  interface OrderedListNodeVisitor {

    void visitNode(Node node);
  }

  static class Node {

    public final long taskId;
    final ArrayList<Node> children = new ArrayList<>();
    Node parent;

    Node(long taskId, Node parent) {
      this.taskId = taskId;
      this.parent = parent;
    }
  }
}
