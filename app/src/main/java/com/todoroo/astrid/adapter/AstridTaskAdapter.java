package com.todoroo.astrid.adapter;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater;
import org.tasks.data.TaskListMetadata;
import timber.log.Timber;

public final class AstridTaskAdapter extends TaskAdapter {

  private final TaskListMetadata list;
  private final Filter filter;
  private final SubtasksFilterUpdater updater;

  public AstridTaskAdapter(
      TaskListMetadata list, Filter filter, SubtasksFilterUpdater updater) {
    this.list = list;
    this.filter = filter;
    this.updater = updater;
  }

  @Override
  public int getIndent(Task task) {
    return updater.getIndentForTask(task.getUuid());
  }

  @Override
  public boolean canIndent(int position, Task task) {
    String parentUuid = getItemUuid(position - 1);
    int parentIndent = updater.getIndentForTask(parentUuid);
    return getIndent(task) <= parentIndent;
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public void moved(int from, int to) {
    String targetTaskId = getItemUuid(from);
    if (!Task.isValidUuid(targetTaskId)) {
      return; // This can happen with gestures on empty parts of the list (e.g. extra space below
      // tasks)
    }

    try {
      if (to >= getCount()) {
        updater.moveTo(list, filter, targetTaskId, "-1"); // $NON-NLS-1$
      } else {
        String destinationTaskId = getItemUuid(to);
        updater.moveTo(list, filter, targetTaskId, destinationTaskId);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public void indented(int which, int delta) {
    String targetTaskId = getItemUuid(which);
    if (!Task.isValidUuid(targetTaskId)) {
      return; // This can happen with gestures on empty parts of the list (e.g. extra space below
      // tasks)
    }
    try {
      updater.indent(list, filter, targetTaskId, delta);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public void onTaskCreated(String uuid) {
    updater.onCreateTask(list, filter, uuid);
  }

  @Override
  public void onTaskDeleted(Task task) {
    updater.onDeleteTask(list, filter, task.getUuid());
  }
}
