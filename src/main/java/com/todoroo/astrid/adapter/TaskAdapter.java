/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.tasklist.ViewHolder;
import org.tasks.tasklist.ViewHolderFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter implements Filterable {

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

    private final TaskDao taskDao;

    private final TaskListFragment fragment;
    private OnCompletedTaskListener onCompletedTaskListener = null;

    private final AtomicReference<String> query;

    private final ViewHolderFactory viewHolderFactory;

    public TaskAdapter(Context context, TaskDao taskDao, TaskListFragment fragment, Cursor c,
                       AtomicReference<String> query, ViewHolderFactory viewHolderFactory) {
        super(context, c, false);
        this.taskDao = taskDao;
        this.query = query;
        this.fragment = fragment;
        this.viewHolderFactory = viewHolderFactory;
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        return fetchFiltered(query.get(), constraint, fragment.taskProperties());
    }

    /**
     * Fetch tasks for the given filter
     * @param constraint text constraint, or null
     */
    private TodorooCursor<Task> fetchFiltered(String queryTemplate, CharSequence constraint,
                                             Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null) {
            whereConstraint = Functions.upper(Task.TITLE).like("%" +
                    constraint.toString().toUpperCase() + "%");
        }

        if(queryTemplate == null) {
            if(whereConstraint == null) {
                return taskDao.query(Query.selectDistinct(properties));
            } else {
                return taskDao.query(Query.selectDistinct(properties).where(whereConstraint));
            }
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE")) {
                sql = queryTemplate + " WHERE " + whereConstraint;
            } else {
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
            }
        } else {
            sql = queryTemplate;
        }

        sql = PermaSql.replacePlaceholders(sql);

        return taskDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(context)
                .inflate(R.layout.task_adapter_row_simple, parent, false);

        // create view holder
        viewHolderFactory.newViewHolder(view, this::onTaskCompleted);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
        ViewHolder viewHolder = ((ViewHolder)view.getTag());
        viewHolder.bindView(cursor);

        adjustView(viewHolder);
    }

    protected void adjustView(ViewHolder viewHolder) {

    }

    public String getItemUuid(int position) {
        TodorooCursor<Task> c = (TodorooCursor<Task>) getCursor();
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

    public void onClick(View v) {
        // expand view (unless deleted)
        final ViewHolder viewHolder = (ViewHolder)v.getTag();
        if(viewHolder.task.isDeleted()) {
            return;
        }

        long taskId = viewHolder.task.getId();
        fragment.onTaskListItemClicked(taskId);
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        viewHolderFactory.updateTagMap();
    }

    private void onTaskCompleted(Task task, boolean newState) {
        if (onCompletedTaskListener != null) {
            onCompletedTaskListener.onCompletedTask(task, newState);
        }
    }

    public void setOnCompletedTaskListener(final OnCompletedTaskListener newListener) {
        this.onCompletedTaskListener = newListener;
    }
}
