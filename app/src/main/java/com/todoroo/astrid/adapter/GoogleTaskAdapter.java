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
    return task.getParent() > 0 ? 1 : 0;
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
  public boolean canIndent(int position, TaskContainer task) {
    return position > 0 && !task.hasChildren() && !task.hasParent();
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public void moved(int from, int to) {
    TaskContainer task = getTask(from);
    GoogleTask googleTask = task.getGoogleTask();
    if (to == 0) {
      googleTaskDao.move(googleTask, 0, 0);
    } else if (to == getCount()) {
      TaskContainer previous = getTask(to - 1);
      if (googleTask.getParent() > 0 && googleTask.getParent() == previous.getParent()) {
        googleTaskDao.move(googleTask, googleTask.getParent(), previous.getSecondarySort());
      } else {
        googleTaskDao.move(googleTask, 0, previous.getPrimarySort());
      }
    } else if (from < to) {
      TaskContainer previous = getTask(to - 1);
      TaskContainer next = getTask(to);
      if (previous.hasParent()) {
        if (next.hasParent()) {
          googleTaskDao.move(googleTask, next.getParent(), next.getSecondarySort());
        } else if (task.getParent() == previous.getParent() || next.hasParent()) {
          googleTaskDao.move(googleTask, previous.getParent(), previous.getSecondarySort());
        } else {
          googleTaskDao.move(googleTask, 0, previous.getPrimarySort());
        }
      } else if (previous.hasChildren()) {
        googleTaskDao.move(googleTask, previous.getId(), 0);
      } else if (task.hasParent()) {
        googleTaskDao.move(googleTask, 0, next.getPrimarySort());
      } else {
        googleTaskDao.move(googleTask, 0, previous.getPrimarySort());
      }
    } else {
      TaskContainer previous = getTask(to - 1);
      TaskContainer next = getTask(to);
      if (previous.hasParent()) {
        if (next.hasParent()) {
          googleTaskDao.move(googleTask, next.getParent(), next.getSecondarySort());
        } else if (task.getParent() == previous.getParent()) {
          googleTaskDao.move(googleTask, previous.getParent(), previous.getSecondarySort());
        } else {
          googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + 1);
        }
      } else if (previous.hasChildren()) {
        googleTaskDao.move(googleTask, previous.getId(), 0);
      } else {
        googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + 1);
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

  @Override
  public void indented(int which, int delta) {
    TaskContainer task = getTask(which);
    TaskContainer previous;
    GoogleTask current = task.getGoogleTask();
    if (delta == -1) {
      googleTaskDao.unindent(googleTaskDao.getByTaskId(task.getParent()), current);
    } else {
      previous = getTask(which - 1);
      googleTaskDao.indent(googleTaskDao.getByTaskId(previous.getId()), current);
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
