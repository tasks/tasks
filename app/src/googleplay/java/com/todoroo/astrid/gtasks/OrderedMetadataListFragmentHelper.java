/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

class OrderedMetadataListFragmentHelper {

    private final GtasksTaskListUpdater updater;

    private final TaskDao taskDao;
    private final MetadataDao metadataDao;

    private DraggableTaskAdapter taskAdapter;
    private TaskListFragment fragment;
    private GtasksList list;

    @Inject
    OrderedMetadataListFragmentHelper(TaskDao taskDao, MetadataDao metadataDao, GtasksTaskListUpdater updater) {
        this.taskDao = taskDao;
        this.metadataDao = metadataDao;
        this.updater = updater;
    }

    void setTaskListFragment(TaskListFragment fragment) {
        this.fragment = fragment;
    }

    void beforeSetUpTaskList(Filter filter) {
        updater.initialize(filter);
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
            return task.getValue(GtasksMetadata.INDENT);
        }

        @Override
        public boolean canIndent(int position, Task task) {
            Task parent = taskAdapter.getTask(position - 1);
            return parent != null && getIndent(task) <= parent.getValue(GtasksMetadata.INDENT);
        }

        @Override
        public boolean isManuallySorted() {
            return true;
        }

        @Override
        public void moved(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            if (targetTaskId <= 0) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            long destinationTaskId = taskAdapter.getItemId(to);

            try {
                if(to >= taskAdapter.getCount()) {
                    updater.moveTo(list, targetTaskId, -1);
                } else {
                    updater.moveTo(list, targetTaskId, destinationTaskId);
                }
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }

            fragment.loadTaskListContent();
        }

        @Override
        public void indented(int which, int delta) {
            long targetTaskId = taskAdapter.getItemId(which);
            if (targetTaskId <= 0) {
                return; // This can happen with gestures on empty parts of the list (e.g. extra space below tasks)
            }
            try {
                updater.indent(list, targetTaskId, delta);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
            fragment.loadTaskListContent();
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
                    taskDao.save(model);
                    model.clear();
                }
                taskAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        final ArrayList<Long> chained = new ArrayList<>();
        final int parentIndent = item.getValue(GtasksMetadata.INDENT);
        updater.applyToChildren(list, itemId, node -> {
            Task childTask = taskDao.fetch(node.taskId, Task.RECURRENCE);
            if(!TextUtils.isEmpty(childTask.getRecurrence())) {
                Metadata metadata = updater.getTaskMetadata(node.taskId);
                metadata.setValue(GtasksMetadata.INDENT, parentIndent);
                metadataDao.persist(metadata);
            }

            model.setId(node.taskId);
            model.setCompletionDate(completionDate);
            taskDao.save(model);
            model.clear();

            chained.add(node.taskId);
        });

        if(chained.size() > 0) {
            chainedCompletions.put(itemId, chained);
            taskAdapter.notifyDataSetInvalidated();
        }
    }

    public void setList(GtasksList list) {
        this.list = list;
    }

    void onDeleteTask(Task task) {
        updater.onDeleteTask(list, task.getId());
        taskAdapter.notifyDataSetInvalidated();
    }
}
