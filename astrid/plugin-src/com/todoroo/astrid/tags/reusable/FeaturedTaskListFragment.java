package com.todoroo.astrid.tags.reusable;

import android.view.View;
import android.view.View.OnClickListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;

public class FeaturedTaskListFragment extends TagViewFragment {

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new ReusableTaskAdapter(this, R.layout.reusable_task_adapter_row,
                cursor, sqlQueryTemplate, false, null);
    }

    @Override
    protected void setupQuickAddBar() {
        super.setupQuickAddBar();
        quickAddBar.setVisibility(View.GONE);
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        // Do nothing
    }

    @Override
    protected int getTaskListBodyLayout() {
        return R.layout.task_list_body_featured_list;
    }

    @Override
    protected void setUpMembersGallery() {
        View clone = getView().findViewById(R.id.clone_list);
        clone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.err.println("Cloning list");
            }
        });
    }

}
