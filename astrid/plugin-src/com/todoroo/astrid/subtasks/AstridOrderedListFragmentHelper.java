package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
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
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.DraggableListView;
import com.todoroo.astrid.utility.AstridPreferences;

public class AstridOrderedListFragmentHelper<LIST> implements OrderedListFragmentHelperInterface<LIST> {


    private final DisplayMetrics metrics = new DisplayMetrics();
    private final AstridOrderedListUpdater<LIST> updater;
    private final TaskListFragment fragment;

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;

    private DraggableTaskAdapter taskAdapter;

    private LIST list;

    public AstridOrderedListFragmentHelper(TaskListFragment fragment, AstridOrderedListUpdater<LIST> updater) {
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

    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            String targetTaskId = taskAdapter.getItemUuid(from);
            if (!RemoteModel.isValidUuid(targetTaskId)) return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            String destinationTaskId = taskAdapter.getItemUuid(to);

            try {
                if(to >= getListView().getCount())
                    updater.moveTo(list, getFilter(), targetTaskId, "-1"); //$NON-NLS-1$
                else
                    updater.moveTo(list, getFilter(), targetTaskId, destinationTaskId);
            } catch (Exception e) {
                Log.e("drag", "Drag Error", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            fragment.reconstructCursor();
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
            String targetTaskId = taskAdapter.getItemUuid(which);
            if (!RemoteModel.isValidUuid(targetTaskId)) return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            try {
                updater.indent(list, getFilter(), targetTaskId, delta);
            } catch (Exception e) {
                Log.e("drag", "Indent Error", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            fragment.reconstructCursor();
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

        getTouchListView().setItemHightNormal(taskAdapter.computeFullRowHeight());

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
            int indent = updater.getIndentForTask(vh.task.getUuid());
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

    private final Map<String, ArrayList<String>> chainedCompletions =
        Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());

    private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
        final String itemId = item.getUuid();

        final Task model = new Task();
        final long completionDate = completedState ? DateUtilities.now() : 0;

        if(completedState == false) {
            ArrayList<String> chained = chainedCompletions.get(itemId);
            if(chained != null) {
                for(String taskId : chained) {
                    model.setValue(Task.COMPLETION_DATE, completionDate);
                    taskService.update(Task.UUID.eq(taskId), model);
                    model.clear();

                    taskAdapter.getCompletedItems().put(taskId, false);
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<String> chained = new ArrayList<String>();
        updater.applyToDescendants(itemId, new AstridOrderedListUpdater.OrderedListNodeVisitor() {
            @Override
            public void visitNode(AstridOrderedListUpdater.Node node) {
                String uuid = node.uuid;
                model.setValue(Task.COMPLETION_DATE, completionDate);
                taskService.update(Task.UUID.eq(uuid), model);
                model.clear();

                taskAdapter.getCompletedItems().put(node.uuid, true);
                chained.add(node.uuid);
            }
        });

        if(chained.size() > 0) {
            // move recurring items to item parent
            TodorooCursor<Task> recurring = taskService.query(Query.select(Task.UUID, Task.RECURRENCE).where(
                    Criterion.and(Task.UUID.in(chained.toArray(new String[chained.size()])),
                                   Task.RECURRENCE.isNotNull(), Functions.length(Task.RECURRENCE).gt(0))));
            try {
                Task t = new Task();
                boolean madeChanges = false;
                for (recurring.moveToFirst(); !recurring.isAfterLast(); recurring.moveToNext()) {
                    t.clear();
                    t.readFromCursor(recurring);
                    if (!TextUtils.isEmpty(t.getValue(Task.RECURRENCE))) {
                        updater.moveToParentOf(t.getUuid(), itemId);
                        madeChanges = true;
                    }
                }

                if (madeChanges) {
                    updater.writeSerialization(list, updater.serializeTree(), true);
                }
            } finally {
                recurring.close();
            }


            chainedCompletions.put(itemId, chained);
            taskAdapter.notifyDataSetInvalidated();
        }
    }

    @Override
    public Property<?>[] taskProperties() {
        return fragment.taskProperties();
    }

    public void setList(LIST list) {
        this.list = list;
    }

    public void onCreateTask(Task task) {
        updater.onCreateTask(list, getFilter(), task.getUuid());
        fragment.reconstructCursor();
        fragment.loadTaskListContent(true);
        fragment.selectCustomId(task.getId());
    }

    public void onDeleteTask(Task task) {
        updater.onDeleteTask(list, getFilter(), task.getUuid());
        taskAdapter.notifyDataSetInvalidated();
    }


}
