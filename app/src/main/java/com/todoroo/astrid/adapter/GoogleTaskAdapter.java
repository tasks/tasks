package com.todoroo.astrid.adapter;

import android.text.TextUtils;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.TaskContainer;
import timber.log.Timber;

public final class GoogleTaskAdapter extends TaskAdapter {

  private final GoogleTaskList list;
  private final GtasksTaskListUpdater updater;
  private final TaskDao taskDao;
  private final GoogleTaskDao googleTaskDao;
  private final Map<Long, ArrayList<Long>> chainedCompletions =
      Collections.synchronizedMap(new HashMap<>());

  public GoogleTaskAdapter(
      GoogleTaskList list,
      GtasksTaskListUpdater updater,
      TaskDao taskDao,
      GoogleTaskDao googleTaskDao) {
    this.list = list;
    this.updater = updater;
    this.taskDao = taskDao;
    this.googleTaskDao = googleTaskDao;
  }

  @Override
  public int getIndent(TaskContainer task) {
    return task.getIndent();
  }

  @Override
  public boolean canIndent(int position, TaskContainer task) {
    Task parent = getTask(position - 1);
    return parent != null && getIndent(task) == 0;
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public void moved(int from, int to) {
    long targetTaskId = getTaskId(from);
    if (targetTaskId <= 0) {
      return; // This can happen with gestures on empty parts of the list (e.g. extra space below
      // tasks)
    }

    try {
      if (to >= getCount()) {
        updater.moveTo(list, targetTaskId, -1);
      } else {
        long destinationTaskId = getTaskId(to);
        updater.moveTo(list, targetTaskId, destinationTaskId);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public void indented(int which, int delta) {
    long targetTaskId = getTaskId(which);
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

  @Override
  public void onTaskDeleted(Task task) {
    updater.onDeleteTask(list, task.getId());
  }

  @Override
  public void onCompletedTask(TaskContainer item, boolean completedState) {
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
    }
  }
}
