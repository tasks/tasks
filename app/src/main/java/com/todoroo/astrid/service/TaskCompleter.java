package com.todoroo.astrid.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.DateUtilities.now;

import com.google.common.collect.Iterables;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskDao;
import timber.log.Timber;

public class TaskCompleter {

  private final TaskDao taskDao;
  private final GoogleTaskDao googleTaskDao;

  @Inject
  TaskCompleter(TaskDao taskDao, GoogleTaskDao googleTaskDao) {
    this.taskDao = taskDao;
    this.googleTaskDao = googleTaskDao;
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
    setComplete(Collections.singletonList(item), completionDate);
    List<Task> tasks = newArrayList(googleTaskDao.getChildTasks(item.getId()));
    List<Long> caldavChildren = taskDao.getChildren(item.getId());
    if (!caldavChildren.isEmpty()) {
      tasks.addAll(taskDao.fetch(caldavChildren));
    }
    setComplete(
        newArrayList(Iterables.filter(tasks, t -> t.isCompleted() != completed)), completionDate);
  }

  private void setComplete(List<Task> tasks, long completionDate) {
    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      task.setCompletionDate(completionDate);
      if (i < tasks.size() - 1) {
        task.suppressRefresh();
      }
      taskDao.save(task);
    }
  }
}
