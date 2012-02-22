package com.todoroo.astrid.subtasks;

import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;

public class SubtasksTagListFragment extends TagViewFragment {

    private final SubtasksFragmentHelper helper;

    public SubtasksTagListFragment() {
        super();
        helper = new SubtasksFragmentHelper(this);
    }

    @Override
    protected void postLoadTagData() {
        String list = "td:" + tagData.getId(); //$NON-NLS-1$
        helper.setList(list);
    }

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#getListBody(android.view.ViewGroup)
     */
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
    protected void setUpTaskList() {
        helper.beforeSetUpTaskList(filter);

        super.setUpTaskList();

        unregisterForContextMenu(getListView());
    }

    @Override
    public Property<?>[] taskProperties() {
        return helper.taskProperties();
    }


    @Override
    protected boolean isDraggable() {
        return true;
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return helper.createTaskAdapter(cursor, sqlQueryTemplate);
    }

}
