/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.arch.paging.PagedListAdapterHelper;

import com.google.common.collect.ObjectArrays;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.Task;

import org.tasks.data.TaskAttachment;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter {

    private PagedListAdapterHelper<Task> helper;

    public int getCount() {
        return helper.getItemCount();
    }

    public void setHelper(PagedListAdapterHelper<Task> helper) {
        this.helper = helper;
    }

    public interface OnCompletedTaskListener {
        void onCompletedTask(Task item, boolean newState);
    }

    private static final StringProperty TAGS = new StringProperty(null, "group_concat(nullif(" + TaskListFragment.TAGS_METADATA_JOIN + ".tag_uid, '')"+ ", ',')").as("tags");
    private static final LongProperty FILE_ID_PROPERTY = TaskAttachment.ID.cloneAs(TaskListFragment.FILE_METADATA_JOIN, "fileId");

    public static final Property<?>[] PROPERTIES = ObjectArrays.concat(
            Task.PROPERTIES,
            new Property<?>[]{
                    TAGS, // Concatenated list of tags
                    FILE_ID_PROPERTY // File id
            }, Property.class);

    private OnCompletedTaskListener onCompletedTaskListener = null;

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

    public long getTaskId(int position) {
        return getTask(position).getId();
    }

    public Task getTask(int position) {
        return helper.getItem(position);
    }

    protected String getItemUuid(int position) {
        return getTask(position).getUuid();
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
