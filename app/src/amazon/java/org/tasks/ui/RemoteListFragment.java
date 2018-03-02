package org.tasks.ui;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.FragmentComponent;

public class RemoteListFragment extends TaskEditControlFragment {
    public static final int TAG = R.string.TEA_ctrl_google_task_list;

    @Override
    protected int getLayout() {
        return 0;
    }

    @Override
    protected int getIcon() {
        return 0;
    }

    @Override
    public int controlId() {
        return 0;
    }

    @Override
    public void apply(Task task) {

    }

    @Override
    protected void inject(FragmentComponent component) {

    }

    public void setList(Filter filter) {

    }
}
