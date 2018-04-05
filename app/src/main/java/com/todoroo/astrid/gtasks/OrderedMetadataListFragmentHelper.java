/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.text.TextUtils;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import timber.log.Timber;

class OrderedMetadataListFragmentHelper {

  private final GtasksTaskListUpdater updater;
  private final GoogleTaskDao googleTaskDao;

  private final TaskDao taskDao;
  private final Map<Long, ArrayList<Long>> chainedCompletions =
      Collections.synchronizedMap(new HashMap<Long, ArrayList<Long>>());
  private DraggableTaskAdapter taskAdapter;
  private TaskListFragment fragment;
  private GoogleTaskList list;

  @Inject
  OrderedMetadataListFragmentHelper(
      TaskDao taskDao, GtasksTaskListUpdater updater, GoogleTaskDao googleTaskDao) {
    this.taskDao = taskDao;
    this.updater = updater;
    this.googleTaskDao = googleTaskDao;
  }

  void setTaskListFragment(TaskListFragment fragment) {
    this.fragment = fragment;
  }

  void beforeSetUpTaskList(Filter filter) {
    updater.initialize(filter);
  }

  TaskAdapter createTaskAdapter() {
    taskAdapter = new DraggableTaskAdapter();

    taskAdapter.setOnCompletedTaskListener(this::setCompletedForItemAndSubtasks);

    return taskAdapter;
  }

  private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
    final long itemId = item.getId();

    final long completionDate = completedState ? DateUtilities.now() : 0;

    if (!completedState) {
      ArrayList<Long> chained = chainedCompletions.get(itemId);
      if (chained != null) {
        for (Long taskId : chained) {
          Task task = taskDao.fetch(taskId);
          task.setCompletionDate(completionDate);
          taskDao.save(task);
        }
        fragment.loadTaskListContent();
      }
      return;
    }

    final ArrayList<Long> chained = new ArrayList<>();
    final int parentIndent = item.getIndent();
    updater.applyToChildren(
        list,
        itemId,
        node -> {
          Task childTask = taskDao.fetch(node.taskId);
          if (!TextUtils.isEmpty(childTask.getRecurrence())) {
            GoogleTask googleTask = updater.getTaskMetadata(node.taskId);
            googleTask.setIndent(parentIndent);
            googleTaskDao.update(googleTask);
          }
          childTask.setCompletionDate(completionDate);
          taskDao.save(childTask);

          chained.add(node.taskId);
        });

    if (chained.size() > 0) {
      chainedCompletions.put(itemId, chained);
      fragment.loadTaskListContent();
    }
  }

  public void setList(GoogleTaskList list) {
    this.list = list;
  }

  void onDeleteTask(Task task) {
    updater.onDeleteTask(list, task.getId());
    fragment.loadTaskListContent();
  }

  private final class DraggableTaskAdapter extends TaskAdapter {

    @Override
    public int getIndent(Task task) {
      return task.getIndent();
    }

    @Override
    public boolean canIndent(int position, Task task) {
      Task parent = taskAdapter.getTask(position - 1);
      return parent != null && getIndent(task) <= parent.getIndent();
    }

    @Override
    public boolean isManuallySorted() {
      return true;
    }

    @Override
    public void moved(int from, int to) {
      long targetTaskId = taskAdapter.getTaskId(from);
      if (targetTaskId <= 0) {
        return; // This can happen with gestures on empty parts of the list (e.g. extra space below
        // tasks)
      }

      try {
        if (to >= taskAdapter.getCount()) {
          updater.moveTo(list, targetTaskId, -1);
        } else {
          long destinationTaskId = taskAdapter.getTaskId(to);
          updater.moveTo(list, targetTaskId, destinationTaskId);
        }
      } catch (Exception e) {
        Timber.e(e);
      }
    }

    @Override
    public void indented(int which, int delta) {
      long targetTaskId = taskAdapter.getTaskId(which);
      if (targetTaskId <= 0) {
        return; // This can happen with gestures on empty parts of the list (e.g. extra space below
        // tasks)
      }
      try {
        updater.indent(list, targetTaskId, delta);
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }
}
