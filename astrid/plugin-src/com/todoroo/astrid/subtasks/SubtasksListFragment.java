package com.todoroo.astrid.subtasks;

import java.util.concurrent.atomic.AtomicReference;

import android.database.Cursor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.GrabberClickListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;

public class SubtasksListFragment extends TaskListFragment {

    private final DisplayMetrics metrics = new DisplayMetrics();

    public TouchListView getTouchListView() {
        TouchListView tlv = (TouchListView) getListView();
        return tlv;
    }

    @Override
    protected View getListBody(ViewGroup root) {
        return getActivity().getLayoutInflater().inflate(R.layout.task_list_body_subtasks, root, false);
    }

    @Override
    protected void setUpUiComponents() {
        super.setUpUiComponents();

        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.asThemeTextColor, tv, false);
        getTouchListView().setDragndropBackgroundColor(tv.data);
        getTouchListView().setDropListener(dropListener);
        getTouchListView().setClickListener(rowClickListener);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            System.err.println("DROPPED TO " + to);
        }
    };

    private final GrabberClickListener rowClickListener = new GrabberClickListener() {
        @Override
        public void onLongClick(final View v) {
            System.err.println(v);
            getListView().showContextMenuForChild(v);
        }

        @Override
        public void onClick(View v) {
            System.err.println(v);
            ((DraggableTaskAdapter) taskAdapter).getListener().onClick(v);
        }
    };

    // --- task adapter

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new DraggableTaskAdapter(this, R.layout.task_adapter_row,
                cursor, sqlQueryTemplate, false, null);
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(TaskListFragment activity, int resource,
                Cursor c, AtomicReference<String> query, boolean autoRequery,
                OnCompletedTaskListener onCompletedTaskListener) {
            super(activity, resource, c, query, autoRequery,
                    onCompletedTaskListener);

            applyListeners = APPLY_LISTENERS_NONE;
        }

        @Override
        protected ViewHolder getTagFromCheckBox(View v) {
            return (ViewHolder)((View)v.getParent()).getTag();
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            view.getLayoutParams().height = Math.round(45 * metrics.density);
        }

        @Override
        protected void addListeners(View container) {
            super.addListeners(container);
        }

        public TaskRowListener getListener() {
            return listener;
        }

    }

}
