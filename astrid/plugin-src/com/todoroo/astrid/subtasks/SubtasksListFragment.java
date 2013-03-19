/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

/**
 * Fragment for subtasks
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SubtasksListFragment extends TaskListFragment {

    protected OrderedListFragmentHelperInterface<?> helper;

    private int lastVisibleIndex = -1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        helper = createFragmentHelper();
        super.onActivityCreated(savedInstanceState);
    }

    protected OrderedListFragmentHelperInterface<?> createFragmentHelper() {
        AstridOrderedListFragmentHelper<TaskListMetadata> olfh =
            new AstridOrderedListFragmentHelper<TaskListMetadata>(this, new SubtasksFilterUpdater());
        return olfh;
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
    protected boolean isDraggable() {
        return true;
    }

    @Override
    public void onTaskCreated(Task task) {
        super.onTaskCreated(task);
        helper.onCreateTask(task);
    }

    @Override
    protected void onTaskDelete(Task task) {
        super.onTaskDelete(task);
        helper.onDeleteTask(task);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(cursor, sqlQueryTemplate);
    }

    @Override
    protected void refresh() {
        initializeTaskListMetadata();
        setUpTaskList();
    }

}
