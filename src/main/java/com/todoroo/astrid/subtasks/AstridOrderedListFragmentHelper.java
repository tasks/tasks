package com.todoroo.astrid.subtasks;

import android.app.Activity;
import android.content.Context;
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
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.ui.DraggableListView;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public class AstridOrderedListFragmentHelper<LIST> implements OrderedListFragmentHelperInterface {

    private final DisplayMetrics metrics = new DisplayMetrics();
    private final AstridOrderedListUpdater<LIST> updater;
    private DialogBuilder dialogBuilder;
    private final TaskListFragment fragment;
    private final Preferences preferences;
    private final TaskAttachmentDao taskAttachmentDao;
    private final TaskService taskService;

    private DraggableTaskAdapter taskAdapter;

    private LIST list;

    public AstridOrderedListFragmentHelper(Preferences preferences, TaskAttachmentDao taskAttachmentDao,
                                           TaskService taskService, TaskListFragment fragment,
                                           AstridOrderedListUpdater<LIST> updater, DialogBuilder dialogBuilder) {
        this.preferences = preferences;
        this.taskAttachmentDao = taskAttachmentDao;
        this.taskService = taskService;
        this.fragment = fragment;
        this.updater = updater;
        this.dialogBuilder = dialogBuilder;
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

    @Override
    public void setUpUiComponents() {
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.asThemeTextColor, tv, false);
        DraggableListView draggableListView = (DraggableListView) fragment.getListView();
        draggableListView.setDragndropBackgroundColor(tv.data);
        draggableListView.setDropListener(dropListener);
        draggableListView.setClickListener(rowClickListener);
        draggableListView.setSwipeListener(swipeListener);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        draggableListView.setItemHightNormal(taskAdapter.computeFullRowHeight());
    }

    @Override
    public void beforeSetUpTaskList(Filter filter) {
        updater.initialize(list, filter);
    }

    private final DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            String targetTaskId = taskAdapter.getItemUuid(from);
            if (!RemoteModel.isValidUuid(targetTaskId)) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            String destinationTaskId = taskAdapter.getItemUuid(to);

            try {
                if(to >= getListView().getCount()) {
                    updater.moveTo(list, getFilter(), targetTaskId, "-1"); //$NON-NLS-1$
                } else {
                    updater.moveTo(list, getFilter(), targetTaskId, destinationTaskId);
                }
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }

            fragment.reconstructCursor();
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
            String targetTaskId = taskAdapter.getItemUuid(which);
            if (!RemoteModel.isValidUuid(targetTaskId)) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            try {
                updater.indent(list, getFilter(), targetTaskId, delta);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }

            fragment.reconstructCursor();
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
    public TaskAdapter createTaskAdapter(Context context, TodorooCursor<Task> cursor,
            AtomicReference<String> sqlQueryTemplate) {

        taskAdapter = new DraggableTaskAdapter(context, preferences, fragment, cursor, sqlQueryTemplate, dialogBuilder);

        taskAdapter.addOnCompletedTaskListener(new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                setCompletedForItemAndSubtasks(item, newState);
            }
        });

        return taskAdapter;
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(Context context, Preferences preferences, TaskListFragment activity,
                Cursor c, AtomicReference<String> query, DialogBuilder dialogBuilder) {
            super(context, preferences, taskAttachmentDao, taskService, activity, c, query, null, dialogBuilder);
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            ViewHolder vh = (ViewHolder) view.getTag();
            int indent = updater.getIndentForTask(vh.task.getUuid());
            vh.rowBody.setPadding(Math.round(indent * 20 * metrics.density), 0, 0, 0);
        }
    }

    private final Map<String, ArrayList<String>> chainedCompletions =
        Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());

    private void setCompletedForItemAndSubtasks(final Task item, final boolean completedState) {
        final String itemId = item.getUuid();

        final Task model = new Task();
        final long completionDate = completedState ? DateUtilities.now() : 0;

        if(!completedState) {
            ArrayList<String> chained = chainedCompletions.get(itemId);
            if(chained != null) {
                for(String taskId : chained) {
                    model.setCompletionDate(completionDate);
                    taskService.update(Task.UUID.eq(taskId), model);
                    model.clear();
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<String> chained = new ArrayList<>();
        updater.applyToDescendants(itemId, new AstridOrderedListUpdater.OrderedListNodeVisitor() {
            @Override
            public void visitNode(AstridOrderedListUpdater.Node node) {
                String uuid = node.uuid;
                model.setCompletionDate(completionDate);
                taskService.update(Task.UUID.eq(uuid), model);
                model.clear();
                chained.add(node.uuid);
            }
        });

        if(chained.size() > 0) {
            // move recurring items to item parent
            TodorooCursor<Task> recurring = taskService.query(Query.select(Task.UUID, Task.RECURRENCE).where(
                    Criterion.and(Task.UUID.in(chained.toArray(new String[chained.size()])),
                                   Task.RECURRENCE.isNotNull(), Functions.length(Task.RECURRENCE).gt(0))));
            try {
                boolean madeChanges = false;
                for (recurring.moveToFirst(); !recurring.isAfterLast(); recurring.moveToNext()) {
                    Task t = new Task(recurring);
                    if (!TextUtils.isEmpty(t.getRecurrence())) {
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

    @Override
    public void onCreateTask(long id, String uuid) {
        updater.onCreateTask(list, getFilter(), uuid);
        fragment.reconstructCursor();
        fragment.loadTaskListContent();
    }

    @Override
    public void onDeleteTask(Task task) {
        updater.onDeleteTask(list, getFilter(), task.getUuid());
        taskAdapter.notifyDataSetInvalidated();
    }
}
