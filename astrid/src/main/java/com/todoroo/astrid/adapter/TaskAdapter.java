/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.core.LinkActionExposer;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.notes.NotesAction;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.ui.CheckableImageView;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter implements Filterable {

    public interface OnCompletedTaskListener {
        public void onCompletedTask(Task item, boolean newState);
    }

    public static final String DETAIL_SEPARATOR = " | "; //$NON-NLS-1$

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
        Task.DETAILS,
        Task.ELAPSED_SECONDS,
        Task.TIMER_START,
        Task.RECURRENCE,
        Task.USER_ID,
        Task.REMINDER_LAST,
        Task.SOCIAL_REMINDER,
        HAS_NOTES_PROPERTY, // Whether or not the task has notes
        TAGS, // Concatenated list of tags
        FILE_ID_PROPERTY // File id
    };

    public static final Property<?>[] BASIC_PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.UUID,
        Task.TITLE,
        Task.IMPORTANCE,
        Task.RECURRENCE,
        Task.COMPLETION_DATE,
        Task.HIDE_UNTIL,
        Task.DELETION_DATE
    };

    public static final int[] IMPORTANCE_RESOURCES = new int[] {
        R.drawable.check_box_1,
        R.drawable.check_box_2,
        R.drawable.check_box_3,
        R.drawable.check_box_4,
    };

    public static final int[] IMPORTANCE_RESOURCES_CHECKED = new int[] {
        R.drawable.check_box_checked_1,
        R.drawable.check_box_checked_2,
        R.drawable.check_box_checked_3,
        R.drawable.check_box_checked_4,
    };

    public static final int[] IMPORTANCE_RESOURCES_LARGE = new int[] {
        R.drawable.check_box_large_1,
        R.drawable.check_box_large_2,
        R.drawable.check_box_large_3,
        R.drawable.check_box_large_4,
    };

    public static final int[] IMPORTANCE_REPEAT_RESOURCES = new int[] {
        R.drawable.check_box_repeat_1,
        R.drawable.check_box_repeat_2,
        R.drawable.check_box_repeat_3,
        R.drawable.check_box_repeat_4,
    };

    public static final int[] IMPORTANCE_REPEAT_RESOURCES_CHECKED = new int[] {
        R.drawable.check_box_repeat_checked_1,
        R.drawable.check_box_repeat_checked_2,
        R.drawable.check_box_repeat_checked_3,
        R.drawable.check_box_repeat_checked_4,
    };

    public static final Drawable[] IMPORTANCE_DRAWABLES = new Drawable[IMPORTANCE_RESOURCES.length];
    public static final Drawable[] IMPORTANCE_DRAWABLES_CHECKED = new Drawable[IMPORTANCE_RESOURCES_CHECKED.length];
    private static final Drawable[] IMPORTANCE_DRAWABLES_LARGE = new Drawable[IMPORTANCE_RESOURCES_LARGE.length];
    public static final Drawable[] IMPORTANCE_REPEAT_DRAWABLES = new Drawable[IMPORTANCE_REPEAT_RESOURCES.length];
    public static final Drawable[] IMPORTANCE_REPEAT_DRAWABLES_CHECKED = new Drawable[IMPORTANCE_REPEAT_RESOURCES_CHECKED.length];

    // --- instance variables

    @Autowired
    protected TaskService taskService;

    protected final Context context;
    protected final TaskListFragment fragment;
    protected final Resources resources;
    protected final HashMap<Object, Boolean> completedItems = new HashMap<>(0);
    protected OnCompletedTaskListener onCompletedTaskListener = null;
    protected final int resource;
    protected final LayoutInflater inflater;
    private int fontSize;
    private long mostRecentlyMade = -1;
    private final ScaleAnimation scaleAnimation;

    private final AtomicReference<String> query;

    // measure utilities
    protected final Paint paint;
    protected final DisplayMetrics displayMetrics;

    private final boolean simpleLayout;
    private final boolean titleOnlyLayout;
    protected final int minRowHeight;

    private final Map<Long, TaskAction> taskActionLoader = Collections.synchronizedMap(new HashMap<Long, TaskAction>());

    /**
     * Constructor
     *
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param onCompletedTaskListener
     *            task listener. can be null
     */
    public TaskAdapter(TaskListFragment fragment, int resource,
            Cursor c, AtomicReference<String> query, OnCompletedTaskListener onCompletedTaskListener) {
        super(ContextManager.getContext(), c, false);
        DependencyInjectionService.getInstance().inject(this);

        this.context = ContextManager.getContext();
        this.query = query;
        this.resource = resource;
        this.titleOnlyLayout = resource == R.layout.task_adapter_row_title_only;
        this.fragment = fragment;
        this.resources = fragment.getResources();
        this.onCompletedTaskListener = onCompletedTaskListener;
        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 18);
        paint = new Paint();
        displayMetrics = new DisplayMetrics();
        fragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        this.simpleLayout = (resource == R.layout.task_adapter_row_simple);
        this.minRowHeight = computeMinRowHeight();

        startDetailThread();

        scaleAnimation = new ScaleAnimation(1.4f, 1.0f, 1.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(100);

        preloadDrawables(IMPORTANCE_RESOURCES, IMPORTANCE_DRAWABLES);
        preloadDrawables(IMPORTANCE_RESOURCES_CHECKED, IMPORTANCE_DRAWABLES_CHECKED);
        preloadDrawables(IMPORTANCE_RESOURCES_LARGE, IMPORTANCE_DRAWABLES_LARGE);
        preloadDrawables(IMPORTANCE_REPEAT_RESOURCES, IMPORTANCE_REPEAT_DRAWABLES);
        preloadDrawables(IMPORTANCE_REPEAT_RESOURCES_CHECKED, IMPORTANCE_REPEAT_DRAWABLES_CHECKED);
    }

    private void preloadDrawables(int[] resourceIds, Drawable[] drawables) {
        for (int i = 0; i < resourceIds.length; i++) {
            drawables[i] = resources.getDrawable(resourceIds[i]);
        }
    }

    protected int computeMinRowHeight() {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        if (simpleLayout || titleOnlyLayout) {
            return (int) (metrics.density * 40);
        } else {
            return (int) (metrics.density * 45);
        }
    }

    public int computeFullRowHeight() {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        if (fontSize < 16) {
            return (int) (39 * metrics.density);
        } else {
            return minRowHeight + (int) (10 * metrics.density);
        }
    }

    private void startDetailThread() {
        if (Preferences.getBoolean(R.string.p_showNotes, false) && !simpleLayout && !titleOnlyLayout) {
            DetailLoaderThread detailLoader = new DetailLoaderThread();
            detailLoader.start();
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
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        // create view holder
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.task = new Task();
        viewHolder.view = view;
        viewHolder.rowBody = (ViewGroup)view.findViewById(R.id.rowBody);
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.completeBox = (CheckableImageView)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
        viewHolder.tagsView = (TextView)view.findViewById(R.id.tagsDisplay);
        viewHolder.details1 = (TextView)view.findViewById(R.id.details1);
        viewHolder.details2 = (TextView)view.findViewById(R.id.details2);
        viewHolder.taskRow = (LinearLayout)view.findViewById(R.id.task_row);
        viewHolder.taskActionContainer = view.findViewById(R.id.taskActionContainer);
        viewHolder.taskActionIcon = (ImageView)view.findViewById(R.id.taskActionIcon);

        boolean showFullTaskTitle = Preferences.getBoolean(R.string.p_fullTaskTitle, false);
        boolean showNotes = Preferences.getBoolean(R.string.p_showNotes, false);
        if (showFullTaskTitle && !titleOnlyLayout) {
            viewHolder.nameView.setMaxLines(Integer.MAX_VALUE);
            viewHolder.nameView.setSingleLine(false);
            viewHolder.nameView.setEllipsize(null);
        } else if (titleOnlyLayout) {
            viewHolder.nameView.setMaxLines(1);
            viewHolder.nameView.setSingleLine(true);
            viewHolder.nameView.setEllipsize(TruncateAt.END);
        }

        if (showNotes && !simpleLayout && !titleOnlyLayout) {
            RelativeLayout.LayoutParams taskRowParams = (RelativeLayout.LayoutParams)viewHolder.taskRow.getLayoutParams();
            taskRowParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);
        }


        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++) {
            view.getChildAt(i).setTag(viewHolder);
        }
        if(viewHolder.details1 != null) {
            viewHolder.details1.setTag(viewHolder);
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

        if (!titleOnlyLayout) {
            viewHolder.tagsString = cursor.get(TAGS);
            viewHolder.hasFiles = cursor.get(FILE_ID_PROPERTY) > 0;
            viewHolder.hasNotes = cursor.get(HAS_NOTES_PROPERTY) > 0;
        }

        Task task = viewHolder.task;
        task.clear();
        task.readFromCursor(cursor);

        setFieldContentsAndVisibility(view);
        setTaskAppearance(viewHolder, task);
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
        public TextView details1, details2;
        public LinearLayout taskRow;
        public View taskActionContainer;
        public ImageView taskActionIcon;
        public String tagsString; // From join query, not part of the task model
        public boolean hasFiles; // From join query, not part of the task model
        public boolean hasNotes;

        public View[] decorations;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        Task task = viewHolder.task;
        if (fontSize < 16 || titleOnlyLayout) {
            viewHolder.rowBody.setMinimumHeight(0);
            viewHolder.completeBox.setMinimumHeight(0);
        } else {
            viewHolder.rowBody.setMinimumHeight(minRowHeight);
            viewHolder.completeBox.setMinimumHeight(minRowHeight);
        }

        viewHolder.view.setBackgroundColor(resources.getColor(android.R.color.transparent));

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

        if (titleOnlyLayout) {
            return;
        }

        float dueDateTextWidth = setupDueDateAndTags(viewHolder, task);

        String details;
        if(viewHolder.details1 != null) {
            if(taskDetailLoader.containsKey(task.getId())) {
                details = taskDetailLoader.get(task.getId()).toString();
            } else {
                details = task.getDetails();
            }
            if(TextUtils.isEmpty(details) || DETAIL_SEPARATOR.equals(details) || task.isCompleted()) {
                viewHolder.details1.setVisibility(View.GONE);
                viewHolder.details2.setVisibility(View.GONE);
            } else if (Preferences.getBoolean(R.string.p_showNotes, false)) {
                viewHolder.details1.setVisibility(View.VISIBLE);
                if (details.startsWith(DETAIL_SEPARATOR)) {
                    StringBuilder buffer = new StringBuilder(details);
                    int length = DETAIL_SEPARATOR.length();
                    while(buffer.lastIndexOf(DETAIL_SEPARATOR, length) == 0) {
                        buffer.delete(0, length);
                    }
                    details = buffer.toString(); //details.substring(DETAIL_SEPARATOR.length());
                }
                drawDetails(viewHolder, details, dueDateTextWidth);
            }
        }

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

        if(Math.abs(DateUtilities.now() - task.getModificationDate()) < 2000L) {
            mostRecentlyMade = task.getId();
        }

    }

    private TaskAction getTaskAction(Task task, boolean hasFiles, boolean hasNotes) {
        if (titleOnlyLayout || task.isCompleted()) {
            return null;
        }
        if (taskActionLoader.containsKey(task.getId())) {
            return taskActionLoader.get(task.getId());
        } else {
            TaskAction action = LinkActionExposer.getActionsForTask(context, task, hasFiles, hasNotes);
            taskActionLoader.put(task.getId(), action);
            return action;
        }
    }

    private void drawDetails(ViewHolder viewHolder, String details, float rightWidth) {
        SpannableStringBuilder prospective = new SpannableStringBuilder();
        SpannableStringBuilder actual = new SpannableStringBuilder();

        details = details.trim().replace("\n", "<br>");
        String[] splitDetails = details.split("\\|");
        viewHolder.completeBox.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        rightWidth = rightWidth + viewHolder.dueDate.getPaddingRight();
        float left = viewHolder.completeBox.getMeasuredWidth() +
        ((MarginLayoutParams)viewHolder.completeBox.getLayoutParams()).leftMargin;
        int availableWidth = (int) (displayMetrics.widthPixels - left - (rightWidth + 16) * displayMetrics.density);

        int i = 0;
        for(; i < splitDetails.length; i++) {
            Spanned spanned = convertToHtml(splitDetails[i] + "  ", detailImageGetter);
            prospective.insert(prospective.length(), spanned);
            viewHolder.details1.setText(prospective);
            viewHolder.details1.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            if(rightWidth > 0 && viewHolder.details1.getMeasuredWidth() > availableWidth) {
                break;
            }

            actual.insert(actual.length(), spanned);
        }
        viewHolder.details1.setText(actual);
        actual.clear();

        if(i >= splitDetails.length) {
            viewHolder.details2.setVisibility(View.GONE);
            return;
        } else {
            viewHolder.details2.setVisibility(View.VISIBLE);
        }

        for(; i < splitDetails.length; i++) {
            actual.insert(actual.length(), convertToHtml(splitDetails[i] + "  ", detailImageGetter));
        }
        viewHolder.details2.setText(actual);
    }

    protected TaskRowListener listener = new TaskRowListener();

    private Pair<Float, Float> lastTouchYRawY = new Pair<>(0f, 0f);

    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    protected void addListeners(final View container) {
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

        int theme = ThemeService.getEditDialogTheme();
        final Dialog dialog = new Dialog(fragment.getActivity(), theme);
        dialog.setTitle(R.string.TEA_note_label);
        View notesView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.notes_view_dialog, null);
        dialog.setContentView(notesView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

        notesView.findViewById(R.id.edit_dlg_ok).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        final TextView notesField = (TextView) notesView.findViewById(R.id.notes);
        notesField.setText(notes);

        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;
        Configuration config = fragment.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (AndroidUtilities.getSdkVersion() >= 9 && size == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            DisplayMetrics metrics = fragment.getResources().getDisplayMetrics();
            params.width = metrics.widthPixels / 2;
        }
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        dialog.show();
    }

    private void showFilesDialog(Task task) {
        FilesControlSet filesControlSet = new FilesControlSet(fragment.getActivity(), R.layout.control_set_files,
                R.layout.control_set_files_display, R.string.TEA_control_files);
        filesControlSet.readFromTask(task);
        filesControlSet.getDisplayView().performClick();
    }

    /* ======================================================================
     * ============================================================== details
     * ====================================================================== */

    private final HashMap<String, Spanned> htmlCache = new HashMap<>(8);

    private Spanned convertToHtml(String string, ImageGetter imageGetter) {
        if(!htmlCache.containsKey(string)) {
            Spanned html;
            try {
                html = Html.fromHtml(string, imageGetter, null);
            } catch (RuntimeException e) {
                html = Spannable.Factory.getInstance().newSpannable(string);
            }
            htmlCache.put(string, html);
            return html;
        }
        return htmlCache.get(string);
    }

    private final HashMap<Long, String> dateCache = new HashMap<>(8);

    private String formatDate(long date) {
        if(dateCache.containsKey(date)) {
            return dateCache.get(date);
        }

        String formatString = "%s" + (simpleLayout ? " " : "\n") + "%s";
        String string = DateUtilities.getRelativeDay(fragment.getActivity(), date);
        if(Task.hasDueTime(date)) {
            string = String.format(formatString, string, //$NON-NLS-1$
                    DateUtilities.getTimeString(fragment.getActivity(), newDate(date)));
        }

        dateCache.put(date, string);
        return string;
    }

    // implementation note: this map is really costly if users have
    // a large number of tasks to load, since it all goes into memory.
    // it's best to do this, though, in order to append details to each other
    private final Map<Long, StringBuilder> taskDetailLoader = Collections.synchronizedMap(new HashMap<Long, StringBuilder>(0));

    public class DetailLoaderThread extends Thread {
        @Override
        public void run() {
            // for all of the tasks returned by our cursor, verify details
            AndroidUtilities.sleepDeep(500L);
            TodorooCursor<Task> fetchCursor = taskService.fetchFiltered(
                    query.get(), null, Task.ID, Task.TITLE, Task.DETAILS, Task.DETAILS_DATE,
                    Task.MODIFICATION_DATE, Task.COMPLETION_DATE);
            try {
                Random random = new Random();

                Task task = new Task();

                for(fetchCursor.moveToFirst(); !fetchCursor.isAfterLast(); fetchCursor.moveToNext()) {
                    task.clear();
                    task.readFromCursor(fetchCursor);
                    if(task.isCompleted()) {
                        continue;
                    }

                    if(detailsAreRecentAndUpToDate(task)) {
                        // even if we are up to date, randomly load a fraction
                        if(random.nextFloat() < 0.1) {
                            taskDetailLoader.put(task.getId(),
                                    new StringBuilder(task.getDetails()));
                            requestNewDetails(task);
                        }
                        continue;
                    }
                    addTaskToLoadingArray(task);

                    task.setDetails(DETAIL_SEPARATOR);
                    task.setDetailsDate(DateUtilities.now());
                    taskService.save(task);

                    requestNewDetails(task);
                }
                if(taskDetailLoader.size() > 0) {
                    Activity activity = fragment.getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // suppress silently
            } finally {
                fetchCursor.close();
            }
        }

        private boolean detailsAreRecentAndUpToDate(Task task) {
            return task.getDetailsDate() >= task.getModificationDate() &&
            !TextUtils.isEmpty(task.getDetails());
        }

        private void addTaskToLoadingArray(Task task) {
            StringBuilder detailStringBuilder = new StringBuilder();
            taskDetailLoader.put(task.getId(), detailStringBuilder);
        }

        private void requestNewDetails(Task task) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            Activity activity = fragment.getActivity();
            if (activity != null) {
                activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            }
        }
    }

    /**
     * Add detail to a task
     */
    public void addDetails(long id, String detail) {
        final StringBuilder details = taskDetailLoader.get(id);
        if(details == null) {
            return;
        }
        synchronized(details) {
            if(details.toString().contains(detail)) {
                return;
            }
            if(details.length() > 0) {
                details.append(DETAIL_SEPARATOR);
            }
            details.append(detail);
            Task task = new Task();
            task.setId(id);
            task.setDetails(details.toString());
            task.setDetailsDate(DateUtilities.now());
            taskService.save(task);
        }

        Activity activity = fragment.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    private final ImageGetter detailImageGetter = new ImageGetter() {
        private final HashMap<Integer, Drawable> cache =
            new HashMap<>(3);
        @Override
        public Drawable getDrawable(String source) {
            int drawable = 0;
            switch (source) {
                case "silk_clock":
                    drawable = R.drawable.details_alarm;
                    break;
                case "silk_tag_pink":
                    drawable = R.drawable.details_tag;
                    break;
                case "silk_date":
                    drawable = R.drawable.details_repeat;
                    break;
                case "silk_note":
                    drawable = R.drawable.details_note;
                    break;
            }

            if (drawable == 0) {
                drawable = resources.getIdentifier("drawable/" + source, null, Constants.PACKAGE);
            }
            if(drawable == 0) {
                return null;
            }
            Drawable d;
            if(!cache.containsKey(drawable)) {
                d = resources.getDrawable(drawable);
                d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
                cache.put(drawable, d);
            } else {
                d = cache.get(drawable);
            }
            return d;
        }
    };

    /* ======================================================================
     * ============================================================== add-ons
     * ====================================================================== */

    /**
     * Called to tell the cache to be cleared
     */
    public void flushCaches() {
        completedItems.clear();
        taskDetailLoader.clear();
        startDetailThread();
    }

    public HashMap<Object, Boolean> getCompletedItems() {
        return completedItems;
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 18);
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
            if (viewHolder.completeBox.getVisibility() == View.VISIBLE) {
                viewHolder.completeBox.startAnimation(scaleAnimation);
            }
        }
    };

    protected ViewHolder getTagFromCheckBox(View v) {
        return (ViewHolder)((View)v.getParent()).getTag();
    }

    public class TaskRowListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            // expand view (unless deleted)
            final ViewHolder viewHolder = (ViewHolder)v.getTag();
            if(viewHolder.task.isDeleted()) {
                return;
            }

            long taskId = viewHolder.task.getId();
            fragment.onTaskListItemClicked(taskId);
        }
    }

    /** Helper method to adjust a tasks' appearance if the task is completed or
     * uncompleted.
     */
    protected void setTaskAppearance(ViewHolder viewHolder, Task task) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        // show item as completed if it was recently checked
        Boolean value = completedItems.get(task.getUuid());
        if (value == null) {
            value = completedItems.get(task.getId());
        }
        if(value != null) {
            task.setCompletionDate(
                    value ? DateUtilities.now() : 0);
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

        if (!titleOnlyLayout) {
            setupDueDateAndTags(viewHolder, task);

            float detailTextSize = Math.max(10, fontSize * 14 / 20);
            if(viewHolder.details1 != null) {
                viewHolder.details1.setTextSize(detailTextSize);
            }
            if(viewHolder.details2 != null) {
                viewHolder.details2.setTextSize(detailTextSize);
            }
            if(viewHolder.dueDate != null) {
                viewHolder.dueDate.setTextSize(detailTextSize);
                if (simpleLayout) {
                    viewHolder.dueDate.setTypeface(null, 0);
                }
            }
            if (viewHolder.tagsView != null) {
                viewHolder.tagsView.setTextSize(detailTextSize);
                if (simpleLayout) {
                    viewHolder.tagsView.setTypeface(null, 0);
                }
            }
            paint.setTextSize(detailTextSize);
        }

        setupCompleteBox(viewHolder);

    }

    private void setupCompleteBox(ViewHolder viewHolder) {
     // complete box
        final Task task = viewHolder.task;
        final CheckableImageView checkBoxView = viewHolder.completeBox; {
            boolean completed = task.isCompleted();
            checkBoxView.setChecked(completed);
            checkBoxView.setEnabled(true);

            int value = task.getImportance();
            if (value >= IMPORTANCE_RESOURCES.length) {
                value = IMPORTANCE_RESOURCES.length - 1;
            }
            Drawable[] boxes;
            if (!TextUtils.isEmpty(task.getRecurrence())) {
                boxes = completed ? IMPORTANCE_REPEAT_DRAWABLES_CHECKED : IMPORTANCE_REPEAT_DRAWABLES;
            } else {
                boxes = completed ? IMPORTANCE_DRAWABLES_CHECKED : IMPORTANCE_DRAWABLES;
            }
            checkBoxView.setImageDrawable(boxes[value]);
            if (titleOnlyLayout) {
                return;
            }

            checkBoxView.setVisibility(View.VISIBLE);
        }
    }

    // Returns due date text width
    private float setupDueDateAndTags(ViewHolder viewHolder, Task task) {
        // due date / completion date
        float dueDateTextWidth = 0;
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
                    String dateValue = formatDate(dueDate);
                    dueDateView.setText(dateValue);
                    dueDateTextWidth = paint.measureText(dateValue);
                    dueDateView.setVisibility(View.VISIBLE);
                } else if(task.isCompleted()) {
                    String dateValue = formatDate(task.getCompletionDate());
                    dueDateView.setText(resources.getString(R.string.TAd_completed, dateValue));
                    dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Completed);
                    dueDateTextWidth = paint.measureText(dateValue);
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
        return dueDateTextWidth;
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

            completedItems.put(task.getUuid(), newState);
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
