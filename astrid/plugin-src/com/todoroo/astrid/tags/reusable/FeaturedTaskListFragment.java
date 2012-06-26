package com.todoroo.astrid.tags.reusable;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.utility.Flags;

public class FeaturedTaskListFragment extends TagViewFragment {

    @Autowired private TagDataService tagDataService;

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
        // Repurposed this method to set up listener for clone list button
        View clone = getView().findViewById(R.id.clone_list);
        if (taskAdapter == null || taskAdapter.getCount() == 0) {
            clone.setOnClickListener(null);
        } else {
            clone.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Clone list
                    String localName = tagData.getValue(TagData.NAME) + " (Copy)";
                    long remoteId = 0;
                    TodorooCursor<TagData> existing = tagDataService.query(Query.select(TagData.REMOTE_ID)
                            .where(TagData.NAME.eqCaseInsensitive(localName)));
                    try {
                        if (existing.getCount() > 0) {
                            existing.moveToFirst();
                            TagData match = new TagData(existing);
                            remoteId = match.getValue(TagData.REMOTE_ID);
                        }

                    } finally {
                        existing.close();
                    }

                    TodorooCursor<Task> tasks = taskService.fetchFiltered(taskAdapter.getQuery(), null, Task.PROPERTIES);
                    try {
                        Task t = new Task();
                        for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                            t.readFromCursor(tasks);
                            taskService.cloneReusableTask(t,
                                    localName, remoteId);
                        }
                        Flags.set(Flags.REFRESH);
                        DialogUtilities.okDialog(getActivity(), "Success!", null);
                    } finally {
                        tasks.close();
                    }
                }
            });
        }
    }

    @Override
    protected void refresh() {
        loadTaskListContent(true);
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
        setUpMembersGallery();
    }

}
