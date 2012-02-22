package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import android.database.Cursor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.GrabberClickListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.DraggableListView;

public class SubtasksListFragment extends TaskListFragment {

    private final DisplayMetrics metrics = new DisplayMetrics();

    private final SubtasksUpdater updater = new SubtasksUpdater();

    public DraggableListView getTouchListView() {
        DraggableListView tlv = (DraggableListView) getListView();
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
        getTouchListView().setSwipeListener(swipeListener);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    protected void setUpTaskList() {

        updater.applySubtasksToFilter(filter, null);

        super.setUpTaskList();

        unregisterForContextMenu(getListView());
    }

    @Override
    public Property<?>[] taskProperties() {
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(TaskAdapter.PROPERTIES));
        properties.add(SubtasksMetadata.INDENT);
        return properties.toArray(new Property<?>[properties.size()]);
    }

    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);

            System.err.println("MOVE " + from + " TO " + to);

            System.err.println("BEFORE");
            updater.debugPrint(filter, SubtasksMetadata.LIST_ACTIVE_TASKS);

            if(to == getListView().getCount() - 1)
                updater.moveTo(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, -1);
            else
                updater.moveTo(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, destinationTaskId);

            System.err.println("AFTER");
            updater.debugPrint(filter, SubtasksMetadata.LIST_ACTIVE_TASKS);

            loadTaskListContent(true);
        }
    };

    private final SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            System.err.println("SWIPE RIGHT " + targetTaskId);
            updater.indent(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, 1);
            updater.debugPrint(filter, SubtasksMetadata.LIST_ACTIVE_TASKS);
            loadTaskListContent(true);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            System.err.println("SWIPE LEFT " + targetTaskId);
            updater.indent(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, -1);
            updater.debugPrint(filter, SubtasksMetadata.LIST_ACTIVE_TASKS);
            loadTaskListContent(true);
        }
    };

    private final GrabberClickListener rowClickListener = new GrabberClickListener() {
        @Override
        public void onLongClick(final View v) {
            registerForContextMenu(getListView());
            getListView().showContextMenuForChild(v);
            unregisterForContextMenu(getListView());
        }

        @Override
        public void onClick(View v) {
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
            ViewHolder vh = (ViewHolder) view.getTag();
            int indent = vh.task.getValue(SubtasksMetadata.INDENT);
            vh.rowBody.setPadding(Math.round(indent * 20 * metrics.density), 0, 0, 0);
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
