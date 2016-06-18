package org.tasks.ui;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;

import org.tasks.R;
import org.tasks.injection.FragmentComponent;

public class GoogleTaskListFragment extends TaskEditControlFragment {
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
    public void initialize(boolean isNewTask, Task task) {

    }

    @Override
    public void apply(Task task) {

    }

    @Override
    protected void inject(FragmentComponent component) {

    }

    public void setList(GtasksList list) {

    }
}
