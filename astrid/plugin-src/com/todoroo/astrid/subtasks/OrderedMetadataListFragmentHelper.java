/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.Node;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.OrderedListNodeVisitor;
import com.todoroo.astrid.ui.DraggableListView;
import com.todoroo.astrid.utility.AstridPreferences;

public class OrderedMetadataListFragmentHelper<LIST> implements OrderedListFragmentHelperInterface<LIST> {

    private final DisplayMetrics metrics = new DisplayMetrics();
    private final OrderedMetadataListUpdater<LIST> updater;
    private final TaskListFragment fragment;

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;

    private DraggableTaskAdapter taskAdapter;

    private LIST list;

    public OrderedMetadataListFragmentHelper(TaskListFragment fragment, OrderedMetadataListUpdater<LIST> updater) {
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

        if(Preferences.getInt(AstridPreferences.P_SUBTASKS_HELP, 0) == 0)
            showSubtasksHelp();
    }

    @SuppressWarnings("nls")
    private void showSubtasksHelp() {
        String body = String.format("<h3>%s</h3><img src='%s'>" +
                "<br>%s<br><br><br><img src='%s'><br>%s",
                getActivity().getString(R.string.subtasks_help_1),
                "subtasks_vertical.png",
                getActivity().getString(R.string.subtasks_help_2),
                "subtasks_horizontal.png",
                getActivity().getString(R.string.subtasks_help_3));

        String color = ThemeService.getDialogTextColorString();
        String html = String.format("<html><body style='text-align:center;color:%s'>%s</body></html>",
                color, body);

        DialogUtilities.htmlDialog(getActivity(), html, R.string.subtasks_help_title);
        Preferences.setInt(AstridPreferences.P_SUBTASKS_HELP, 1);
    }

    public void beforeSetUpTaskList(Filter filter) {
        updater.initialize(list, filter);
    }

    public Property<?>[] taskProperties() {
        Property<?>[] baseProperties = TaskAdapter.PROPERTIES;
        if (Preferences.getIntegerFromString(R.string.p_taskRowStyle_v2, 0) == 2)
            baseProperties = TaskAdapter.BASIC_PROPERTIES;

        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(baseProperties));
        properties.add(updater.indentProperty());
        properties.add(updater.orderProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }


    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            if (targetTaskId <= 0) return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            long destinationTaskId = taskAdapter.getItemId(to);

            try {
                if(to >= getListView().getCount())
                    updater.moveTo(getFilter(), list, targetTaskId, -1);
                else
                    updater.moveTo(getFilter(), list, targetTaskId, destinationTaskId);
            } catch (Exception e) {
                Log.e("drag", "Drag Error", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            fragment.loadTaskListContent(true);
        }
    };

    private final SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            indent(which, 1);
        }

        @Override
        public void swipeLeft(int which) {
            indent(which, -1);
        }

        protected void indent(int which, int delta) {
            long targetTaskId = taskAdapter.getItemId(which);
            if (targetTaskId <= 0) return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            try {
                updater.indent(getFilter(), list, targetTaskId, delta);
            } catch (Exception e) {
                Log.e("drag", "Indent Error", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            fragment.loadTaskListContent(true);
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

        taskAdapter = new DraggableTaskAdapter(fragment, TaskListFragment.getTaskRowResource(),
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
        final int parentIndent = item.getValue(updater.indentProperty());
        updater.applyToChildren(getFilter(), list, itemId, new OrderedListNodeVisitor() {
            @Override
            public void visitNode(Node node) {
                Task childTask = taskService.fetchById(node.taskId, Task.RECURRENCE);
                if(!TextUtils.isEmpty(childTask.getValue(Task.RECURRENCE))) {
                    Metadata metadata = updater.getTaskMetadata(list, node.taskId);
                    metadata.setValue(updater.indentProperty(), parentIndent);
                    metadataService.save(metadata);
                }

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

    @Override
    public void onCreateTask(Task task) {
        //
    }

    public void onDeleteTask(Task task) {
        updater.onDeleteTask(getFilter(), list, task.getId());
        taskAdapter.notifyDataSetInvalidated();
    }

}
