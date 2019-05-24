package com.todoroo.astrid.adapter;

import android.text.TextUtils;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tasks.data.TaskContainer;
import org.tasks.data.TaskListMetadata;
import org.tasks.tasklist.ViewHolder;
import timber.log.Timber;

public final class AstridTaskAdapter extends TaskAdapter {

  private final TaskListMetadata list;
  private final Filter filter;
  private final SubtasksFilterUpdater updater;
  private final TaskDao taskDao;
  private final Map<String, ArrayList<String>> chainedCompletions =
      Collections.synchronizedMap(new HashMap<>());

  public AstridTaskAdapter(
      TaskListMetadata list, Filter filter, SubtasksFilterUpdater updater, TaskDao taskDao) {
    this.list = list;
    this.filter = filter;
    this.updater = updater;
    this.taskDao = taskDao;
  }

  @Override
  public int getIndent(TaskContainer task) {
    return updater.getIndentForTask(task.getUuid());
  }

  @Override
  public boolean canMove(ViewHolder source, ViewHolder target) {
    return true;
  }

  public boolean canIndent(int position, TaskContainer task) {
    String parentUuid = getItemUuid(position - 1);
    int parentIndent = updater.getIndentForTask(parentUuid);
    return getIndent(task) <= parentIndent;
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public void moved(int from, int to, int indent) {
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
  public void swiped(int which, int delta) {
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

  @Override
  public void onCompletedTask(TaskContainer item, boolean completedState) {
    final String itemId = item.getUuid();

    final long completionDate = completedState ? DateUtilities.now() : 0;

    if (!completedState) {
      ArrayList<String> chained = chainedCompletions.get(itemId);
      if (chained != null) {
        for (String taskId : chained) {
          taskDao.setCompletionDate(taskId, completionDate);
        }
      }
      return;
    }

    final ArrayList<String> chained = new ArrayList<>();
    updater.applyToDescendants(
        itemId,
        node -> {
          String uuid = node.uuid;
          taskDao.setCompletionDate(uuid, completionDate);
          chained.add(node.uuid);
        });

    if (chained.size() > 0) {
      // move recurring items to item parent
      List<Task> tasks = taskDao.getRecurringTasks(chained);

      boolean madeChanges = false;
      for (Task t : tasks) {
        if (!TextUtils.isEmpty(t.getRecurrence())) {
          updater.moveToParentOf(t.getUuid(), itemId);
          madeChanges = true;
        }
      }

      if (madeChanges) {
        updater.writeSerialization(list, updater.serializeTree());
      }

      chainedCompletions.put(itemId, chained);
    }
  }

  @Override
  public boolean supportsHiddenTasks() {
    return false;
  }
}
