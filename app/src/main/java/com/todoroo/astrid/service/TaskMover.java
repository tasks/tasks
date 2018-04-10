package com.todoroo.astrid.service;

import static com.todoroo.andlib.utility.DateUtilities.now;

import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.sync.SyncAdapters;

public class TaskMover {
  private final TaskDao taskDao;
  private final CaldavDao caldavDao;
  private final GoogleTaskDao googleTaskDao;
  private final SyncAdapters syncAdapters;

  @Inject
  public TaskMover(TaskDao taskDao, CaldavDao caldavDao, GoogleTaskDao googleTaskDao, SyncAdapters syncAdapters) {
    this.taskDao = taskDao;
    this.caldavDao = caldavDao;
    this.googleTaskDao = googleTaskDao;
    this.syncAdapters = syncAdapters;
  }

  public void move(List<Long> tasks, Filter selectedList) {
    List<Task> fetch = taskDao.fetch(tasks);
    for (Task task : fetch) {
      performMove(task, selectedList);
      task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
      task.setModificationDate(now());
      taskDao.save(task);
    }
    syncAdapters.syncNow();
  }

  public void move(Task task, Filter selectedList) {
    performMove(task, selectedList);
  }

  private void performMove(Task task, Filter selectedList) {
    long id = task.getId();
    GoogleTask googleTask = googleTaskDao.getByTaskId(id);
    if (googleTask != null
        && selectedList instanceof GtasksFilter
        && googleTask.getListId().equals(((GtasksFilter) selectedList).getRemoteId())) {
      return;
    }
    CaldavTask caldavTask = caldavDao.getTask(id);
    if (caldavTask != null
        && selectedList instanceof CaldavFilter
        && caldavTask.getCalendar().equals(((CaldavFilter) selectedList).getUuid())) {
      return;
    }
    task.putTransitory(SyncFlags.FORCE_SYNC, true);
    if (googleTask != null) {
      googleTask.setDeleted(now());
      googleTaskDao.update(googleTask);
    }

    if (caldavTask != null) {
      caldavTask.setDeleted(now());
      caldavDao.update(caldavTask);
    }

    if (selectedList instanceof GtasksFilter) {
      googleTaskDao.insert(new GoogleTask(id, ((GtasksFilter) selectedList).getRemoteId()));
    } else if (selectedList instanceof CaldavFilter) {
      caldavDao.insert(
          new CaldavTask(id, ((CaldavFilter) selectedList).getUuid(), UUIDHelper.newUUID()));
    }
  }
}
