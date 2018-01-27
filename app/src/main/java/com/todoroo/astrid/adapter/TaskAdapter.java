/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;

import com.google.common.collect.ObjectArrays;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.Task;

import org.tasks.data.TaskAttachment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter implements Filterable  {

    public List<Integer> getTaskPositions(List<Long> longs) {
        List<Integer> result = new ArrayList<>();
        TodorooCursor taskCursor = getTaskCursor();
        for (taskCursor.moveToFirst() ; !taskCursor.isAfterLast() ; taskCursor.moveToNext()) {
            if (longs.contains(taskCursor.get(Task.ID))) {
                result.add(taskCursor.getPosition());
            }
        }
        return result;
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

    public TaskAdapter(Context context, Cursor c) {
        super(context, c, false);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        throw new RuntimeException();
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        throw new RuntimeException();
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

    private TodorooCursor getTaskCursor() {
        return (TodorooCursor) getCursor();
    }

    public long getTaskId(int position) {
        TodorooCursor c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.get(Task.ID);
            }
        }
        return Task.NO_ID;
    }

    public Task getTask(int position) {
        TodorooCursor c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.toModel();
            }
        }
        return null;
    }

    protected String getItemUuid(int position) {
        TodorooCursor c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.get(Task.UUID);
            } else {
                return Task.NO_UUID;
            }
        } else {
            return Task.NO_UUID;
        }
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
