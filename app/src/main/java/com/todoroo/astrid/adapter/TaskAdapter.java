/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Longs.asList;

import com.todoroo.astrid.data.Task;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tasks.data.TaskContainer;
import org.tasks.tasklist.TaskListRecyclerAdapter;
import org.tasks.tasklist.ViewHolder;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TaskAdapter {

  private final Set<Long> selected = new HashSet<>();
  private TaskListRecyclerAdapter helper;

  public int getCount() {
    return helper.getItemCount();
  }

  public void setHelper(TaskListRecyclerAdapter helper) {
    this.helper = helper;
  }

  public int getNumSelected() {
    return selected.size();
  }

  public List<Long> getSelected() {
    return newArrayList(selected);
  }

  public void setSelected(long... ids) {
    selected.clear();
    selected.addAll(asList(ids));
  }

  public void clearSelections() {
    selected.clear();
  }

  public int getIndent(TaskContainer task) {
    return 0;
  }

  public boolean canMove(ViewHolder source, ViewHolder target) {
    return false;
  }

  public int maxIndent(int previousPosition, TaskContainer task) {
    return 0;
  }

  public int minIndent(int nextPosition, TaskContainer task) {
    return 0;
  }

  public boolean isSelected(TaskContainer task) {
    return selected.contains(task.getId());
  }

  public void toggleSelection(TaskContainer task) {
    long id = task.getId();
    if (selected.contains(id)) {
      selected.remove(id);
    } else {
      selected.add(id);
    }
  }

  public boolean isManuallySorted() {
    return false;
  }

  public boolean supportsParentingOrManualSort() {
    return false;
  }

  public void moved(int from, int to, int indent) {}

  public TaskContainer getTask(int position) {
    return helper.getItem(position);
  }

  String getItemUuid(int position) {
    return getTask(position).getUuid();
  }

  public void onCompletedTask(TaskContainer task, boolean newState) {}

  public void onTaskCreated(String uuid) {}

  public void onTaskDeleted(Task task) {}

  public boolean supportsHiddenTasks() {
    return true;
  }
}
