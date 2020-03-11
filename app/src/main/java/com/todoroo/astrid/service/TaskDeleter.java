package com.todoroo.astrid.service;

import static org.tasks.db.DbUtils.collect;
import static org.tasks.db.QueryUtils.removeOrder;
import static org.tasks.db.QueryUtils.showHiddenAndCompleted;

import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.DeletionDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.TaskContainer;
import org.tasks.data.TaskListQuery;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.Preferences;

public class TaskDeleter {

  private final WorkManager workManager;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final GoogleTaskDao googleTaskDao;
  private final Preferences preferences;
  private final DeletionDao deletionDao;

  @Inject
  public TaskDeleter(
      DeletionDao deletionDao,
      WorkManager workManager,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      GoogleTaskDao googleTaskDao,
      Preferences preferences) {
    this.deletionDao = deletionDao;
    this.workManager = workManager;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.googleTaskDao = googleTaskDao;
    this.preferences = preferences;
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
    Set<Long> ids = new HashSet<>(taskIds);
    ids.addAll(collect(taskIds, googleTaskDao::getChildren));
    ids.addAll(collect(taskIds, taskDao::getChildren));
    deletionDao.markDeleted(ids);
    workManager.cleanup(ids);
    workManager.sync(false);
    localBroadcastManager.broadcastRefresh();
    return collect(ids, taskDao::fetch);
  }

  public void delete(Task task) {
    delete(task.getId());
  }

  public void delete(Long task) {
    delete(ImmutableList.of(task));
  }

  public void delete(List<Long> tasks) {
    deletionDao.delete(tasks);
    workManager.cleanup(tasks);
    localBroadcastManager.broadcastRefresh();
  }

  public int clearCompleted(Filter filter) {
    List<Long> completed = new ArrayList<>();
    Filter deleteFilter = new Filter(null, null);
    deleteFilter.setFilterQueryOverride(
        removeOrder(showHiddenAndCompleted(filter.getOriginalSqlQuery())));
    List<TaskContainer> tasks =
        taskDao.fetchTasks(
            (includeGoogleSubtasks, includeCaldavSubtasks) ->
                TaskListQuery.getQuery(
                    preferences, deleteFilter, includeGoogleSubtasks, includeCaldavSubtasks));
    for (TaskContainer task : tasks) {
      if (task.isCompleted()) {
        completed.add(task.getId());
      }
    }
    markDeleted(completed);
    return completed.size();
  }

  public void delete(GoogleTaskList googleTaskList) {
    List<Long> ids = deletionDao.delete(googleTaskList);
    workManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(GoogleTaskAccount googleTaskAccount) {
    List<Long> ids = deletionDao.delete(googleTaskAccount);
    workManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(CaldavCalendar caldavCalendar) {
    List<Long> ids = deletionDao.delete(caldavCalendar);
    workManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }

  public void delete(CaldavAccount caldavAccount) {
    List<Long> ids = deletionDao.delete(caldavAccount);
    workManager.cleanup(ids);
    localBroadcastManager.broadcastRefresh();
    localBroadcastManager.broadcastRefreshList();
  }
}
