package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ListView;

import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.GrabberClickListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedListUpdater.Node;
import com.todoroo.astrid.subtasks.OrderedListUpdater.OrderedListNodeVisitor;
import com.todoroo.astrid.ui.DraggableListView;

public class OrderedListFragmentHelper<LIST> {

    private final DisplayMetrics metrics = new DisplayMetrics();
    private final OrderedListUpdater<LIST> updater;
    private final TaskListFragment fragment;

    @Autowired TaskService taskService;

    private DraggableTaskAdapter taskAdapter;

    private LIST list;

    public OrderedListFragmentHelper(TaskListFragment fragment, OrderedListUpdater<LIST> updater) {
        DependencyInjectionService.getInstance().inject(this);
        this.fragment = fragment;
        this.updater = updater;
    }

    // --- ui component setup

    private Activity getActivity() {
        return fragment.getActivity();
    }

    private ListView getListView() {
        return fragment.getListView();
    }

    private Filter getFilter() {
        return fragment.getFilter();
    }

    public DraggableListView getTouchListView() {
        DraggableListView tlv = (DraggableListView) fragment.getListView();
        return tlv;
    }

    public void setUpUiComponents() {
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.asThemeTextColor, tv, false);
        getTouchListView().setDragndropBackgroundColor(tv.data);
        getTouchListView().setDropListener(dropListener);
        getTouchListView().setClickListener(rowClickListener);
        getTouchListView().setSwipeListener(swipeListener);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    public void beforeSetUpTaskList(Filter filter) {
        updater.initialize(list, filter);
    }

    public Property<?>[] taskProperties() {
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(TaskAdapter.PROPERTIES));
        properties.add(updater.indentProperty());
        properties.add(updater.orderProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }


    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);

            if(to >= getListView().getCount())
                updater.moveTo(getFilter(), list, targetTaskId, -1);
            else
                updater.moveTo(getFilter(), list, targetTaskId, destinationTaskId);

            fragment.loadTaskListContent(true);
            onMetadataChanged(targetTaskId);
        }
    };

    private final SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            updater.indent(getFilter(), list, targetTaskId, 1);
            fragment.loadTaskListContent(true);
            onMetadataChanged(targetTaskId);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            updater.indent(getFilter(), list, targetTaskId, -1);
            fragment.loadTaskListContent(true);
            onMetadataChanged(targetTaskId);
        }
    };

    private final GrabberClickListener rowClickListener = new GrabberClickListener() {
        @Override
        public void onLongClick(final View v) {
            if(v == null)
                return;

            fragment.registerForContextMenu(getListView());
            getListView().showContextMenuForChild(v);
            fragment.unregisterForContextMenu(getListView());
        }

        @Override
        public void onClick(View v) {
            if(v == null)
                return;
            ((DraggableTaskAdapter) taskAdapter).getListener().onClick(v);
        }
    };

    public TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor,
            AtomicReference<String> sqlQueryTemplate) {
        taskAdapter = new DraggableTaskAdapter(fragment, R.layout.task_adapter_row,
                cursor, sqlQueryTemplate, false, null);

        taskAdapter.addOnCompletedTaskListener(new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                setCompletedForItemAndSubtasks(item, newState);
            }
        });

        return taskAdapter;
    }

    /**
     * @param targetTaskId
     */
    protected void onMetadataChanged(long targetTaskId) {
        // hook
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
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            view.getLayoutParams().height = Math.round(45 * metrics.density);

            ViewHolder vh = (ViewHolder) view.getTag();

            MarginLayoutParams rowParams = (MarginLayoutParams) vh.rowBody.getLayoutParams();
            rowParams.topMargin = rowParams.bottomMargin = 0;

            ViewGroup.LayoutParams pictureParams = vh.picture.getLayoutParams();
            pictureParams.width = pictureParams.height = Math.round(38 * metrics.density);

            pictureParams = vh.pictureBorder.getLayoutParams();
            pictureParams.width = pictureParams.height = Math.round(38 * metrics.density);

            return view;
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            ViewHolder vh = (ViewHolder) view.getTag();
            int indent = vh.task.getValue(updater.indentProperty());
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

    private final Map<Long, ArrayList<Long>> chainedCompletions =
        Collections.synchronizedMap(new HashMap<Long, ArrayList<Long>>());

    private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
        final long itemId = item.getId();

        final Task model = new Task();
        final long completionDate = completedState ? DateUtilities.now() : 0;

        if(completedState == false) {
            ArrayList<Long> chained = chainedCompletions.get(itemId);
            if(chained != null) {
                for(Long taskId : chained) {
                    model.setId(taskId);
                    model.setValue(Task.COMPLETION_DATE, completionDate);
                    taskService.save(model);
                    model.clear();

                    taskAdapter.getCompletedItems().put(taskId, false);
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<Long> chained = new ArrayList<Long>();
        updater.applyToChildren(getFilter(), list, itemId, new OrderedListNodeVisitor() {
            @Override
            public void visitNode(Node node) {
                model.setId(node.taskId);
                model.setValue(Task.COMPLETION_DATE, completionDate);
                taskService.save(model);
                model.clear();

                taskAdapter.getCompletedItems().put(node.taskId, true);
                chained.add(node.taskId);
            }
        });

        if(chained.size() > 0) {
            chainedCompletions.put(itemId, chained);
            taskAdapter.notifyDataSetInvalidated();
        }
    }

    public void setList(LIST list) {
        this.list = list;
    }

    public void onDeleteTask(Task task) {
        updater.onDeleteTask(getFilter(), list, task.getId());
        taskAdapter.notifyDataSetInvalidated();
    }

}
