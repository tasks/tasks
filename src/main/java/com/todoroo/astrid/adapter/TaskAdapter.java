/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.app.Activity;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.core.LinkActionExposer;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.ui.CheckableImageView;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;

import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

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

    private static final StringProperty TAGS = new StringProperty(null, "group_concat(nullif(" + TaskListFragment.TAGS_METADATA_JOIN + "." + TaskToTagMetadata.TAG_NAME.name + ", '')"+ ", '  |  ')").as("tags");
    private static final LongProperty FILE_ID_PROPERTY = TaskAttachment.ID.cloneAs(TaskListFragment.FILE_METADATA_JOIN, "fileId");
    private static final IntegerProperty HAS_NOTES_PROPERTY = new IntegerProperty(null, "length(" + Task.NOTES + ") > 0").as("hasNotes");

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

    private final CheckBoxes checkBoxes;
    private final Preferences preferences;
    private final TaskAttachmentDao taskAttachmentDao;
    private final TaskService taskService;

    protected final Context context;
    protected final TaskListFragment fragment;
    private DialogBuilder dialogBuilder;
    protected final Resources resources;
    protected OnCompletedTaskListener onCompletedTaskListener = null;
    protected final LayoutInflater inflater;
    private int fontSize;

    private final AtomicReference<String> query;

    // measure utilities
    protected final Paint paint;
    protected final DisplayMetrics displayMetrics;

    protected final int minRowHeight;

    public TaskAdapter(Context context, Preferences preferences, TaskAttachmentDao taskAttachmentDao, TaskService taskService, TaskListFragment fragment,
            Cursor c, AtomicReference<String> query, OnCompletedTaskListener onCompletedTaskListener,
                       DialogBuilder dialogBuilder) {
        super(context, c, false);
        this.checkBoxes = new CheckBoxes(context);
        this.preferences = preferences;
        this.taskAttachmentDao = taskAttachmentDao;
        this.taskService = taskService;
        this.context = context;
        this.query = query;
        this.fragment = fragment;
        this.dialogBuilder = dialogBuilder;
        this.resources = fragment.getResources();
        this.onCompletedTaskListener = onCompletedTaskListener;
        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        fontSize = preferences.getIntegerFromString(R.string.p_fontSize, 18);
        paint = new Paint();
        displayMetrics = new DisplayMetrics();
        fragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        this.minRowHeight = computeMinRowHeight();
    }

    protected int computeMinRowHeight() {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (metrics.density * 40);
    }

    public int computeFullRowHeight() {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        if (fontSize < 16) {
            return (int) (39 * metrics.density);
        } else {
            return minRowHeight + (int) (10 * metrics.density);
        }
    }

    /* ======================================================================
     * =========================================================== filterable
     * ====================================================================== */

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        return taskService.fetchFiltered(query.get(), constraint, fragment.taskProperties());
    }

    /* ======================================================================
     * =========================================================== view setup
     * ====================================================================== */

    /** Creates a new view for use in the list view */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(R.layout.task_adapter_row_simple, parent, false);

        // create view holder
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.task = new Task();
        viewHolder.view = view;
        viewHolder.rowBody = (ViewGroup)view.findViewById(R.id.rowBody);
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.completeBox = (CheckableImageView)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
        viewHolder.tagsView = (TextView)view.findViewById(R.id.tagsDisplay);
        viewHolder.taskActionContainer = view.findViewById(R.id.taskActionContainer);
        viewHolder.taskActionIcon = (ImageView)view.findViewById(R.id.taskActionIcon);

        boolean showFullTaskTitle = preferences.getBoolean(R.string.p_fullTaskTitle, false);
        if (showFullTaskTitle) {
            viewHolder.nameView.setMaxLines(Integer.MAX_VALUE);
            viewHolder.nameView.setSingleLine(false);
            viewHolder.nameView.setEllipsize(null);
        }


        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++) {
            view.getChildAt(i).setTag(viewHolder);
        }

        // add UI component listeners
        addListeners(view);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
        ViewHolder viewHolder = ((ViewHolder)view.getTag());

        viewHolder.tagsString = cursor.get(TAGS);
        viewHolder.hasFiles = cursor.get(FILE_ID_PROPERTY) > 0;
        viewHolder.hasNotes = cursor.get(HAS_NOTES_PROPERTY) > 0;

        // TODO: see if this is a performance issue
        viewHolder.task = new Task(cursor);

        setFieldContentsAndVisibility(view);
        setTaskAppearance(viewHolder, viewHolder.task);
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

    /**
     * View Holder saves a lot of findViewById lookups.
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class ViewHolder {
        public Task task;
        public ViewGroup view;
        public ViewGroup rowBody;
        public TextView nameView;
        public CheckableImageView completeBox;
        public TextView dueDate;
        public TextView tagsView;
        public View taskActionContainer;
        public ImageView taskActionIcon;
        public String tagsString; // From join query, not part of the task model
        public boolean hasFiles; // From join query, not part of the task model
        public boolean hasNotes;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        Task task = viewHolder.task;
        if (fontSize < 16) {
            viewHolder.rowBody.setMinimumHeight(0);
            viewHolder.completeBox.setMinimumHeight(0);
        } else {
            viewHolder.rowBody.setMinimumHeight(minRowHeight);
            viewHolder.completeBox.setMinimumHeight(minRowHeight);
        }

        // name
        final TextView nameView = viewHolder.nameView; {
            String nameValue = task.getTitle();

            long hiddenUntil = task.getHideUntil();
            if(task.getDeletionDate() > 0) {
                nameValue = resources.getString(R.string.TAd_deletedFormat, nameValue);
            }
            if(hiddenUntil > DateUtilities.now()) {
                nameValue = resources.getString(R.string.TAd_hiddenFormat, nameValue);
            }
            nameView.setText(nameValue);
        }

        setupDueDateAndTags(viewHolder, task);

        // Task action
        ImageView taskAction = viewHolder.taskActionIcon;
        if (taskAction != null) {
            TaskAction action = getTaskAction(task, viewHolder.hasFiles, viewHolder.hasNotes);
            if (action != null) {
                taskAction.setVisibility(View.VISIBLE);
                taskAction.setImageDrawable(action.icon);
                taskAction.setTag(action);
            } else {
                taskAction.setVisibility(View.GONE);
                taskAction.setTag(null);
            }
        }
    }

    private TaskAction getTaskAction(Task task, boolean hasFiles, boolean hasNotes) {
        if (task.isCompleted()) {
            return null;
        }
        return LinkActionExposer.getActionsForTask(context, task, hasFiles, hasNotes);
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

    private Pair<Float, Float> lastTouchYRawY = new Pair<>(0f, 0f);

    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    private void addListeners(final View container) {
        final ViewHolder viewHolder = (ViewHolder)container.getTag();

        // check box listener
        OnTouchListener otl = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                lastTouchYRawY = new Pair<>(event.getY(), event.getRawY());
                return false;
            }
        };
        viewHolder.completeBox.setOnTouchListener(otl);
        viewHolder.completeBox.setOnClickListener(completeBoxListener);

        if (viewHolder.taskActionContainer != null) {
            viewHolder.taskActionContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskAction action = (TaskAction) viewHolder.taskActionIcon.getTag();
                    if (action instanceof NotesAction) {
                        showEditNotesDialog(viewHolder.task);
                    } else if (action instanceof FilesAction) {
                        showFilesDialog(viewHolder.task);
                    } else if (action != null) {
                        try {
                            action.intent.send();
                        } catch (CanceledException e) {
                            // Oh well
                            Timber.e(e, e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void showEditNotesDialog(final Task task) {
        String notes = null;
        Task t = taskService.fetchById(task.getId(), Task.NOTES);
        if (t != null) {
            notes = t.getNotes();
        }
        if (TextUtils.isEmpty(notes)) {
            return;
        }

        dialogBuilder.newDialog()
                .setMessage(notes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showFilesDialog(Task task) {
        // TODO: reimplement this
//        FilesControlSet filesControlSet = new FilesControlSet();
//        filesControlSet.hideAddAttachmentButton();
//        filesControlSet.readFromTask(task);
//        filesControlSet.getView().performClick();
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        fontSize = preferences.getIntegerFromString(R.string.p_fontSize, 18);
    }

    protected final View.OnClickListener completeBoxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            ViewHolder viewHolder = getTagFromCheckBox(v);

            if(Math.abs(location[1] + lastTouchYRawY.getLeft() - lastTouchYRawY.getRight()) > 10) {
                viewHolder.completeBox.setChecked(!viewHolder.completeBox.isChecked());
                return;
            }

            Task task = viewHolder.task;

            completeTask(task, viewHolder.completeBox.isChecked());

            // set check box to actual action item state
            setTaskAppearance(viewHolder, task);
        }
    };

    private ViewHolder getTagFromCheckBox(View v) {
        return (ViewHolder)((View)v.getParent()).getTag();
    }

    /** Helper method to adjust a tasks' appearance if the task is completed or
     * uncompleted.
     */
    protected void setTaskAppearance(ViewHolder viewHolder, Task task) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }

        boolean state = task.isCompleted();

        TextView name = viewHolder.nameView;
        if(state) {
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle_Completed);
        } else {
            name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle);
        }
        name.setTextSize(fontSize);

        setupDueDateAndTags(viewHolder, task);

        float detailTextSize = Math.max(10, fontSize * 14 / 20);
        if(viewHolder.dueDate != null) {
            viewHolder.dueDate.setTextSize(detailTextSize);
            viewHolder.dueDate.setTypeface(null, 0);
        }
        if (viewHolder.tagsView != null) {
            viewHolder.tagsView.setTextSize(detailTextSize);
            viewHolder.tagsView.setTypeface(null, 0);
        }
        paint.setTextSize(detailTextSize);

        setupCompleteBox(viewHolder);

    }

    private void setupCompleteBox(ViewHolder viewHolder) {
        // complete box
        final Task task = viewHolder.task;
        final CheckableImageView checkBoxView = viewHolder.completeBox;
        boolean completed = task.isCompleted();
        checkBoxView.setChecked(completed);

        if (completed) {
            checkBoxView.setImageDrawable(checkBoxes.getCompletedCheckbox(task.getImportance()));
        } else if (TextUtils.isEmpty(task.getRecurrence())) {
            checkBoxView.setImageDrawable(checkBoxes.getCheckBox(task.getImportance()));
        } else {
            checkBoxView.setImageDrawable(checkBoxes.getRepeatingCheckBox(task.getImportance()));
        }
        checkBoxView.invalidate();
    }

    // Returns due date text width
    private void setupDueDateAndTags(ViewHolder viewHolder, Task task) {
        // due date / completion date
        final TextView dueDateView = viewHolder.dueDate; {
            Activity activity = fragment.getActivity();
            if (activity != null) {
                if(!task.isCompleted() && task.hasDueDate()) {
                    long dueDate = task.getDueDate();
                    if(task.isOverdue()) {
                        dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Overdue);
                    } else {
                        dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate);
                    }
                    String dateValue = DateUtilities.getRelativeDateStringWithTime(context, dueDate);
                    dueDateView.setText(dateValue);
                    dueDateView.setVisibility(View.VISIBLE);
                } else if(task.isCompleted()) {
                    String dateValue = DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate());
                    dueDateView.setText(resources.getString(R.string.TAd_completed, dateValue));
                    dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Completed);
                    dueDateView.setVisibility(View.VISIBLE);
                } else {
                    dueDateView.setVisibility(View.GONE);
                }

                if (viewHolder.tagsView != null) {
                    String tags = viewHolder.tagsString;
                    if (tags != null && task.hasDueDate()) {
                        tags = "  |  " + tags; //$NON-NLS-1$
                    }
                    if (!task.isCompleted()) {
                        viewHolder.tagsView.setText(tags);
                        viewHolder.tagsView.setVisibility(TextUtils.isEmpty(tags) ? View.GONE : View.VISIBLE);
                    } else {
                        viewHolder.tagsView.setText(""); //$NON-NLS-1$
                        viewHolder.tagsView.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    /**
     * This method is called when user completes a task via check box or other
     * means
     *
     * @param newState
     *            state that this task should be set to
     */
    protected void completeTask(final Task task, final boolean newState) {
        if(task == null) {
            return;
        }

        if (newState != task.isCompleted()) {
            if(onCompletedTaskListener != null) {
                onCompletedTaskListener.onCompletedTask(task, newState);
            }

            taskService.setComplete(task, newState);
        }
    }

    /**
     * Add a new listener
     */
    public void addOnCompletedTaskListener(final OnCompletedTaskListener newListener) {
        if(this.onCompletedTaskListener == null) {
            this.onCompletedTaskListener = newListener;
        } else {
            final OnCompletedTaskListener old = this.onCompletedTaskListener;
            this.onCompletedTaskListener = new OnCompletedTaskListener() {
                @Override
                public void onCompletedTask(Task item, boolean newState) {
                    old.onCompletedTask(item, newState);
                    newListener.onCompletedTask(item, newState);
                }
            };
        }
    }

}
