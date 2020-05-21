package com.todoroo.astrid.service;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.emptyList;

import androidx.annotation.Nullable;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.preferences.Preferences;

public class TaskMover {
  private final TaskDao taskDao;
  private final CaldavDao caldavDao;
  private final GoogleTaskDao googleTaskDao;
  private final GoogleTaskListDao googleTaskListDao;
  private final Preferences preferences;
  private final LocalBroadcastManager localBroadcastManager;

  @Inject
  public TaskMover(
      TaskDao taskDao,
      CaldavDao caldavDao,
      GoogleTaskDao googleTaskDao,
      GoogleTaskListDao googleTaskListDao,
      Preferences preferences,
      LocalBroadcastManager localBroadcastManager) {
    this.taskDao = taskDao;
    this.caldavDao = caldavDao;
    this.googleTaskDao = googleTaskDao;
    this.googleTaskListDao = googleTaskListDao;
    this.preferences = preferences;
    this.localBroadcastManager = localBroadcastManager;
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

  public void move(List<Long> tasks, Filter selectedList) {
    tasks = new ArrayList<>(tasks);
    tasks.removeAll(googleTaskDao.findChildrenInList(tasks));
    tasks.removeAll(taskDao.findChildrenInList(tasks));
    taskDao.setParent(0, null, tasks);
    for (Task task : taskDao.fetch(tasks)) {
      performMove(task, selectedList);
    }
    if (selectedList instanceof CaldavFilter) {
      caldavDao.updateParents((((CaldavFilter) selectedList).getUuid()));
    }
    taskDao.touch(tasks);
    localBroadcastManager.broadcastRefresh();
  }

  private void performMove(Task task, @Nullable Filter selectedList) {
    long id = task.getId();

    GoogleTask googleTask = googleTaskDao.getByTaskId(id);
    if (googleTask != null) {
      moveGoogleTask(task, googleTask, selectedList);
      return;
    }

    CaldavTask caldavTask = caldavDao.getTask(id);
    if (caldavTask != null) {
      moveCaldavTask(task, caldavTask, selectedList);
      return;
    }

    moveLocalTask(task, selectedList);
  }

  private void moveGoogleTask(Task task, GoogleTask googleTask, Filter selected) {
    if (selected instanceof GtasksFilter
        && googleTask.getListId().equals(((GtasksFilter) selected).getRemoteId())) {
      return;
    }

    long id = googleTask.getTask();
    List<GoogleTask> children = googleTaskDao.getChildren(id);
    List<Long> childIds = from(children).transform(GoogleTask::getTask).toList();
    googleTaskDao.markDeleted(now(), id);

    if (selected instanceof GtasksFilter) {
      String listId = ((GtasksFilter) selected).getRemoteId();
      googleTaskDao.insertAndShift(new GoogleTask(id, listId), preferences.addTasksToTop());
      if (!children.isEmpty()) {
        googleTaskDao.insert(
            transform(
                children,
                child -> {
                  GoogleTask newChild = new GoogleTask(child.getTask(), listId);
                  newChild.setOrder(child.getOrder());
                  newChild.setParent(id);
                  return newChild;
                }));
      }
    } else if (selected instanceof CaldavFilter) {
      String listId = ((CaldavFilter) selected).getUuid();
      CaldavTask newParent = new CaldavTask(id, listId);
      caldavDao.insert(task, newParent, preferences.addTasksToTop());
      caldavDao.insert(
          transform(
              childIds,
              child -> {
                CaldavTask newChild = new CaldavTask(child, listId);
                newChild.setRemoteParent(newParent.getRemoteId());
                return newChild;
              }));
    } else {
      taskDao.setParent(task.getId(), task.getUuid(), childIds);
    }
  }

  private void moveCaldavTask(Task task, CaldavTask caldavTask, Filter selected) {
    if (selected instanceof CaldavFilter
        && caldavTask.getCalendar().equals(((CaldavFilter) selected).getUuid())) {
      return;
    }

    long id = task.getId();
    List<Long> childIds = taskDao.getChildren(id);
    List<Long> toDelete = newArrayList(id);
    List<CaldavTask> children = emptyList();
    if (!childIds.isEmpty()) {
      children = caldavDao.getTasks(childIds);
      toDelete.addAll(childIds);
    }
    caldavDao.markDeleted(now(), toDelete);

    if (selected instanceof CaldavFilter) {
      long id1 = caldavTask.getTask();
      String listId = ((CaldavFilter) selected).getUuid();
      CaldavTask newParent =
          new CaldavTask(id1, listId, caldavTask.getRemoteId(), caldavTask.getObject());
      newParent.setVtodo(caldavTask.getVtodo());
      caldavDao.insert(task, newParent, preferences.addTasksToTop());
      caldavDao.insert(
          transform(
              children,
              child -> {
                CaldavTask newChild =
                    new CaldavTask(child.getTask(), listId, child.getRemoteId(), child.getObject());
                newChild.setVtodo(child.getVtodo());
                newChild.setRemoteParent(child.getRemoteParent());
                return newChild;
              }));
    } else if (selected instanceof GtasksFilter) {
      moveToGoogleTasks(id, childIds, (GtasksFilter) selected);
    } else {
      taskDao.updateParentUids(from(children).transform(CaldavTask::getTask).toList());
    }
  }

  private void moveLocalTask(Task task, @Nullable Filter selected) {
    if (selected instanceof GtasksFilter) {
      moveToGoogleTasks(task.getId(), taskDao.getChildren(task.getId()), (GtasksFilter) selected);
    } else if (selected instanceof CaldavFilter) {
      long id = task.getId();
      String listId = ((CaldavFilter) selected).getUuid();
      Map<Long, CaldavTask> tasks = new HashMap<>();
      CaldavTask root = new CaldavTask(id, listId);
      for (Task child : taskDao.fetchChildren(task.getId())) {
        CaldavTask newTask = new CaldavTask(child.getId(), listId);
        long parent = child.getParent();
        newTask.setRemoteParent((parent == id ? root : tasks.get(parent)).getRemoteId());
        tasks.put(child.getId(), newTask);
      }
      caldavDao.insert(task, root, preferences.addTasksToTop());
      caldavDao.insert(tasks.values());
    }
  }

  private void moveToGoogleTasks(long id, List<Long> children, GtasksFilter filter) {
    taskDao.setParent(0, null, children);
    String listId = filter.getRemoteId();
    googleTaskDao.insertAndShift(new GoogleTask(id, listId), preferences.addTasksToTop());
    List<GoogleTask> newChildren = new ArrayList<>();
    for (int i = 0; i < children.size(); i++) {
      GoogleTask newChild = new GoogleTask(children.get(i), listId);
      newChild.setOrder(i);
      newChild.setParent(id);
      newChildren.add(newChild);
    }
    googleTaskDao.insert(newChildren);
  }
}
