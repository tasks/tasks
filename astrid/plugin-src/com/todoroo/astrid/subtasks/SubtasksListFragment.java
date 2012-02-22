package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.OrderedListUpdater.OrderedListIterator;
import com.todoroo.astrid.ui.DraggableListView;

/**
 * Fragment for subtasks
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
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
        updater.sanitizeTaskList(filter, SubtasksMetadata.LIST_ACTIVE_TASKS);

        super.setUpTaskList();

        unregisterForContextMenu(getListView());

    }

    @Override
    public Property<?>[] taskProperties() {
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(TaskAdapter.PROPERTIES));
        properties.add(SubtasksMetadata.INDENT);
        properties.add(SubtasksMetadata.ORDER);
        return properties.toArray(new Property<?>[properties.size()]);
    }

    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);

            if(to == getListView().getCount() - 1)
                updater.moveTo(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, -1);
            else
                updater.moveTo(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, destinationTaskId);

            loadTaskListContent(true);
        }
    };

    private final SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            updater.indent(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, 1);
            loadTaskListContent(true);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            updater.indent(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, targetTaskId, -1);
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
        taskAdapter = new DraggableTaskAdapter(this, R.layout.task_adapter_row,
                cursor, sqlQueryTemplate, false, null);

        taskAdapter.addOnCompletedTaskListener(new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                setCompletedForItemAndSubtasks(item, newState);
            }
        });

        return taskAdapter;
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

    @Override
    protected boolean isDraggable() {
        return true;
    }



    private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
        final Map<Long, ArrayList<Long>> chainedCompletions =
            Collections.synchronizedMap(new HashMap<Long, ArrayList<Long>>());

        final long itemId = item.getId();

        final Task task = new Task();
        task.setValue(Task.COMPLETION_DATE, completedState ? DateUtilities.now() : 0);

        if(completedState == false) {
            ArrayList<Long> chained = chainedCompletions.get(itemId);
            if(chained != null) {
                for(Long taskId : chained) {
                    taskAdapter.getCompletedItems().put(taskId, false);
                    task.setId(taskId);
                    taskService.save(task);
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        new Thread() {
            @Override
            public void run() {
                final AtomicInteger startIndent = new AtomicInteger(
                        item.getValue(SubtasksMetadata.INDENT));
                final AtomicLong startOrder= new AtomicLong(
                        item.getValue(SubtasksMetadata.ORDER));
                final AtomicBoolean finished = new AtomicBoolean(false);
                final ArrayList<Long> chained = new ArrayList<Long>();
                chainedCompletions.put(itemId, chained);

                updater.iterateThroughList(filter, SubtasksMetadata.LIST_ACTIVE_TASKS, new OrderedListIterator() {
                    @Override
                    public void processTask(long taskId, Metadata metadata) {
                        if(finished.get())
                            return;

                        long order = metadata.containsNonNullValue(SubtasksMetadata.ORDER) ?
                                metadata.getValue(SubtasksMetadata.ORDER) : 0;
                        int indent = metadata.containsNonNullValue(SubtasksMetadata.INDENT) ?
                                metadata.getValue(SubtasksMetadata.INDENT) : 0;

                        if(order < startOrder.get())
                            return;
                        else if(indent == startIndent.get()) {
                            finished.set(true);
                            return;
                        }

                        taskAdapter.getCompletedItems().put(taskId, true);
                        task.setId(taskId);
                        taskService.save(task);
                        chained.add(taskId);
                    }
                });
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        taskAdapter.notifyDataSetInvalidated();
                    }
                });
            }
        }.start();
    }

}
