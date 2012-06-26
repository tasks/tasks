package com.todoroo.astrid.tags.reusable;

import java.util.concurrent.atomic.AtomicReference;

import android.database.Cursor;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;

public class ReusableTaskAdapter extends TaskAdapter {

    public ReusableTaskAdapter(TaskListFragment fragment, int resource,
            Cursor c, AtomicReference<String> query, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(fragment, resource, c, query, autoRequery, onCompletedTaskListener);
    }

}
