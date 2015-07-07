/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

/**
 * Fragment for subtasks
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SubtasksListFragment extends TaskListFragment {

    protected OrderedListFragmentHelperInterface<?> helper;

    private int lastVisibleIndex = -1;

    @Inject TaskService taskService;
    @Inject SubtasksFilterUpdater subtasksFilterUpdater;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject ActivityPreferences preferences;
    @Inject @ForApplication Context context;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        helper = createFragmentHelper();
    }

    protected OrderedListFragmentHelperInterface<?> createFragmentHelper() {
        return new AstridOrderedListFragmentHelper<>(preferences, taskAttachmentDao, taskService, this, subtasksFilterUpdater, dialogBuilder);
    }

    @Override
    protected View getListBody(ViewGroup root) {
        return getActivity().getLayoutInflater().inflate(R.layout.task_list_body_subtasks, root, false);
    }

    @Override
    protected void setUpUiComponents() {
        super.setUpUiComponents();

        helper.setUpUiComponents();
    }

    @Override
    public void setUpTaskList() {
        if (helper instanceof AstridOrderedListFragmentHelper) {
            ((AstridOrderedListFragmentHelper<TaskListMetadata>) helper).setList(taskListMetadata);
        }
        helper.beforeSetUpTaskList(filter);

        super.setUpTaskList();

        unregisterForContextMenu(getListView());
    }

    @Override
    public void onPause() {
        super.onPause();
        lastVisibleIndex = getListView().getFirstVisiblePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lastVisibleIndex >=0) {
            getListView().setSelection(lastVisibleIndex);
        }
    }

    @Override
    public void onTaskCreated(long id, String uuid) {
        helper.onCreateTask(id, uuid);
    }

    @Override
    protected void onTaskDelete(Task task) {
        super.onTaskDelete(task);
        helper.onDeleteTask(task);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(context, cursor, sqlQueryTemplate);
    }

    @Override
    protected void refresh() {
        super.refresh();
        initializeTaskListMetadata();
    }
}
