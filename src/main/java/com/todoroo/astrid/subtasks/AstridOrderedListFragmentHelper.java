package com.todoroo.astrid.subtasks;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

class AstridOrderedListFragmentHelper {

    private final SubtasksFilterUpdater updater;
    private final TaskDao taskDao;

    private DraggableTaskAdapter taskAdapter;
    private TaskListFragment fragment;
    private TaskListMetadata list;

    @Inject
    AstridOrderedListFragmentHelper(SubtasksFilterUpdater updater, TaskDao taskDao) {
        this.updater = updater;
        this.taskDao = taskDao;
    }

    void setTaskListFragment(TaskListFragment fragment) {
        this.fragment = fragment;
    }

    void beforeSetUpTaskList(Filter filter) {
        updater.initialize(list, filter);
    }

    TaskAdapter createTaskAdapter(Context context, TodorooCursor<Task> cursor) {
        taskAdapter = new DraggableTaskAdapter(context, cursor);

        taskAdapter.setOnCompletedTaskListener(this::setCompletedForItemAndSubtasks);

        return taskAdapter;
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public int getIndent(Task task) {
            return updater.getIndentForTask(task.getUuid());
        }

        @Override
        public boolean canIndent(int position, Task task) {
            String parentUuid = taskAdapter.getItemUuid(position - 1);
            int parentIndent = updater.getIndentForTask(parentUuid);
            return getIndent(task) <= parentIndent;
        }

        @Override
        public boolean isManuallySorted() {
            return true;
        }

        @Override
        public void moved(int from, int to) {
            String targetTaskId = taskAdapter.getItemUuid(from);
            if (!RemoteModel.isValidUuid(targetTaskId)) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            String destinationTaskId = taskAdapter.getItemUuid(to);

            try {
                if(to >= taskAdapter.getCount()) {
                    updater.moveTo(list, fragment.getFilter(), targetTaskId, "-1"); //$NON-NLS-1$
                } else {
                    updater.moveTo(list, fragment.getFilter(), targetTaskId, destinationTaskId);
                }
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }

            fragment.reconstructCursor();
            fragment.loadTaskListContent();
        }

        @Override
        public void indented(int which, int delta) {
            String targetTaskId = taskAdapter.getItemUuid(which);
            if (!RemoteModel.isValidUuid(targetTaskId)) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            try {
                updater.indent(list, fragment.getFilter(), targetTaskId, delta);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }

            fragment.reconstructCursor();
            fragment.loadTaskListContent();
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
                    taskDao.update(Task.UUID.eq(taskId), model);
                    model.clear();
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<String> chained = new ArrayList<>();
        updater.applyToDescendants(itemId, node -> {
            String uuid = node.uuid;
            model.setCompletionDate(completionDate);
            taskDao.update(Task.UUID.eq(uuid), model);
            model.clear();
            chained.add(node.uuid);
        });

        if(chained.size() > 0) {
            // move recurring items to item parent
            TodorooCursor<Task> recurring = taskDao.query(Query.select(Task.UUID, Task.RECURRENCE).where(
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

    public void setList(TaskListMetadata list) {
        this.list = list;
    }

    void onCreateTask(String uuid) {
        updater.onCreateTask(list, fragment.getFilter(), uuid);
        fragment.reconstructCursor();
        fragment.loadTaskListContent();
    }

    void onDeleteTask(Task task) {
        updater.onDeleteTask(list, fragment.getFilter(), task.getUuid());
        taskAdapter.notifyDataSetInvalidated();
    }
}
