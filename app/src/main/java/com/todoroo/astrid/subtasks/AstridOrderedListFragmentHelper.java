package com.todoroo.astrid.subtasks;

import android.text.TextUtils;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.AstridTaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.data.TaskListMetadata;

class AstridOrderedListFragmentHelper {

  private final SubtasksFilterUpdater updater;
  private final TaskDao taskDao;
  private final Map<String, ArrayList<String>> chainedCompletions =
      Collections.synchronizedMap(new HashMap<>());
  private TaskListFragment fragment;
  private TaskListMetadata list;

  @Inject
  AstridOrderedListFragmentHelper(SubtasksFilterUpdater updater, TaskDao taskDao) {
    this.updater = updater;
    this.taskDao = taskDao;
  }

  void setTaskListFragment(TaskListFragment fragment) {
    this.fragment = fragment;
  }

  void beforeSetUpTaskList(Filter filter) {
    updater.initialize(list, filter);
  }

  TaskAdapter createTaskAdapter() {
    AstridTaskAdapter taskAdapter = new AstridTaskAdapter(list, fragment.getFilter(), updater);

    taskAdapter.setOnCompletedTaskListener(this::setCompletedForItemAndSubtasks);

    return taskAdapter;
  }

  private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
    final String itemId = item.getUuid();

    final long completionDate = completedState ? DateUtilities.now() : 0;

    if (!completedState) {
      ArrayList<String> chained = chainedCompletions.get(itemId);
      if (chained != null) {
        for (String taskId : chained) {
          taskDao.setCompletionDate(taskId, completionDate);
        }
        fragment.loadTaskListContent();
      }
      return;
    }

    final ArrayList<String> chained = new ArrayList<>();
    updater.applyToDescendants(
        itemId,
        node -> {
          String uuid = node.uuid;
          taskDao.setCompletionDate(uuid, completionDate);
          chained.add(node.uuid);
        });

    if (chained.size() > 0) {
      // move recurring items to item parent
      List<Task> tasks = taskDao.getRecurringTasks(chained);

      boolean madeChanges = false;
      for (Task t : tasks) {
        if (!TextUtils.isEmpty(t.getRecurrence())) {
          updater.moveToParentOf(t.getUuid(), itemId);
          madeChanges = true;
        }
      }

      if (madeChanges) {
        updater.writeSerialization(list, updater.serializeTree());
      }

      chainedCompletions.put(itemId, chained);
      fragment.loadTaskListContent();
    }
  }

  public void setList(TaskListMetadata list) {
    this.list = list;
  }

  void onDeleteTask(Task task) {
    updater.onDeleteTask(list, fragment.getFilter(), task.getUuid());
    fragment.loadTaskListContent();
  }
}
