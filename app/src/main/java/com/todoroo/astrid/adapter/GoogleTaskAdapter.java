package com.todoroo.astrid.adapter;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import org.tasks.BuildConfig;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.TaskContainer;
import org.tasks.tasklist.ViewHolder;

public final class GoogleTaskAdapter extends TaskAdapter {

  private final TaskDao taskDao;
  private final GoogleTaskDao googleTaskDao;

  GoogleTaskAdapter(TaskDao taskDao, GoogleTaskDao googleTaskDao) {
    this.taskDao = taskDao;
    this.googleTaskDao = googleTaskDao;
  }

  @Override
  public int getIndent(TaskContainer task) {
    return task.getIndent();
  }

  @Override
  public boolean canMove(ViewHolder sourceVh, ViewHolder targetVh) {
    TaskContainer source = sourceVh.task;
    int to = targetVh.getAdapterPosition();

    if (!source.hasChildren() || to <= 0 || to >= getCount() - 1) {
      return true;
    }

    TaskContainer target = targetVh.task;
    if (sourceVh.getAdapterPosition() < to) {
      if (target.hasChildren()) {
        return false;
      }
      if (target.hasParent()) {
        return target.isLastSubtask();
      }
      return true;
    } else {
      if (target.hasChildren()) {
        return true;
      }
      if (target.hasParent()) {
        return target.getParent() == source.getId() && target.secondarySort == 0;
      }
      return true;
    }
  }

  @Override
  public int maxIndent(int previousPosition, TaskContainer task) {
    return task.hasChildren() ? 0 : 1;
  }

  @Override
  public int minIndent(int nextPosition, TaskContainer task) {
    return task.hasChildren() || !getTask(nextPosition).hasParent() ? 0 : 1;
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public boolean supportsParentingOrManualSort() {
    return true;
  }

  @Override
  public void moved(int from, int to, int indent) {
    TaskContainer task = getTask(from);
    GoogleTask googleTask = task.getGoogleTask();
    TaskContainer previous = to > 0 ? getTask(to - 1) : null;

    if (previous == null) {
      googleTaskDao.move(googleTask, 0, 0);
    } else if (to == getCount() || to <= from) {
      if (indent == 0) {
        googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + 1);
      } else if (previous.hasParent()) {
        googleTaskDao.move(googleTask, previous.getParent(), previous.getSecondarySort() + 1);
      } else {
        googleTaskDao.move(googleTask, previous.getId(), 0);
      }
    } else {
      if (indent == 0) {
        googleTaskDao.move(
            googleTask,
            0,
            task.hasParent() ? previous.getPrimarySort() + 1 : previous.getPrimarySort());
      } else if (previous.hasParent()) {
        googleTaskDao.move(
            googleTask,
            previous.getParent(),
            task.getParent() == previous.getParent()
                ? previous.getSecondarySort()
                : previous.getSecondarySort() + 1);
      } else {
        googleTaskDao.move(googleTask, previous.getId(), 0);
      }
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
