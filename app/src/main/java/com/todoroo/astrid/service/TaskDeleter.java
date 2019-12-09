package com.todoroo.astrid.service;

import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.includeCompleted;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.includeHidden;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.notCompleted;
import static org.tasks.db.DbUtils.batch;

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
import org.tasks.data.CaldavDao;
import org.tasks.data.DeletionDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.TaskContainer;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.Preferences;
import org.tasks.ui.TaskListViewModel;

public class TaskDeleter {

  private final WorkManager workManager;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final Preferences preferences;
  private final DeletionDao deletionDao;

  @Inject
  public TaskDeleter(
      DeletionDao deletionDao,
      WorkManager workManager,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      Preferences preferences) {
    this.deletionDao = deletionDao;
    this.workManager = workManager;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
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
    batch(taskIds, i -> ids.addAll(googleTaskDao.getChildren(i)));
    batch(taskIds, i -> ids.addAll(caldavDao.getChildren(i)));
    deletionDao.markDeleted(ids);
    workManager.cleanup(taskIds);
    workManager.sync(false);
    localBroadcastManager.broadcastRefresh();
    return taskDao.fetch(taskIds);
  }

  public void delete(Task task) {
    delete(ImmutableList.of(task.getId()));
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
        filter
            .getOriginalSqlQuery()
            .replace(isVisible().toString(), includeHidden().toString())
            .replace(notCompleted().toString(), includeCompleted().toString()));
    List<TaskContainer> tasks = taskDao.fetchTasks(
        hasSubtasks -> TaskListViewModel.getQuery(preferences, deleteFilter, hasSubtasks));
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
