package com.todoroo.astrid.service;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;

public class TaskMover {
  private final TaskDao taskDao;
  private final CaldavDao caldavDao;
  private final GoogleTaskDao googleTaskDao;
  private final SyncAdapters syncAdapters;
  private final GoogleTaskListDao googleTaskListDao;
  private final Preferences preferences;

  @Inject
  public TaskMover(
      TaskDao taskDao,
      CaldavDao caldavDao,
      GoogleTaskDao googleTaskDao,
      SyncAdapters syncAdapters,
      GoogleTaskListDao googleTaskListDao,
      Preferences preferences) {
    this.taskDao = taskDao;
    this.caldavDao = caldavDao;
    this.googleTaskDao = googleTaskDao;
    this.syncAdapters = syncAdapters;
    this.googleTaskListDao = googleTaskListDao;
    this.preferences = preferences;
  }

  public void move(List<Long> tasks, Filter selectedList) {
    tasks = newArrayList(tasks);
    tasks.removeAll(googleTaskDao.findChildrenInList(tasks));
    for (Task task : taskDao.fetch(tasks)) {
      performMove(task, selectedList);
      task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
      task.setModificationDate(now());
      taskDao.save(task);
    }
    syncAdapters.sync();
  }

  public void move(Task task, Filter selectedList) {
    performMove(task, selectedList);
  }

  public Filter getSingleFilter(List<Long> tasks) {
    List<String> caldavCalendars = caldavDao.getCalendars(tasks);
    List<String> googleTaskLists = googleTaskDao.getLists(tasks);
    if (caldavCalendars.isEmpty()) {
      if (googleTaskLists.size() == 1) {
        return new GtasksFilter(googleTaskListDao.getByRemoteId(googleTaskLists.get(0)));
      }
    } else if (googleTaskLists.isEmpty()) {
      if (caldavCalendars.size() == 1) {
        return new CaldavFilter(caldavDao.getCalendar(caldavCalendars.get(0)));
      }
    }
    return null;
  }

  private void performMove(Task task, Filter selectedList) {
    long id = task.getId();
    GoogleTask googleTask = googleTaskDao.getByTaskId(id);
    List<GoogleTask> googleTaskChildren = emptyList();
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
      googleTaskChildren = googleTaskDao.getChildren(id);
      googleTaskDao.markDeleted(now(), id);
    }

    if (caldavTask != null) {
      caldavTask.setDeleted(now());
      caldavDao.update(caldavTask);
    }

    if (selectedList instanceof GtasksFilter) {
      String listId = ((GtasksFilter) selectedList).getRemoteId();
      googleTaskDao.insertAndShift(new GoogleTask(id, listId), preferences.addGoogleTasksToTop());
      if (!googleTaskChildren.isEmpty()) {
        googleTaskDao.insert(
            transform(
                googleTaskChildren,
                child -> {
                  GoogleTask newChild = new GoogleTask(child.getTask(), listId);
                  newChild.setOrder(child.getOrder());
                  newChild.setParent(id);
                  return newChild;
                }));
      }
    } else if (selectedList instanceof CaldavFilter) {
      String listId = ((CaldavFilter) selectedList).getUuid();
      caldavDao.insert(
          transform(
              concat(singletonList(id), transform(googleTaskChildren, GoogleTask::getTask)),
              _id -> new CaldavTask(_id, listId)));
    }
  }
}
