/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.FragmentComponent;
import org.tasks.tasklist.TagListFragment;
import org.tasks.themes.Theme;

import javax.inject.Inject;

public class SubtasksTagListFragment extends TagListFragment {

    public static TaskListFragment newSubtasksTagListFragment(TagFilter filter, TagData tagData) {
        SubtasksTagListFragment fragment = new SubtasksTagListFragment();
        fragment.filter = filter;
        fragment.tagData = tagData;
        return fragment;
    }

    @Inject @ForApplication Context context;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject Theme theme;
    @Inject AstridOrderedListFragmentHelper helper;

    private int lastVisibleIndex = -1;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        helper.setTaskListFragment(this);
    }

    @Override
    protected int getListBody() {
        return R.layout.task_list_body_subtasks;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        helper.setUpUiComponents();
    }

    @Override
    public void setTaskAdapter() {
        String tdId = tagData.getUuid();
        TaskListMetadata taskListMetadata = taskListMetadataDao.fetchByTagId(tagData.getUuid(), TaskListMetadata.PROPERTIES);
        if (taskListMetadata == null && !RemoteModel.isUuidEmpty(tdId)) {
            taskListMetadata = new TaskListMetadata();
            taskListMetadata.setTagUUID(tdId);
            taskListMetadataDao.createNew(taskListMetadata);
        }
        helper.setList(taskListMetadata);
        helper.beforeSetUpTaskList(filter);

        super.setTaskAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();
        lastVisibleIndex = getListView().getFirstVisiblePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lastVisibleIndex >= 0) {
            getListView().setSelection(lastVisibleIndex);
        }
        unregisterForContextMenu(getListView());
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
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(theme.wrap(context), cursor, taskListDataProvider.getSqlQueryTemplate());
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
