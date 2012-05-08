package com.todoroo.astrid.people;

import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.TaskListFragment;

public class PersonViewFragment extends TaskListFragment {

    public static final String EXTRA_USER_ID_LOCAL = "user_local_id"; //$NON-NLS-1$

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_person, root, false);

        View taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

}
