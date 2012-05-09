package com.todoroo.astrid.people;

import android.view.View;
import android.view.ViewGroup;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.User;

public class PersonViewFragment extends TaskListFragment {

    public static final String EXTRA_USER_ID_LOCAL = "user_local_id"; //$NON-NLS-1$

    @Autowired UserDao userDao;

    private User user;

    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.task_list_body_person, root, false);

        View taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

    @Override
    protected void initializeData() {
        super.initializeData();
        if (extras.containsKey(EXTRA_USER_ID_LOCAL))
            user = userDao.fetch(extras.getLong(EXTRA_USER_ID_LOCAL), User.PROPERTIES);
    }

    @Override
    protected void setupQuickAddBar() {
        super.setupQuickAddBar();
        quickAddBar.setUsePeopleControl(false);
        quickAddBar.getQuickAddBox().setHint(getString(R.string.TLA_quick_add_hint_assign, user.getDisplayName()));
    }

}
