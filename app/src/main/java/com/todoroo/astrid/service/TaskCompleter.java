package com.todoroo.astrid.service;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.singletonList;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import timber.log.Timber;

public class TaskCompleter {

  private final TaskDao taskDao;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;

  @Inject
  TaskCompleter(TaskDao taskDao, GoogleTaskDao googleTaskDao, CaldavDao caldavDao) {
    this.taskDao = taskDao;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
  }

  public void setComplete(long taskId) {
    Task task = taskDao.fetch(taskId);
    if (task != null) {
      setComplete(task, true);
    } else {
      Timber.e("Could not find task with id %s", taskId);
    }
  }

  public void setComplete(Task item, boolean completed) {
    long completionDate = completed ? now() : 0L;
    setComplete(singletonList(item), completionDate);
    setComplete(googleTaskDao.getChildTasks(item.getId()), completionDate);
    List<Long> caldavChildren = caldavDao.getChildren(singletonList(item.getId()));
    if (!caldavChildren.isEmpty()) {
      setComplete(taskDao.fetch(caldavChildren), completionDate);
    }
  }

  private void setComplete(List<Task> tasks, long completionDate) {
    for (Task task : tasks) {
      task.setCompletionDate(completionDate);
      taskDao.save(task);
    }
  }
}
