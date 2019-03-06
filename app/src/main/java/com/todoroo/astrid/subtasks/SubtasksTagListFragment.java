/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.subtasks;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.AstridTaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.data.TagData;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.FragmentComponent;
import org.tasks.tasklist.TagListFragment;

public class SubtasksTagListFragment extends TagListFragment {

  @Inject TaskListMetadataDao taskListMetadataDao;
  @Inject SubtasksFilterUpdater updater;
  @Inject TaskDao taskDao;

  public static TaskListFragment newSubtasksTagListFragment(TagFilter filter, TagData tagData) {
    SubtasksTagListFragment fragment = new SubtasksTagListFragment();
    fragment.filter = filter;
    fragment.tagData = tagData;
    return fragment;
  }

  @Override
  protected TaskAdapter createTaskAdapter() {
    TaskListMetadata list = initializeTaskListMetadata();
    updater.initialize(list, filter);
    return new AstridTaskAdapter(list, filter, updater, taskDao);
  }

  private TaskListMetadata initializeTaskListMetadata() {
    String tdId = tagData.getRemoteId();
    TaskListMetadata taskListMetadata =
        taskListMetadataDao.fetchByTagOrFilter(tagData.getRemoteId());
    if (taskListMetadata == null && !Task.isUuidEmpty(tdId)) {
      taskListMetadata = new TaskListMetadata();
      taskListMetadata.setTagUuid(tdId);
      taskListMetadataDao.createNew(taskListMetadata);
    }
    return taskListMetadata;
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }
}
