package com.todoroo.astrid.adapter;

import com.todoroo.astrid.helper.AsyncImageView;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
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
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.core.LinkActionExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskAdapterAddOnManager;
import com.todoroo.astrid.notes.NotesDecorationExposer;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.taskrabbit.TaskRabbitDataService;
import com.todoroo.astrid.taskrabbit.TaskRabbitTaskContainer;
import com.todoroo.astrid.timers.TimerDecorationExposer;
import com.todoroo.astrid.ui.CheckableImageView;
import com.todoroo.astrid.utility.Constants;

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

    public static final String BROADCAST_EXTRA_TASK = "model"; //$NON-NLS-1$

    // --- other constants

    /** Properties that need to be read from the action item */
    public static final Property<?>[] PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.TITLE,
        Task.FLAGS,
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
        Task.NOTES,
        Task.USER_ID,
        Task.USER
    };

    public static int[] IMPORTANCE_RESOURCES = new int[] {
        R.drawable.importance_check_1,
        R.drawable.importance_check_2,
        R.drawable.importance_check_3,
        R.drawable.importance_check_4,
    };

    public static int[] LEGACY_IMPORTANCE_RESOURCES = new int[] {
        R.drawable.importance_1,
        R.drawable.importance_2,
        R.drawable.importance_3,
        R.drawable.importance_4,
    };

    public static int[] IMPORTANCE_RESOURCES_LARGE = new int[] {
        R.drawable.check_box_large_1,
        R.drawable.check_box_large_2,
        R.drawable.check_box_large_3,
        R.drawable.check_box_large_4,
    };

    public static int[] IMPORTANCE_REPEAT_RESOURCES = new int[] {
        R.drawable.importance_check_repeat_1,
        R.drawable.importance_check_repeat_2,
        R.drawable.importance_check_repeat_3,
        R.drawable.importance_check_repeat_4,
    };

    // --- instance variables

    @Autowired
    private TaskService taskService;

    public static int APPLY_LISTENERS_PARENT = 0;
    public static int APPLY_LISTENERS_ROW_BODY= 1;
    public static int APPLY_LISTENERS_NONE = 2;

    protected final TaskListFragment fragment;
    protected final Resources resources;
    protected final HashMap<Long, Boolean> completedItems = new HashMap<Long, Boolean>(0);
    protected OnCompletedTaskListener onCompletedTaskListener = null;
    public boolean isFling = false;
    private final int resource;
    private final LayoutInflater inflater;
    private DetailLoaderThread detailLoader;
    private ActionsLoaderThread actionsLoader;
    private int fontSize;
    protected int applyListeners = APPLY_LISTENERS_PARENT;
    private long mostRecentlyMade = -1;
    private final ScaleAnimation scaleAnimation;

    private final AtomicReference<String> query;

    private final int minRowHeight;

    // measure utilities
    protected final Paint paint;
    protected final DisplayMetrics displayMetrics;

    // --- task detail and decoration soft caches

    public final DecorationManager decorationManager;

    /**
     * Constructor
     *
     * @param fragment
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param autoRequery
     *            whether cursor is automatically re-queried on changes
     * @param onCompletedTaskListener
     *            task listener. can be null
     */
    public TaskAdapter(TaskListFragment fragment, int resource,
            Cursor c, AtomicReference<String> query, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(ContextManager.getContext(), c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.query = query;
        this.resource = resource;
        this.fragment = fragment;
        this.resources = fragment.getResources();
        this.onCompletedTaskListener = onCompletedTaskListener;

        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 18);
        paint = new Paint();
        displayMetrics = new DisplayMetrics();
        fragment.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        this.minRowHeight = (int) (45 * displayMetrics.density);

        startDetailThread();
        startTaskActionsThread();

        decorationManager = new DecorationManager();

        scaleAnimation = new ScaleAnimation(1.4f, 1.0f, 1.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(100);

    }

    private void startDetailThread() {
        if (Preferences.getBoolean(R.string.p_showNotes, false)) {
            detailLoader = new DetailLoaderThread();
            detailLoader.start();
        }
    }

    private void startTaskActionsThread() {
        actionsLoader = new ActionsLoaderThread();
        actionsLoader.start();
    }

    /* ======================================================================
     * =========================================================== filterable
     * ====================================================================== */

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        // perform query
        TodorooCursor<Task> newCursor = taskService.fetchFiltered(
                query.get(), constraint, TaskAdapter.PROPERTIES);
        return newCursor;
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
        viewHolder.importance = view.findViewById(R.id.importance_legacy);
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.picture = (AsyncImageView)view.findViewById(R.id.picture);
        viewHolder.pictureBorder = (ImageView)view.findViewById(R.id.pictureBorder);
        viewHolder.completeBox = (CheckableImageView)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
        viewHolder.details1 = (TextView)view.findViewById(R.id.details1);
        viewHolder.details2 = (TextView)view.findViewById(R.id.details2);
        viewHolder.taskRow = (LinearLayout)view.findViewById(R.id.task_row);
        viewHolder.taskActionContainer = view.findViewById(R.id.taskActionContainer);
        viewHolder.taskActionIcon = (ImageView)view.findViewById(R.id.taskActionIcon);

        boolean showFullTaskTitle = Preferences.getBoolean(R.string.p_fullTaskTitle, false);
        boolean showNotes = Preferences.getBoolean(R.string.p_showNotes, false);
        if (showFullTaskTitle) {
            viewHolder.nameView.setMaxLines(Integer.MAX_VALUE);
        }

        if (showNotes) {
            RelativeLayout.LayoutParams taskRowParams = (RelativeLayout.LayoutParams)viewHolder.taskRow.getLayoutParams();
            taskRowParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);
        }


        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++)
            view.getChildAt(i).setTag(viewHolder);
        if(viewHolder.details1 != null)
            viewHolder.details1.setTag(viewHolder);

        // add UI component listeners
        addListeners(view);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
        ViewHolder viewHolder = ((ViewHolder)view.getTag());

        Task task = viewHolder.task;
        task.clear();
        task.readFromCursor(cursor);

        setFieldContentsAndVisibility(view);
        setTaskAppearance(viewHolder, task);
    }

    /** Helper method to set the visibility based on if there's stuff inside */
    private static void setVisibility(TextView v) {
        if(v.getText().length() > 0)
            v.setVisibility(View.VISIBLE);
        else
            v.setVisibility(View.GONE);
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
        public View importance; // for legacy importance style
        public CheckableImageView completeBox;
        public AsyncImageView picture;
        public ImageView pictureBorder;
        public TextView dueDate;
        public TextView details1, details2;
        public LinearLayout taskRow;
        public View taskActionContainer;
        public ImageView taskActionIcon;

        public View[] decorations;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        Task task = viewHolder.task;

        if (Preferences.getBoolean(R.string.p_allowCompressedTaskRows, false)) {
            viewHolder.rowBody.setMinimumHeight(0);
        } else {
            viewHolder.rowBody.setMinimumHeight(minRowHeight);
        }

        // name
        final TextView nameView = viewHolder.nameView; {
            String nameValue = task.getValue(Task.TITLE);

            long hiddenUntil = task.getValue(Task.HIDE_UNTIL);
            if(task.getValue(Task.DELETION_DATE) > 0)
                nameValue = resources.getString(R.string.TAd_deletedFormat, nameValue);
            if(hiddenUntil > DateUtilities.now())
                nameValue = resources.getString(R.string.TAd_hiddenFormat, nameValue);
            nameView.setText(nameValue);
        }

        // due date / completion date
        float dueDateTextWidth = 0;
        final TextView dueDateView = viewHolder.dueDate; {
            if(!task.isCompleted() && task.hasDueDate()) {
                long dueDate = task.getValue(Task.DUE_DATE);
                if(dueDate > DateUtilities.now())
                    dueDateView.setTextAppearance(fragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate);
                else
                    dueDateView.setTextAppearance(fragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate_Overdue);
                String dateValue = formatDate(dueDate);
                dueDateView.setText(dateValue);
                dueDateTextWidth = paint.measureText(dateValue);
                setVisibility(dueDateView);
            } else if(task.isCompleted()) {
                String dateValue = formatDate(task.getValue(Task.COMPLETION_DATE));
                dueDateView.setText(resources.getString(R.string.TAd_completed, dateValue));
                dueDateView.setTextAppearance(fragment.getActivity(), R.style.TextAppearance_TAd_ItemDueDate_Completed);
                dueDateTextWidth = paint.measureText(dateValue);
                setVisibility(dueDateView);
            } else {
                dueDateView.setVisibility(View.GONE);
            }
        }

        // complete box
        final CheckableImageView completeBox = viewHolder.completeBox; {
            // show item as completed if it was recently checked
            if(completedItems.get(task.getId()) != null) {
                task.setValue(Task.COMPLETION_DATE,
                        completedItems.get(task.getId()) ? DateUtilities.now() : 0);
            }
            completeBox.setChecked(task.isCompleted());
            // disable checkbox if task is readonly
            completeBox.setEnabled(!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY));
        }

        // image view
        final AsyncImageView pictureView = viewHolder.picture; {
            if (pictureView != null) {
                TaskRabbitTaskContainer container = TaskRabbitDataService.getInstance().getContainerForTask(task);

                if(task.getValue(Task.USER_ID) == Task.USER_ID_SELF && !container.isTaskRabbit()) {
                    pictureView.setVisibility(View.GONE);
                    if (viewHolder.pictureBorder != null)
                        viewHolder.pictureBorder.setVisibility(View.GONE);
                } else {
                    pictureView.setVisibility(View.VISIBLE);
                    if (viewHolder.pictureBorder != null)
                        viewHolder.pictureBorder.setVisibility(View.VISIBLE);
                    pictureView.setUrl(null);
                    if (container.isTaskRabbit()) {
                        pictureView.setDefaultImageResource(R.drawable.task_rabbit_image);
                    } else if(task.getValue(Task.USER_ID) == Task.USER_ID_UNASSIGNED)
                        pictureView.setDefaultImageResource(R.drawable.icn_anyone_transparent);
                    else {
                        pictureView.setDefaultImageResource(R.drawable.icn_default_person_image);
                        try {
                            JSONObject user = new JSONObject(task.getValue(Task.USER));
                            pictureView.setUrl(user.optString("picture")); //$NON-NLS-1$
                        } catch (JSONException e) {
                            Log.w("astrid", "task-adapter-image", e); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }
            }
        }

        // importance bar
        final CheckableImageView checkBoxView = viewHolder.completeBox; {
            // Logic for legacy style
            boolean useLegacyImportance = Preferences.getBoolean(R.string.p_useLegacyImportanceStyle, false);
            if (useLegacyImportance) {
                viewHolder.importance.setVisibility(View.VISIBLE);
            } else {
                viewHolder.importance.setVisibility(View.GONE);
            }

            int value = task.getValue(Task.IMPORTANCE);
            if (value >= IMPORTANCE_RESOURCES.length)
                value = IMPORTANCE_RESOURCES.length - 1;
            if (useLegacyImportance) {
                checkBoxView.setImageResource(R.drawable.btn_check);
                viewHolder.importance.setBackgroundResource(LEGACY_IMPORTANCE_RESOURCES[value]);
            } else if (!TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
                checkBoxView.setImageResource(IMPORTANCE_REPEAT_RESOURCES[value]);
            } else {
                checkBoxView.setImageResource(IMPORTANCE_RESOURCES[value]);
            }
            if (pictureView != null && pictureView.getVisibility() == View.VISIBLE) {
                checkBoxView.setVisibility(View.INVISIBLE);
                if (viewHolder.pictureBorder != null && !useLegacyImportance)
                    viewHolder.pictureBorder.setBackgroundResource(IMPORTANCE_RESOURCES_LARGE[value]);
            } else {
                checkBoxView.setVisibility(View.VISIBLE);
            }
        }

        String details;
        if(viewHolder.details1 != null) {
            if(taskDetailLoader.containsKey(task.getId()))
                details = taskDetailLoader.get(task.getId()).toString();
            else
                details = task.getValue(Task.DETAILS);
            if(TextUtils.isEmpty(details) || DETAIL_SEPARATOR.equals(details) || task.isCompleted()) {
                viewHolder.details1.setVisibility(View.GONE);
                viewHolder.details2.setVisibility(View.GONE);
            } else if (Preferences.getBoolean(R.string.p_showNotes, false)) {
                viewHolder.details1.setVisibility(View.VISIBLE);
                if (details.startsWith(DETAIL_SEPARATOR)) {
                    StringBuffer buffer = new StringBuffer(details);
                    int length = DETAIL_SEPARATOR.length();
                    while(buffer.lastIndexOf(DETAIL_SEPARATOR, length) == 0)
                        buffer.delete(0, length);
                    details = buffer.toString(); //details.substring(DETAIL_SEPARATOR.length());
                }
                drawDetails(viewHolder, details, dueDateTextWidth);
            }
        }

        // Task action
        ImageView taskAction = viewHolder.taskActionIcon;
        if (taskAction != null) {
            if (taskActionLoader.containsKey(task.getId())) {
                taskAction.setVisibility(View.VISIBLE);
                TaskAction action = taskActionLoader.get(task.getId());
                taskAction.setImageBitmap(action.icon);
                taskAction.setTag(action);
            } else {
                taskAction.setVisibility(View.GONE);
                taskAction.setTag(null);
            }
        }

        if(Math.abs(DateUtilities.now() - task.getValue(Task.MODIFICATION_DATE)) < 2000L)
            mostRecentlyMade = task.getId();

        //        // details and decorations, expanded


        if (Preferences.getBoolean(R.string.p_default_showdecorations_key, false)) {
            decorationManager.request(viewHolder);
        }

    }

    @SuppressWarnings("nls")
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
            Spanned spanned = convertToHtml(splitDetails[i] + "  ", detailImageGetter, null);
            prospective.insert(prospective.length(), spanned);
            viewHolder.details1.setText(prospective);
            viewHolder.details1.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            if(rightWidth > 0 && viewHolder.details1.getMeasuredWidth() > availableWidth)
                break;

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

        for(; i < splitDetails.length; i++)
            actual.insert(actual.length(), convertToHtml(splitDetails[i] + "  ", detailImageGetter, null));
        viewHolder.details2.setText(actual);
    }

    protected TaskRowListener listener = new TaskRowListener();

    private Pair<Float, Float> lastTouchYRawY = new Pair<Float, Float>(0f, 0f);

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
                lastTouchYRawY = new Pair<Float, Float>(event.getY(), event.getRawY());
                return false;
            }
        };
        viewHolder.completeBox.setOnTouchListener(otl);
        viewHolder.completeBox.setOnClickListener(completeBoxListener);

        viewHolder.taskActionContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskAction action = (TaskAction) viewHolder.taskActionIcon.getTag();
                if (action != null) {
                    try {
                        action.intent.send();
                    } catch (CanceledException e) {
                        // Oh well
                    }
                }
            }
        });

        if(applyListeners == APPLY_LISTENERS_ROW_BODY) {
            viewHolder.rowBody.setOnCreateContextMenuListener(listener);
            viewHolder.rowBody.setOnClickListener(listener);
        } else if(applyListeners == APPLY_LISTENERS_PARENT) {
            container.setOnCreateContextMenuListener(listener);
            container.setOnClickListener(listener);
        }
    }

    /* ======================================================================
     * ============================================================== details
     * ====================================================================== */

    private final HashMap<String, Spanned> htmlCache = new HashMap<String, Spanned>(8);

    private Spanned convertToHtml(String string, ImageGetter imageGetter, TagHandler tagHandler) {
        if(!htmlCache.containsKey(string)) {
            Spanned html;
            try {
                html = Html.fromHtml(string, imageGetter, tagHandler);
            } catch (RuntimeException e) {
                html = Spannable.Factory.getInstance().newSpannable(string);
            }
            htmlCache.put(string, html);
            return html;
        }
        return htmlCache.get(string);
    }

    private final HashMap<Long, String> dateCache = new HashMap<Long, String>(8);

    private String formatDate(long date) {
        if(dateCache.containsKey(date))
            return dateCache.get(date);

        String string = DateUtilities.getRelativeDay(fragment.getActivity(), date);
        if(Task.hasDueTime(date))
            string = String.format("%s\n%s", string, //$NON-NLS-1$
                    DateUtilities.getTimeString(fragment.getActivity(), new Date(date)));

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
                    if(task.isCompleted())
                        continue;

                    if(detailsAreRecentAndUpToDate(task)) {
                        // even if we are up to date, randomly load a fraction
                        if(random.nextFloat() < 0.1) {
                            taskDetailLoader.put(task.getId(),
                                    new StringBuilder(task.getValue(Task.DETAILS)));
                            requestNewDetails(task);
                            if(Constants.DEBUG)
                                System.err.println("Refreshing details: " + task.getId()); //$NON-NLS-1$
                        }
                        continue;
                    } else if(Constants.DEBUG) {
                        System.err.println("Forced loading of details: " + task.getId() + //$NON-NLS-1$
                                "\n  details: " + new Date(task.getValue(Task.DETAILS_DATE)) + //$NON-NLS-1$
                                "\n  modified: " + new Date(task.getValue(Task.MODIFICATION_DATE))); //$NON-NLS-1$
                    }
                    addTaskToLoadingArray(task);

                    task.setValue(Task.DETAILS, DETAIL_SEPARATOR);
                    task.setValue(Task.DETAILS_DATE, DateUtilities.now());
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
            return task.getValue(Task.DETAILS_DATE) >= task.getValue(Task.MODIFICATION_DATE) &&
            !TextUtils.isEmpty(task.getValue(Task.DETAILS));
        }

        private void addTaskToLoadingArray(Task task) {
            StringBuilder detailStringBuilder = new StringBuilder();
            taskDetailLoader.put(task.getId(), detailStringBuilder);
        }

        private void requestNewDetails(Task task) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            Activity activity = fragment.getActivity();
            if (activity != null)
                activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }
    }


    private final Map<Long, TaskAction> taskActionLoader = Collections.synchronizedMap(new HashMap<Long, TaskAction>());
    public class ActionsLoaderThread extends Thread {
        @Override
        public void run() {
            AndroidUtilities.sleepDeep(500L);
            TodorooCursor<Task> fetchCursor = taskService.fetchFiltered(
                    query.get(), null, Task.ID, Task.TITLE, Task.DETAILS, Task.DETAILS_DATE,
                    Task.MODIFICATION_DATE, Task.COMPLETION_DATE);
            try {
                Task task = new Task();
                LinkActionExposer linkActionExposer = new LinkActionExposer();

                for(fetchCursor.moveToFirst(); !fetchCursor.isAfterLast(); fetchCursor.moveToNext()) {
                    task.clear();
                    task.readFromCursor(fetchCursor);
                    if(task.isCompleted())
                        continue;

                    List<TaskAction> actions = linkActionExposer.getActionsForTask(ContextManager.getContext(), task.getId());
                    if (actions.size() > 0)
                        taskActionLoader.put(task.getId(), actions.get(0));
                }
                if(taskActionLoader.size() > 0) {
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
            } finally {
                fetchCursor.close();
            }
        }
    }

    /**
     * Add detail to a task
     *
     * @param id
     * @param detail
     */
    public void addDetails(long id, String detail) {
        final StringBuilder details = taskDetailLoader.get(id);
        if(details == null)
            return;
        synchronized(details) {
            if(details.toString().contains(detail))
                return;
            if(details.length() > 0)
                details.append(DETAIL_SEPARATOR);
            details.append(detail);
            Task task = new Task();
            task.setId(id);
            task.setValue(Task.DETAILS, details.toString());
            task.setValue(Task.DETAILS_DATE, DateUtilities.now());
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
            new HashMap<Integer, Drawable>(3);
        @SuppressWarnings("nls")
        public Drawable getDrawable(String source) {
            if(source.equals("silk_clock"))
                source = "details_alarm";
            else if(source.equals("silk_tag_pink"))
                source = "details_tag";
            else if(source.equals("silk_date"))
                source = "details_repeat";
            else if(source.equals("silk_note"))
                source = "details_note";

            int drawable = resources.getIdentifier("drawable/" + source, null, Constants.PACKAGE);
            if(drawable == 0)
                return null;
            Drawable d;
            if(!cache.containsKey(drawable)) {
                d = resources.getDrawable(drawable);
                d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
                cache.put(drawable, d);
            } else
                d = cache.get(drawable);
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
        decorationManager.clearCache();
        taskDetailLoader.clear();
        taskActionLoader.clear();
        startDetailThread();
        startTaskActionsThread();
    }

    /**
     * Called to tell the cache to be cleared
     */
    public void flushSpecific(long taskId) {
        completedItems.put(taskId, null);
        decorationManager.clearCache(taskId);
        taskDetailLoader.remove(taskId);
        taskActionLoader.remove(taskId);
    }

    public HashMap<Long, Boolean> getCompletedItems() {
        return completedItems;
    }

    /**
     * AddOnManager for TaskDecorations
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DecorationManager extends TaskAdapterAddOnManager<TaskDecoration> {

        public DecorationManager() {
            super(fragment);
        }

        private final TaskDecorationExposer[] exposers = new TaskDecorationExposer[] {
                new TimerDecorationExposer(),
                new NotesDecorationExposer()
        };

        /**
         * Request add-ons for the given task
         * @return true if cache miss, false if cache hit
         */
        @Override
        public boolean request(ViewHolder viewHolder) {
            long taskId = viewHolder.task.getId();

            Collection<TaskDecoration> list = initialize(taskId);
            if(list != null) {
                draw(viewHolder, taskId, list);
                return false;
            }

            // request details
            draw(viewHolder, taskId, get(taskId));

            for(TaskDecorationExposer exposer : exposers) {
                TaskDecoration deco = exposer.expose(viewHolder.task);
                if(deco != null) {
                    addNew(viewHolder.task.getId(), exposer.getAddon(), deco, viewHolder);
                }
            }

            return true;
        }

        @Override
        protected void draw(ViewHolder viewHolder, long taskId, Collection<TaskDecoration> decorations) {
            if(decorations == null || viewHolder.task.getId() != taskId)
                return;

            reset(viewHolder, taskId);
            if(decorations.size() == 0)
                return;


            int i = 0;
            boolean colorSet = false;
            if(viewHolder.decorations == null || viewHolder.decorations.length != decorations.size())
                viewHolder.decorations = new View[decorations.size()];
            for(TaskDecoration decoration : decorations) {
                if(decoration.color != 0 && !colorSet) {
                    colorSet = true;
                    viewHolder.view.setBackgroundColor(decoration.color);
                }
                if(decoration.decoration != null) {
                    View view = decoration.decoration.apply(fragment.getActivity(), viewHolder.taskRow);
                    viewHolder.decorations[i] = view;
                    switch(decoration.position) {
                    case TaskDecoration.POSITION_LEFT: {
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.BELOW, R.id.completeBox);
                        view.setLayoutParams(params);
                        viewHolder.rowBody.addView(view);
                        break;
                    }
                    case TaskDecoration.POSITION_RIGHT:
                        viewHolder.taskRow.addView(view, viewHolder.taskRow.getChildCount());
                    }
                }
                i++;
            }
        }

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            if(viewHolder.decorations != null) {
                for(View view : viewHolder.decorations) {
                    viewHolder.rowBody.removeView(view);
                    viewHolder.taskRow.removeView(view);
                }
                viewHolder.decorations = null;
            }
            if(viewHolder.task.getId() == mostRecentlyMade)
                viewHolder.view.setBackgroundColor(Color.argb(30, 150, 150, 150));
            else
                viewHolder.view.setBackgroundResource(android.R.drawable.list_selector_background);
        }

        @Override
        protected Intent createBroadcastIntent(Task task) {
            return null;
        }
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
            viewHolder.completeBox.startAnimation(scaleAnimation);
        }
    };

    protected ViewHolder getTagFromCheckBox(View v) {
        return (ViewHolder)((View)v.getParent()).getTag();
    }

    public class TaskRowListener implements OnCreateContextMenuListener, OnClickListener {

        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            // this is all a big sham. it's actually handled in Task List
            // Activity. however, we need this to be here.
        }

        @Override
        public void onClick(View v) {
            // expand view (unless deleted)
            final ViewHolder viewHolder = (ViewHolder)v.getTag();
            if(viewHolder.task.isDeleted())
                return;

            long taskId = viewHolder.task.getId();
            if (!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY)) {
                editTask(taskId);
            }
        }
    }

    private void editTask(long taskId) {
        fragment.onTaskListItemClicked(taskId);
    }

    /**
     * Call me when the parent presses trackpad
     */
    public void onTrackpadPressed(View container) {
        if(container == null)
            return;

        final CheckBox completeBox = ((CheckBox)container.findViewById(R.id.completeBox));
        completeBox.performClick();
    }

    /** Helper method to adjust a tasks' appearance if the task is completed or
     * uncompleted.
     *
     * @param actionItem
     * @param name
     * @param progress
     */
    void setTaskAppearance(ViewHolder viewHolder, Task task) {
        boolean state = task.isCompleted();

        viewHolder.completeBox.setChecked(state);
        viewHolder.completeBox.setEnabled(!viewHolder.task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY));

        TextView name = viewHolder.nameView;
        if(state) {
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(fragment.getActivity(), R.style.TextAppearance_TAd_ItemTitle_Completed);
        } else {
            name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(fragment.getActivity(), R.style.TextAppearance_TAd_ItemTitle);
        }
        name.setTextSize(fontSize);
        float detailTextSize = Math.max(10, fontSize * 12 / 20);
        if(viewHolder.details1 != null)
            viewHolder.details1.setTextSize(detailTextSize);
        if(viewHolder.details2 != null)
            viewHolder.details2.setTextSize(detailTextSize);
        if(viewHolder.dueDate != null)
            viewHolder.dueDate.setTextSize(detailTextSize);
        paint.setTextSize(detailTextSize);
    }

    /**
     * This method is called when user completes a task via check box or other
     * means
     *
     * @param container
     *            container for the action item
     * @param newState
     *            state that this task should be set to
     * @param completeBox
     *            the box that was clicked. can be null
     */
    protected void completeTask(final Task task, final boolean newState) {
        if(task == null)
            return;

        if (newState != task.isCompleted()) {
            if(onCompletedTaskListener != null)
                onCompletedTaskListener.onCompletedTask(task, newState);

            completedItems.put(task.getId(), newState);
            taskService.setComplete(task, newState);

            if(newState)
                StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_V2);
        }
    }

    /**
     * Add a new listener
     * @param newListener
     */
    public void addOnCompletedTaskListener(final OnCompletedTaskListener newListener) {
        if(this.onCompletedTaskListener == null)
            this.onCompletedTaskListener = newListener;
        else {
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
