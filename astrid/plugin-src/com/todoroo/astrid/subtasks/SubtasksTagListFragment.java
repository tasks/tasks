/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

public class SubtasksTagListFragment extends TagViewFragment {

    private final AstridOrderedListFragmentHelper<TagData> helper;

    public SubtasksTagListFragment() {
        super();
        helper = new AstridOrderedListFragmentHelper<TagData>(this, new SubtasksTagUpdater());
    }

    @Override
    protected void postLoadTagData() {
        helper.setList(tagData);
    }

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(
                R.layout.task_list_body_tag, root, false);

        taskListView =
            getActivity().getLayoutInflater().inflate(R.layout.task_list_body_subtasks, root, false);
        parent.addView(taskListView);

        return parent;
    }

    @Override
    protected void setUpUiComponents() {
        super.setUpUiComponents();

        helper.setUpUiComponents();
    }

    @Override
    public void setUpTaskList() {
        helper.beforeSetUpTaskList(filter);

        super.setUpTaskList();

        unregisterForContextMenu(getListView());
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
        setUpTaskList();
    }

}
