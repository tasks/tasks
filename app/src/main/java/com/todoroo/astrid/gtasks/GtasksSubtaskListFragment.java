/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.GoogleTaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import javax.inject.Inject;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;
import org.tasks.tasklist.GtasksListFragment;

public class GtasksSubtaskListFragment extends GtasksListFragment {

  @Inject TaskDao taskDao;
  @Inject GtasksTaskListUpdater updater;
  @Inject GoogleTaskDao googleTaskDao;

  public static TaskListFragment newGtasksSubtaskListFragment(
      GtasksFilter filter, GoogleTaskList list) {
    GtasksSubtaskListFragment fragment = new GtasksSubtaskListFragment();
    fragment.filter = filter;
    fragment.list = list;
    return fragment;
  }

  @Override
  protected TaskAdapter createTaskAdapter() {
    updater.initialize(filter);
    return new GoogleTaskAdapter(list, updater, taskDao, googleTaskDao);
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }
}
