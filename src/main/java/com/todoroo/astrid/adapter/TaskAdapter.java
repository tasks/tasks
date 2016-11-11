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

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import java.util.ArrayList;
import java.util.List;

import static com.todoroo.andlib.data.AbstractModel.NO_ID;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter implements Filterable  {

    public List<Integer> getTaskPositions(List<Long> longs) {
        List<Integer> result = new ArrayList<>();
        TodorooCursor<Task> taskCursor = getTaskCursor();
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

    public static final StringProperty TAGS = new StringProperty(null, "group_concat(nullif(" + TaskListFragment.TAGS_METADATA_JOIN + "." + TaskToTagMetadata.TAG_UUID.name + ", '')"+ ", ',')").as("tags");
    public static final LongProperty FILE_ID_PROPERTY = TaskAttachment.ID.cloneAs(TaskListFragment.FILE_METADATA_JOIN, "fileId");
    public static final IntegerProperty HAS_NOTES_PROPERTY = new IntegerProperty(null, "length(" + Task.NOTES + ") > 0").as("hasNotes");

    // --- other constants

    /** Properties that need to be read from the action item */
    public static final Property<?>[] PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.UUID,
        Task.TITLE,
        Task.IMPORTANCE,
        Task.DUE_DATE,
        Task.COMPLETION_DATE,
        Task.MODIFICATION_DATE,
        Task.HIDE_UNTIL,
        Task.DELETION_DATE,
        Task.ELAPSED_SECONDS,
        Task.TIMER_START,
        Task.RECURRENCE,
        Task.REMINDER_LAST,
        HAS_NOTES_PROPERTY, // Whether or not the task has notes
        TAGS, // Concatenated list of tags
        FILE_ID_PROPERTY // File id
    };

    // --- instance variables

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

    private TodorooCursor<Task> getTaskCursor() {
        return (TodorooCursor<Task>) getCursor();
    }

    public long getTaskId(int position) {
        TodorooCursor<Task> c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.get(Task.ID);
            }
        }
        return NO_ID;
    }

    public Task getTask(int position) {
        TodorooCursor<Task> c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.toModel();
            }
        }
        return null;
    }

    protected String getItemUuid(int position) {
        TodorooCursor<Task> c = getTaskCursor();
        if (c != null) {
            if (c.moveToPosition(position)) {
                return c.get(Task.UUID);
            } else {
                return RemoteModel.NO_UUID;
            }
        } else {
            return RemoteModel.NO_UUID;
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
