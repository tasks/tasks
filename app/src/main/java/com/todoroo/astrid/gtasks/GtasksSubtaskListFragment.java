/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks;

import android.app.Activity;
import android.os.Bundle;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;
import org.tasks.tasklist.GtasksListFragment;

public class GtasksSubtaskListFragment extends GtasksListFragment {

  @Inject OrderedMetadataListFragmentHelper helper;

  public static TaskListFragment newGtasksSubtaskListFragment(
      GtasksFilter filter, GoogleTaskList list) {
    GtasksSubtaskListFragment fragment = new GtasksSubtaskListFragment();
    fragment.filter = filter;
    fragment.list = list;
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    helper.setList(list);
  }

  @Override
  protected void onTaskDelete(Task task) {
    super.onTaskDelete(task);
    helper.onDeleteTask(task);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    helper.setTaskListFragment(this);
  }

  @Override
  public void setTaskAdapter() {
    helper.setList(list);
    helper.beforeSetUpTaskList(filter);

    super.setTaskAdapter();
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
