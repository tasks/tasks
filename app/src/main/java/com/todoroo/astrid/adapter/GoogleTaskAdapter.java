package com.todoroo.astrid.adapter;

import com.todoroo.astrid.dao.TaskDao;
import org.tasks.BuildConfig;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.SubsetGoogleTask;
import org.tasks.data.TaskContainer;

public final class GoogleTaskAdapter extends GoogleTaskManualSortAdapter {

  private final boolean newTasksOnTop;

  GoogleTaskAdapter(TaskDao taskDao, GoogleTaskDao googleTaskDao, boolean newTasksOnTop) {
    super(taskDao, googleTaskDao);
    this.newTasksOnTop = newTasksOnTop;
  }

  @Override
  public boolean supportsManualSorting() {
    return false;
  }

  @Override
  public void moved(int from, int to, int indent) {
    TaskContainer task = getTask(from);
    SubsetGoogleTask googleTask = task.getGoogleTask();
    TaskContainer previous = to > 0 ? getTask(to - 1) : null;
    if (indent == 0) {
      if (googleTask.getIndent() == 0) {
        return;
      }
      googleTaskDao.move(
          googleTask, 0, newTasksOnTop ? 0 : googleTaskDao.getBottom(googleTask.getListId(), 0));
    } else {
      long newParent = previous.hasParent() ? previous.getParent() : previous.getId();
      if (googleTask.getParent() == newParent) {
        return;
      }
      googleTaskDao.move(
          googleTask,
          newParent,
          newTasksOnTop ? 0 : googleTaskDao.getBottom(googleTask.getListId(), newParent));
    }

    taskDao.touch(task.getId());

    if (BuildConfig.DEBUG) {
      googleTaskDao.validateSorting(task.getGoogleTaskList());
    }
  }
}
