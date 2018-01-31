/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import com.google.common.collect.ObjectArrays;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.Task;

import org.tasks.data.TaskAttachment;

import java.util.ArrayList;
import java.util.List;

import static com.todoroo.astrid.data.Task.NO_ID;
import static com.todoroo.astrid.data.Task.NO_UUID;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter {

    private List<Task> tasks;

    public List<Integer> getTaskPositions(List<Long> longs) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0 ; i < tasks.size() ; i++) {
            if (longs.contains(tasks.get(i).getId())) {
                result.add(i);
            }
        }
        return result;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public int getCount() {
        return tasks.size();
    }

    public interface OnCompletedTaskListener {
        void onCompletedTask(Task item, boolean newState);
    }

    public static final StringProperty TAGS = new StringProperty(null, "group_concat(nullif(" + TaskListFragment.TAGS_METADATA_JOIN + ".tag_uid, '')"+ ", ',')").as("tags");
    public static final LongProperty FILE_ID_PROPERTY = TaskAttachment.ID.cloneAs(TaskListFragment.FILE_METADATA_JOIN, "fileId");

    public static final Property<?>[] PROPERTIES = ObjectArrays.concat(
            Task.PROPERTIES,
            new Property<?>[]{
                    TAGS, // Concatenated list of tags
                    FILE_ID_PROPERTY // File id
            }, Property.class);

    private OnCompletedTaskListener onCompletedTaskListener = null;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public int getIndent(Task task) {
        return 0;
    }

    public boolean canIndent(int position, Task task) {
        return false;
    }

    public boolean isManuallySorted() {
        return false;
    }

    public void moved(int from, int to) {

    }

    public void indented(int position, int delta) {

    }

    public List<Task> getTasks() {
        return tasks;
    }

    public long getTaskId(int position) {
        return position < tasks.size() ? tasks.get(position).getId() : NO_ID;
    }

    public Task getTask(int position) {
        return position < tasks.size() ? tasks.get(position) : null;
    }

    protected String getItemUuid(int position) {
        return position < tasks.size() ? tasks.get(position).getUuid() : NO_UUID;
    }

    public void onCompletedTask(Task task, boolean newState) {
        if (onCompletedTaskListener != null) {
            onCompletedTaskListener.onCompletedTask(task, newState);
        }
    }

    public void setOnCompletedTaskListener(final OnCompletedTaskListener newListener) {
        this.onCompletedTaskListener = newListener;
    }
}
