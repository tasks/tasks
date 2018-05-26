/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.tasks.data.TagData;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.FragmentComponent;
import org.tasks.tasklist.TagListFragment;

public class SubtasksTagListFragment extends TagListFragment {

  @Inject TaskListMetadataDao taskListMetadataDao;
  @Inject AstridOrderedListFragmentHelper helper;

  public static TaskListFragment newSubtasksTagListFragment(TagFilter filter, TagData tagData) {
    SubtasksTagListFragment fragment = new SubtasksTagListFragment();
    fragment.filter = filter;
    fragment.tagData = tagData;
    return fragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    helper.setTaskListFragment(this);
  }

  @Override
  public void setTaskAdapter() {
    String tdId = tagData.getRemoteId();
    TaskListMetadata taskListMetadata =
        taskListMetadataDao.fetchByTagOrFilter(tagData.getRemoteId());
    if (taskListMetadata == null && !Task.isUuidEmpty(tdId)) {
      taskListMetadata = new TaskListMetadata();
      taskListMetadata.setTagUuid(tdId);
      taskListMetadataDao.createNew(taskListMetadata);
    }
    helper.setList(taskListMetadata);
    helper.beforeSetUpTaskList(filter);

    super.setTaskAdapter();
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
