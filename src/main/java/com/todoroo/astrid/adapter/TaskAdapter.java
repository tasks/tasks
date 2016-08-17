/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.ui.CheckableImageView;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.CheckBoxes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.tasks.preferences.ResourceResolver.getData;

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

    private static final char SPACE = '\u0020';
    private static final char HAIR_SPACE = '\u200a';
    private static final StringProperty TAGS = new StringProperty(null, "group_concat(nullif(" + TaskListFragment.TAGS_METADATA_JOIN + "." + TaskToTagMetadata.TAG_UUID.name + ", '')"+ ", ',')").as("tags");
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
    private final TagService tagService;
    private ThemeCache themeCache;
    protected final Resources resources;
    protected OnCompletedTaskListener onCompletedTaskListener = null;
    protected final LayoutInflater inflater;
    private int fontSize;

    private final AtomicReference<String> query;

    // measure utilities
    protected final DisplayMetrics displayMetrics;

    protected final int minRowHeight;
    private final float tagCharacters;

    private final Map<String, TagData> tagMap = new HashMap<>();

    private final int textColorSecondary;
    private final int textColorHint;
    private final int textColorOverdue;

    public TaskAdapter(Context context, Preferences preferences, TaskAttachmentDao taskAttachmentDao, TaskService taskService, TaskListFragment fragment,
                       Cursor c, AtomicReference<String> query, OnCompletedTaskListener onCompletedTaskListener,
                       DialogBuilder dialogBuilder, CheckBoxes checkBoxes, TagService tagService, ThemeCache themeCache) {
        super(context, c, false);
        this.checkBoxes = checkBoxes;
        this.preferences = preferences;
        this.taskAttachmentDao = taskAttachmentDao;
        this.taskService = taskService;
        this.context = context;
        this.query = query;
        this.fragment = fragment;
        this.dialogBuilder = dialogBuilder;
        this.tagService = tagService;
        this.themeCache = themeCache;
        this.resources = fragment.getResources();
        this.onCompletedTaskListener = onCompletedTaskListener;
        inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.tag_characters, typedValue, true);
        tagCharacters = typedValue.getFloat();

        fontSize = preferences.getIntegerFromString(R.string.p_fontSize, 18);
        displayMetrics = new DisplayMetrics();
        fragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        textColorSecondary = getData(context, android.R.attr.textColorSecondary);
        textColorHint = getData(context, android.R.attr.textColorTertiary);
        textColorOverdue = getColor(context, R.color.overdue);

        updateTagMap();
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
        viewHolder.rowBody = (ViewGroup)view.findViewById(R.id.rowBody);
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.completeBox = (CheckableImageView)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.due_date);
        viewHolder.tagBlock = (TextView) view.findViewById(R.id.tag_block);
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
        public ViewGroup rowBody;
        public TextView nameView;
        public CheckableImageView completeBox;
        public TextView dueDate;
        public TextView tagBlock;
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
                viewHolder.taskActionContainer.setVisibility(View.VISIBLE);
                taskAction.setImageDrawable(action.icon);
                taskAction.setTag(action);
            } else {
                viewHolder.taskActionContainer.setVisibility(View.GONE);
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
        OnTouchListener otl = (v, event) -> {
            lastTouchYRawY = new Pair<>(event.getY(), event.getRawY());
            return false;
        };
        viewHolder.completeBox.setOnTouchListener(otl);
        viewHolder.completeBox.setOnClickListener(completeBoxListener);

        if (viewHolder.taskActionContainer != null) {
            viewHolder.taskActionContainer.setOnClickListener(v -> {
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
        updateTagMap();
    }

    private void updateTagMap() {
        tagMap.clear();
        for (TagData tagData : tagService.getTagList()) {
            tagMap.put(tagData.getUuid(), tagData);
        }
    }

    protected final View.OnClickListener completeBoxListener = v -> {
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
    };

    private ViewHolder getTagFromCheckBox(View v) {
        return (ViewHolder)((View)v.getParent()).getTag();
    }

    /** Helper method to adjust a tasks' appearance if the task is completed or
     * uncompleted.
     */
    protected void setTaskAppearance(ViewHolder viewHolder, Task task) {
        boolean completed = task.isCompleted();

        TextView name = viewHolder.nameView;
        if (completed) {
            name.setEnabled(false);
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            name.setEnabled(true);
            name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        name.setTextSize(fontSize);

        setupDueDateAndTags(viewHolder, task);

        float detailTextSize = Math.max(10, fontSize * 14 / 20);
        if(viewHolder.dueDate != null) {
            viewHolder.dueDate.setTextSize(detailTextSize);
            viewHolder.dueDate.setTypeface(null, 0);
        }

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

    private final Function<String, TagData> uuidToTag = tagMap::get;

    private final Ordering<TagData> orderByName = new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
            return left.getName().compareTo(right.getName());
        }
    };

    private final Ordering<TagData> orderByLength = new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
            int leftLength = left.getName().length();
            int rightLength = right.getName().length();
            if (leftLength < rightLength) {
                return -1;
            } else if (rightLength < leftLength) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    private Function<TagData, SpannableString> tagToString(final float maxLength) {
        return tagData -> {
            String tagName = tagData.getName();
            tagName = tagName.substring(0, Math.min(tagName.length(), (int) maxLength));
            SpannableString string = new SpannableString(SPACE + tagName + SPACE);
            int themeIndex = tagData.getColor();
            ThemeColor color = themeIndex >= 0 ? themeCache.getThemeColor(themeIndex) : themeCache.getUntaggedColor();
            string.setSpan(new BackgroundColorSpan(color.getPrimaryColor()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(color.getActionBarTint()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return string;
        };
    }

    // Returns due date text width
    private void setupDueDateAndTags(ViewHolder viewHolder, Task task) {
        // due date / completion date
        final TextView dueDateView = viewHolder.dueDate; {
            if(!task.isCompleted() && task.hasDueDate()) {
                long dueDate = task.getDueDate();
                if(task.isOverdue()) {
                    dueDateView.setTextColor(textColorOverdue);
                } else {
                    dueDateView.setTextColor(textColorSecondary);
                }
                String dateValue = DateUtilities.getRelativeDateStringWithTime(context, dueDate);
                dueDateView.setText(dateValue);
                dueDateView.setVisibility(View.VISIBLE);
            } else if(task.isCompleted()) {
                String dateValue = DateUtilities.getRelativeDateStringWithTime(context, task.getCompletionDate());
                dueDateView.setText(resources.getString(R.string.TAd_completed, dateValue));
                dueDateView.setTextColor(textColorHint);
                dueDateView.setVisibility(View.VISIBLE);
            } else {
                dueDateView.setVisibility(View.GONE);
            }

            if (task.isCompleted()) {
                viewHolder.tagBlock.setVisibility(View.GONE);
            } else {
                String tags = viewHolder.tagsString;
                List<String> tagUuids = tags != null ? newArrayList(tags.split(",")) : Lists.newArrayList();
                Iterable<TagData> t = filter(transform(tagUuids, uuidToTag), Predicates.notNull());
                List<TagData> firstFourByName = orderByName.leastOf(t, 4);
                int numTags = firstFourByName.size();
                if (numTags > 0) {
                    List<TagData> firstFourByNameLength = orderByLength.sortedCopy(firstFourByName);
                    float maxLength = tagCharacters / numTags;
                    for (int i = 0; i < numTags - 1; i++) {
                        TagData tagData = firstFourByNameLength.get(i);
                        String name = tagData.getName();
                        if (name.length() >= maxLength) {
                            break;
                        }
                        float excess = maxLength - name.length();
                        int beneficiaries = numTags - i - 1;
                        float additional = excess / beneficiaries;
                        maxLength += additional;
                    }
                    List<SpannableString> tagStrings = transform(firstFourByName, tagToString(maxLength));
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    for (SpannableString tagString : tagStrings) {
                        if (builder.length() > 0) {
                            builder.append(HAIR_SPACE);
                        }
                        builder.append(tagString);
                    }
                    viewHolder.tagBlock.setText(builder);
                    viewHolder.tagBlock.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.tagBlock.setVisibility(View.GONE);
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
            this.onCompletedTaskListener = (item, newState) -> {
                old.onCompletedTask(item, newState);
                newListener.onCompletedTask(item, newState);
            };
        }
    }

}
