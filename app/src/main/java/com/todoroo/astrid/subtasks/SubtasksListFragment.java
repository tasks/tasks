/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;

/**
 * Fragment for subtasks
 *
 * @author Tim Su <tim@astrid.com>
 */
public class SubtasksListFragment extends TaskListFragment {

  @Inject Preferences preferences;
  @Inject @ForApplication Context context;
  @Inject TaskListMetadataDao taskListMetadataDao;
  @Inject TaskDao taskDao;
  @Inject AstridOrderedListFragmentHelper helper;

  public static TaskListFragment newSubtasksListFragment(Filter filter) {
    SubtasksListFragment fragment = new SubtasksListFragment();
    fragment.filter = filter;
    return fragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    helper.setTaskListFragment(this);
  }

  @Override
  public void setTaskAdapter() {
    helper.setList(initializeTaskListMetadata());
    helper.beforeSetUpTaskList(filter);

    super.setTaskAdapter();
  }

  private TaskListMetadata initializeTaskListMetadata() {
    String filterId = null;
    String prefId = null;
    if (BuiltInFilterExposer.isInbox(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_ALL;
      prefId = SubtasksFilterUpdater.ACTIVE_TASKS_ORDER;
    } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
      filterId = TaskListMetadata.FILTER_ID_TODAY;
      prefId = SubtasksFilterUpdater.TODAY_TASKS_ORDER;
    }
    if (TextUtils.isEmpty(filterId)) {
      return null;
    }
    TaskListMetadata taskListMetadata = taskListMetadataDao.fetchByTagOrFilter(filterId);
    if (taskListMetadata == null) {
      String defaultOrder = preferences.getStringValue(prefId);
      if (TextUtils.isEmpty(defaultOrder)) {
        defaultOrder = "[]"; // $NON-NLS-1$
      }
      defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskDao, defaultOrder);
      taskListMetadata = new TaskListMetadata();
      taskListMetadata.setFilter(filterId);
      taskListMetadata.setTaskIds(defaultOrder);
      taskListMetadataDao.createNew(taskListMetadata);
    }
    return taskListMetadata;
  }

  @Override
  public void onTaskCreated(String uuid) {
    helper.onCreateTask(uuid);
  }

  @Override
  protected void onTaskDelete(Task task) {
    super.onTaskDelete(task);
    helper.onDeleteTask(task);
  }

  @Override
  protected TaskAdapter createTaskAdapter() {
    return helper.createTaskAdapter();
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }
}
