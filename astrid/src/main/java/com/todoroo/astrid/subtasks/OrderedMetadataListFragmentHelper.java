/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;

import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.GrabberClickListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.Node;
import com.todoroo.astrid.subtasks.OrderedMetadataListUpdater.OrderedListNodeVisitor;
import com.todoroo.astrid.ui.DraggableListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class OrderedMetadataListFragmentHelper<LIST> implements OrderedListFragmentHelperInterface<LIST> {

    private static final Logger log = LoggerFactory.getLogger(OrderedMetadataListFragmentHelper.class);

    private final DisplayMetrics metrics = new DisplayMetrics();
    private final OrderedMetadataListUpdater<LIST> updater;
    private final TaskListFragment fragment;

    private final ActivityPreferences preferences;
    private final TaskAttachmentDao taskAttachmentDao;
    private final TaskService taskService;
    private final MetadataService metadataService;

    private DraggableTaskAdapter taskAdapter;

    private LIST list;

    public OrderedMetadataListFragmentHelper(ActivityPreferences preferences, TaskAttachmentDao taskAttachmentDao, TaskService taskService, MetadataService metadataService, TaskListFragment fragment, OrderedMetadataListUpdater<LIST> updater) {
        this.preferences = preferences;
        this.taskAttachmentDao = taskAttachmentDao;
        this.taskService = taskService;
        this.metadataService = metadataService;
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

    public DraggableListView getTouchListView() {
        return (DraggableListView) fragment.getListView();
    }

    @Override
    public void setUpUiComponents() {
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.asThemeTextColor, tv, false);
        getTouchListView().setDragndropBackgroundColor(tv.data);
        getTouchListView().setDropListener(dropListener);
        getTouchListView().setClickListener(rowClickListener);
        getTouchListView().setSwipeListener(swipeListener);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    public void beforeSetUpTaskList(Filter filter) {
    }

    @Override
    public Property<?>[] taskProperties() {
        Property<?>[] baseProperties = TaskAdapter.PROPERTIES;
        ArrayList<Property<?>> properties = new ArrayList<>(Arrays.asList(baseProperties));
        properties.add(updater.indentProperty());
        properties.add(updater.orderProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }


    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            if (targetTaskId <= 0) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            long destinationTaskId = taskAdapter.getItemId(to);

            try {
                if(to >= getListView().getCount()) {
                    updater.moveTo(list, targetTaskId, -1);
                } else {
                    updater.moveTo(list, targetTaskId, destinationTaskId);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            fragment.loadTaskListContent();
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
            if (targetTaskId <= 0) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            try {
                updater.indent(list, targetTaskId, delta);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            fragment.loadTaskListContent();
        }
    };

    private final GrabberClickListener rowClickListener = new GrabberClickListener() {
        @Override
        public void onLongClick(final View v) {
            if(v == null) {
                return;
            }

            fragment.registerForContextMenu(getListView());
            getListView().showContextMenuForChild(v);
            fragment.unregisterForContextMenu(getListView());
        }

        @Override
        public void onClick(View v) {
            if(v == null) {
                return;
            }
            taskAdapter.onClick(v);
        }
    };

    @Override
    public TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor,
            AtomicReference<String> sqlQueryTemplate) {

        taskAdapter = new DraggableTaskAdapter(preferences, fragment, TaskListFragment.getTaskRowResource(),
                cursor, sqlQueryTemplate);

        taskAdapter.addOnCompletedTaskListener(new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                setCompletedForItemAndSubtasks(item, newState);
            }
        });

        return taskAdapter;
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(ActivityPreferences preferences, TaskListFragment activity, int resource,
                Cursor c, AtomicReference<String> query) {
            super(preferences, taskAttachmentDao, taskService, activity, resource, c, query, null);
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            ViewHolder vh = (ViewHolder) view.getTag();
            int indent = vh.task.getValue(updater.indentProperty());
            vh.rowBody.setPadding(Math.round(indent * 20 * metrics.density), 0, 0, 0);
        }
    }

    private final Map<Long, ArrayList<Long>> chainedCompletions =
        Collections.synchronizedMap(new HashMap<Long, ArrayList<Long>>());

    private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
        final long itemId = item.getId();

        final Task model = new Task();
        final long completionDate = completedState ? DateUtilities.now() : 0;

        if(!completedState) {
            ArrayList<Long> chained = chainedCompletions.get(itemId);
            if(chained != null) {
                for(Long taskId : chained) {
                    model.setId(taskId);
                    model.setCompletionDate(completionDate);
                    taskService.save(model);
                    model.clear();

                    taskAdapter.getCompletedItems().put(taskId, false);
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<Long> chained = new ArrayList<>();
        final int parentIndent = item.getValue(updater.indentProperty());
        updater.applyToChildren(list, itemId, new OrderedListNodeVisitor() {
            @Override
            public void visitNode(Node node) {
                Task childTask = taskService.fetchById(node.taskId, Task.RECURRENCE);
                if(!TextUtils.isEmpty(childTask.getRecurrence())) {
                    Metadata metadata = updater.getTaskMetadata(node.taskId);
                    metadata.setValue(updater.indentProperty(), parentIndent);
                    metadataService.save(metadata);
                }

                model.setId(node.taskId);
                model.setCompletionDate(completionDate);
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

    @Override
    public void onDeleteTask(Task task) {
        updater.onDeleteTask(list, task.getId());
        taskAdapter.notifyDataSetInvalidated();
    }

}
