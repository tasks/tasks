package com.todoroo.astrid.tags.reusable;

import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Flags;

public class ReusableTaskAdapter extends TaskAdapter {

    public ReusableTaskAdapter(TaskListFragment fragment, int resource,
            Cursor c, AtomicReference<String> query, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(fragment, resource, c, query, autoRequery, onCompletedTaskListener);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        ReusableTaskViewHolder viewHolder = new ReusableTaskViewHolder();
        viewHolder.task = new Task();
        viewHolder.clone = (ImageView) view.findViewById(R.id.clone_task);
        viewHolder.title = (TextView) view.findViewById(R.id.title);

        boolean showFullTaskTitle = Preferences.getBoolean(R.string.p_fullTaskTitle, false);
        if (showFullTaskTitle) {
            viewHolder.title.setMaxLines(Integer.MAX_VALUE);
        }

        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++)
            view.getChildAt(i).setTag(viewHolder);

        viewHolder.clone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ReusableTaskViewHolder holder = (ReusableTaskViewHolder) v.getTag();
                taskService.cloneReusableTask(holder.task, null, RemoteModel.NO_UUID);
                Toast.makeText(fragment.getActivity(), R.string.actfm_feat_list_task_clone_success, Toast.LENGTH_LONG).show();
                Flags.set(Flags.REFRESH);
            }
        });

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
        ReusableTaskViewHolder viewHolder = (ReusableTaskViewHolder) view.getTag();

        Task task = viewHolder.task;
        task.clear();
        task.readFromCursor(cursor);

        viewHolder.title.setText(task.getValue(Task.TITLE));
        view.setMinimumHeight(minRowHeight);
    }

    public static class ReusableTaskViewHolder {
        public Task task;
        public ImageView clone;
        public TextView title;
    }

}
