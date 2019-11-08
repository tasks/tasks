package com.todoroo.astrid.adapter;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import org.tasks.BuildConfig;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.TaskContainer;

public final class GoogleTaskAdapter extends GoogleTaskManualSortAdapter {

  private final boolean newTasksOnTop;

  GoogleTaskAdapter(TaskDao taskDao, GoogleTaskDao googleTaskDao, boolean newTasksOnTop) {
    super(taskDao, googleTaskDao);
    this.newTasksOnTop = newTasksOnTop;
  }

  @Override
  public void moved(int from, int to, int indent) {
    TaskContainer task = getTask(from);
    GoogleTask googleTask = task.getGoogleTask();
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

    Task update = task.getTask();
    update.setModificationDate(now());
    update.putTransitory(SyncFlags.FORCE_SYNC, true);
    taskDao.save(update);

    if (BuildConfig.DEBUG) {
      googleTaskDao.validateSorting(task.getGoogleTaskList());
    }
  }
}
