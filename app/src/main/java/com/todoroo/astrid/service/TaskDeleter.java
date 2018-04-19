package com.todoroo.astrid.service;

import static com.todoroo.andlib.sql.Criterion.all;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.notCompleted;

import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.DeletionDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.jobs.JobManager;

public class TaskDeleter {

  private final JobManager jobManager;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final DeletionDao deletionDao;

  @Inject
  public TaskDeleter(DeletionDao deletionDao, JobManager jobManager, TaskDao taskDao, LocalBroadcastManager localBroadcastManager) {
    this.deletionDao = deletionDao;
    this.jobManager = jobManager;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
  }

  public int purgeDeleted() {
    List<Long> deleted = deletionDao.getDeleted();
    deletionDao.delete(deleted);
    return deleted.size();
  }

  public void markDeleted(Task item) {
    markDeleted(ImmutableList.of(item.getId()));
  }

  public List<Task> markDeleted(List<Long> taskIds) {
    List<Task> tasks = taskDao.fetch(taskIds);
    deletionDao.markDeleted(now(), taskIds);
    jobManager.cleanup(taskIds);
    jobManager.syncNow();
    localBroadcastManager.broadcastRefresh();
    return tasks;
  }

  public void delete(Task task) {
    delete(ImmutableList.of(task.getId()));
  }

  public void delete(List<Long> tasks) {
    deletionDao.delete(tasks);
    jobManager.cleanup(tasks);
    localBroadcastManager.broadcastRefresh();
  }

  public int clearCompleted(Filter filter) {
    List<Long> completed = new ArrayList<>();
    String query =
        filter
            .getSqlQuery()
            .replace(isVisible().toString(), all.toString())
            .replace(notCompleted().toString(), all.toString());
    for (Task task : taskDao.fetchFiltered(query)) {
      if (task.isCompleted()) {
        completed.add(task.getId());
      }
    }
    markDeleted(completed);
    return completed.size();
  }

  public void delete(GoogleTaskList googleTaskList) {
    List<Long> ids = deletionDao.delete(googleTaskList);
    jobManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(GoogleTaskAccount googleTaskAccount) {
    List<Long> ids = deletionDao.delete(googleTaskAccount);
    jobManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(CaldavCalendar caldavCalendar) {
    List<Long> ids = deletionDao.delete(caldavCalendar);
    jobManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(CaldavAccount caldavAccount) {
    List<Long> ids = deletionDao.delete(caldavAccount);
    jobManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }
}
