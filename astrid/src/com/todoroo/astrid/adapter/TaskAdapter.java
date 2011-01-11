package com.todoroo.astrid.adapter;

import greendroid.widget.QuickAction;
import greendroid.widget.QuickActionBar;
import greendroid.widget.QuickActionWidget;
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskAdapterAddOnManager;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerDecorationExposer;
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
        Task.IMPORTANCE,
        Task.DUE_DATE,
        Task.COMPLETION_DATE,
        Task.HIDE_UNTIL,
        Task.DELETION_DATE,
        Task.DETAILS,
        Task.ELAPSED_SECONDS,
        Task.TIMER_START,
    };

    private static int[] IMPORTANCE_COLORS = null;

    // --- instance variables

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private TaskService taskService;

    protected final ListActivity activity;
    protected final HashMap<Long, Boolean> completedItems = new HashMap<Long, Boolean>(0);
    private OnCompletedTaskListener onCompletedTaskListener = null;
    public boolean isFling = false;
    private final int resource;
    private final LayoutInflater inflater;
    private int fontSize;
    private DetailLoaderThread detailLoader;

    private final AtomicReference<String> query;

    // the task that's expanded
    private long expanded = -1;

    // actions for QuickActionBar/mel
    private QuickActionWidget mBar;
    //private View mBarAnchor;

    // --- task detail and decoration soft caches

    public final ExtendedDetailManager extendedDetailManager;
    public final DecorationManager decorationManager;
    public final TaskActionManager taskActionManager;

    /**
     * Constructor
     *
     * @param activity
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     * @param autoRequery
     *            whether cursor is automatically re-queried on changes
     * @param onCompletedTaskListener
     *            task listener. can be null
     */
    public TaskAdapter(ListActivity activity, int resource,
            Cursor c, AtomicReference<String> query, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(activity, c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.query = query;
        this.resource = resource;
        this.activity = activity;
        this.onCompletedTaskListener = onCompletedTaskListener;

        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 20);

        synchronized(TaskAdapter.class) {
            if(IMPORTANCE_COLORS == null)
                IMPORTANCE_COLORS = Task.getImportanceColors(activity.getResources());
        }

        detailLoader = new DetailLoaderThread();
        detailLoader.start();

        extendedDetailManager = new ExtendedDetailManager();
        decorationManager = new DecorationManager();
        taskActionManager = new TaskActionManager();
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
        activity.startManagingCursor(newCursor);
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
        viewHolder.nameView = (TextView)view.findViewById(R.id.title);
        viewHolder.completeBox = (CheckBox)view.findViewById(R.id.completeBox);
        viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
        viewHolder.details = (TextView)view.findViewById(R.id.details);
        viewHolder.extendedDetails = (TextView)view.findViewById(R.id.extendedDetails);
        viewHolder.actions = (LinearLayout)view.findViewById(R.id.actions);
        viewHolder.taskRow = (LinearLayout)view.findViewById(R.id.task_row);
        viewHolder.importance = (View)view.findViewById(R.id.importance);

        view.setTag(viewHolder);
        for(int i = 0; i < view.getChildCount(); i++)
            view.getChildAt(i).setTag(viewHolder);
        if(viewHolder.details != null)
            viewHolder.details.setTag(viewHolder);

        // add UI component listeners
        addListeners(view);

        // populate view content
        bindView(view, context, cursor);

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
        public View view;
        public TextView nameView;
        public CheckBox completeBox;
        public TextView dueDate;
        public TextView details;
        public TextView extendedDetails;
        public View importance;
        public LinearLayout actions;
        public LinearLayout taskRow;

        public View[] decorations;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view) {
        Resources r = activity.getResources();
        ViewHolder viewHolder = (ViewHolder)view.getTag();
        Task task = viewHolder.task;

        // name
        final TextView nameView = viewHolder.nameView; {
            String nameValue = task.getValue(Task.TITLE);
            long hiddenUntil = task.getValue(Task.HIDE_UNTIL);
            if(task.getValue(Task.DELETION_DATE) > 0)
                nameValue = r.getString(R.string.TAd_deletedFormat, nameValue);
            if(hiddenUntil > DateUtilities.now())
                nameValue = r.getString(R.string.TAd_hiddenFormat, nameValue);
            nameView.setText(nameValue);

            nameView.setMovementMethod(null);
            if(nameValue.contains(".") || nameValue.contains("-")) //$NON-NLS-1$ //$NON-NLS-2$
                Linkify.addLinks(nameView, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS |
                    Linkify.WEB_URLS);
        }

        // due date / completion date
        final TextView dueDateView = viewHolder.dueDate; {
            if(!task.isCompleted() && task.hasDueDate()) {
                long dueDate = task.getValue(Task.DUE_DATE);
                long secondsLeft = dueDate - DateUtilities.now();
                if(secondsLeft > 0) {
                    dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate);
                } else {
                    dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Overdue);
                }

                String dateValue = formatDate(dueDate);
                dueDateView.setText(dateValue);
                setVisibility(dueDateView);
            } else if(task.isCompleted()) {
                String dateValue = DateUtilities.getDateStringWithTime(activity, new Date(task.getValue(Task.COMPLETION_DATE)));
                dueDateView.setText(r.getString(R.string.TAd_completed, dateValue));
                dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Completed);
                setVisibility(dueDateView);
            } else {
                dueDateView.setVisibility(View.GONE);
            }
        }

        // complete box
        final CheckBox completeBox = viewHolder.completeBox; {
            // show item as completed if it was recently checked
            if(completedItems.get(task.getId()) != null) {
                task.setValue(Task.COMPLETION_DATE,
                        completedItems.get(task.getId()) ? DateUtilities.now() : 0);
            }
            completeBox.setChecked(task.isCompleted());
        }

        // importance bar
        final View importanceView = viewHolder.importance; {
            int value = task.getValue(Task.IMPORTANCE);
            if(value < IMPORTANCE_COLORS.length)
                importanceView.setBackgroundColor(IMPORTANCE_COLORS[value]);
            else
                importanceView.setBackgroundColor(0);
        }

        String details;
        if(viewHolder.details != null) {
            if(taskDetailLoader.containsKey(task.getId()))
                details = taskDetailLoader.get(task.getId()).toString();
            else
                details = task.getValue(Task.DETAILS);
            if(TextUtils.isEmpty(details) || DETAIL_SEPARATOR.equals(details) || task.isCompleted()) {
                viewHolder.details.setVisibility(View.GONE);
            } else {
                viewHolder.details.setVisibility(View.VISIBLE);
                while(details.startsWith(DETAIL_SEPARATOR))
                    details = details.substring(DETAIL_SEPARATOR.length());
                viewHolder.details.setText(convertToHtml(details.trim().replace("\n", //$NON-NLS-1$
                        "<br>"), detailImageGetter, null)); //$NON-NLS-1$
            }
        }

        // details and decorations, expanded
        decorationManager.request(viewHolder);
        if(!isFling && expanded == task.getId()) {
            if(viewHolder.extendedDetails != null)
                extendedDetailManager.request(viewHolder);
            taskActionManager.request(viewHolder);
        } else {
            if(viewHolder.extendedDetails != null)
                viewHolder.extendedDetails.setVisibility(View.GONE);
            viewHolder.actions.setVisibility(View.GONE);
        }
    }

    protected TaskRowListener listener = new TaskRowListener();
    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    protected void addListeners(final View container) {
        ViewHolder viewHolder = (ViewHolder)container.getTag();

        // check box listener
        viewHolder.completeBox.setOnClickListener(completeBoxListener);

        // context menu listener
        container.setOnCreateContextMenuListener(listener);

        // tap listener
        container.setOnClickListener(listener);
    }

    /* ======================================================================
     * ============================================================== details
     * ====================================================================== */

    private final HashMap<String, Spanned> htmlCache = new HashMap<String, Spanned>(8);

    private Spanned convertToHtml(String string, ImageGetter imageGetter, TagHandler tagHandler) {
        if(!htmlCache.containsKey(string)) {
            Spanned html = Html.fromHtml(string, imageGetter, tagHandler);
            htmlCache.put(string, html);
            return html;
        }
        return htmlCache.get(string);
    }

    private final HashMap<Long, String> dateCache = new HashMap<Long, String>(8);

    private String formatDate(long date) {
        if(dateCache.containsKey(date))
            return dateCache.get(date);

        String string;
        if(Task.hasDueTime(date))
            string = DateUtilities.getDateStringWithTimeAndWeekday(activity, new Date(date));
        else
            string = DateUtilities.getDateStringWithWeekday(activity, new Date(date));
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
                    query.get(), null, Task.ID, Task.DETAILS, Task.DETAILS_DATE,
                    Task.MODIFICATION_DATE, Task.COMPLETION_DATE);
            try {
                activity.startManagingCursor(fetchCursor);
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
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
            activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
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

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private final ImageGetter detailImageGetter = new ImageGetter() {
        private final HashMap<Integer, Drawable> cache =
            new HashMap<Integer, Drawable>(3);
        public Drawable getDrawable(String source) {
            Resources r = activity.getResources();
            int drawable = r.getIdentifier("drawable/" + source, null, Constants.PACKAGE); //$NON-NLS-1$
            if(drawable == 0)
                return null;
            Drawable d;
            if(!cache.containsKey(drawable)) {
                 d = r.getDrawable(drawable);
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
        extendedDetailManager.clearCache();
        decorationManager.clearCache();
        taskActionManager.clearCache();
        taskDetailLoader.clear();
        detailLoader = new DetailLoaderThread();
        detailLoader.start();
    }

    /**
     * Called to tell the cache to be cleared
     */
    public void flushSpecific(long taskId) {
        completedItems.put(taskId, null);
        extendedDetailManager.clearCache(taskId);
        decorationManager.clearCache(taskId);
        taskActionManager.clearCache(taskId);
        taskDetailLoader.remove(taskId);
    }

    /**
     * AddOnManager for Details
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class ExtendedDetailManager extends TaskAdapterAddOnManager<String> {
        private final Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);

        public ExtendedDetailManager() {
            super(activity);
        }

        @Override
        protected
        Intent createBroadcastIntent(Task task) {
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, true);
            return broadcastIntent;
        }

        @Override
        public void addNew(long taskId, String addOn, String item) {
            super.addNew(taskId, addOn, item);
        }

        private final StringBuilder detailText = new StringBuilder();

        @SuppressWarnings("nls")
        @Override
        protected
        void draw(ViewHolder viewHolder, long taskId, Collection<String> details) {
            if(details == null || viewHolder.task.getId() != taskId)
                return;
            TextView view = viewHolder.extendedDetails;
            if(details.isEmpty() || (expanded != taskId)) {
                reset(viewHolder, taskId);
                return;
            }
            view.setVisibility(View.VISIBLE);
            detailText.setLength(0);
            for(Iterator<String> iterator = details.iterator(); iterator.hasNext(); ) {
                detailText.append(iterator.next());
                if(iterator.hasNext())
                    detailText.append(DETAIL_SEPARATOR);
            }
            String string = detailText.toString();
            if(string.contains("<"))
                view.setText(convertToHtml(string.trim().replace("\n", "<br>"),
                        detailImageGetter, null));
            else
                view.setText(string.trim());
            if(string.contains(".") || string.contains("-"))
                Linkify.addLinks(view, Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS |
                    Linkify.WEB_URLS);
        }

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            TextView view = viewHolder.extendedDetails;
            if(view != null)
                view.setVisibility(View.GONE);
        }
    }

    /**
     * AddOnManager for TaskDecorations
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DecorationManager extends TaskAdapterAddOnManager<TaskDecoration> {

        public DecorationManager() {
            super(activity);
        }

        private final TaskDecorationExposer[] exposers = new TaskDecorationExposer[] {
                new TimerDecorationExposer(),
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
                if(deco != null)
                    addNew(viewHolder.task.getId(), exposer.getAddon(), deco);
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
                    View view = decoration.decoration.apply(activity, viewHolder.taskRow);
                    viewHolder.decorations[i] = view;
                    switch(decoration.position) {
                    case TaskDecoration.POSITION_LEFT:
                        viewHolder.taskRow.addView(view, 2);
                        break;
                    case TaskDecoration.POSITION_RIGHT:
                        viewHolder.taskRow.addView(view, viewHolder.taskRow.getChildCount() - 1);
                    }
                }
                i++;
            }
        }

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            if(viewHolder.decorations != null) {
                for(View view : viewHolder.decorations)
                    viewHolder.taskRow.removeView(view);
            }
            if(taskId == expanded)
                viewHolder.view.setBackgroundResource(R.drawable.list_selector_highlighted);
            else
                viewHolder.view.setBackgroundResource(android.R.drawable.list_selector_background);
        }

        @Override
        protected Intent createBroadcastIntent(Task task) {
            return null;
        }
    }

    /**
     * AddOnManager for TaskActions
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TaskActionManager extends TaskAdapterAddOnManager<TaskAction> {

        private final LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT, 1f);

        private final Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_ACTIONS);

        public TaskActionManager() {
            super(activity);
        }

        @Override
        protected Intent createBroadcastIntent(Task task) {
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            return broadcastIntent;
        }

        //quickactionbar
        //private QuickActionWidget mBar;

        @Override
        public synchronized void addNew(long taskId, String addOn, final TaskAction item) {
            // TODO Auto-generated method stub
            addIfNotExists(taskId, addOn, item);
            if(mBar != null) {
                ListView listView = activity.getListView();
                ViewHolder myHolder = null;
                // update view if it is visible
                int length = listView.getChildCount();
                for(int i = 0; i < length; i++) {
                    ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                    if(viewHolder == null || viewHolder.task.getId() != taskId)
                        continue;
                    myHolder = viewHolder;
                    break;
                }

                if(myHolder != null) {
                    final ViewHolder viewHolder = myHolder;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Drawable drawable = new BitmapDrawable(activity.getResources(), item.icon);
                            mBar.addQuickAction(new QuickAction(drawable, item.text));
                            mBar.setOnQuickActionClickListener(new QuickActionListener(item, viewHolder));
                            mBar.show(viewHolder.view);
                        }
                    });
                }
            }
        }

        @Override
        public Collection<TaskAction> get(long taskId) {
            return super.get(taskId);
        }

        @Override
        protected void draw(final ViewHolder viewHolder, final long taskId, Collection<TaskAction> actions) {
            // do not draw!
        }

        /*
        //preparing quickActionBar
        private void prepareQuickActionBar(ViewHolder viewHolder, Collection<TaskAction> actions){

            mBar = new QuickActionBar(viewHolder.view.getContext());
            QuickAction editAction = new QuickAction(viewHolder.view.getContext(), R.drawable.tango_edit, "   Edit   ");
            mBar.addQuickAction(editAction);
            for(TaskAction action : actions) {

                mBar.addQuickAction(new QuickAction(viewHolder.view.getContext(), R.drawable.tango_clock, action.text));
                mBar.setOnQuickActionClickListener(new quickActionListener(action, viewHolder));
            }

        }
        */
        /*
        protected synchronized void showQuickActionBar(ViewHolder viewHolder, long taskId, Collection<TaskAction> actions) {
            if(mBar != null){
                mBar.dismiss();
            }
            if(expanded != taskId) {
                //viewHolder.actions.setVisibility(View.GONE);
                return;
            }
            //display quickactionbar
            else {
                prepareQuickActionBar(viewHolder, actions);
                mBar.show(viewHolder.view);

             }
            //viewHolder.actions.setVisibility(View.VISIBLE);
        }
         */

        @Override
        protected void reset(ViewHolder viewHolder, long taskId) {
            if(expanded != taskId) {
                mBar.dismiss();
                //viewHolder.actions.setVisibility(View.GONE);
                return;
            }
            //display quickactionbar
            /*
            else{
                prepareQuickActionBar(viewHolder);
                mBar.show(viewHolder.view);
            }
            */
            //viewHolder.actions.setVisibility(View.VISIBLE);
        }
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize, 20);

    }

    protected final View.OnClickListener completeBoxListener = new View.OnClickListener() {
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            Task task = viewHolder.task;

            completeTask(task, ((CheckBox)v).isChecked());

            // set check box to actual action item state
            setTaskAppearance(viewHolder, task);
        }
    };

/*
    private final class ActionClickListener implements View.OnClickListener {
        private final TaskAction action;
        private final ViewHolder viewHolder;

        public ActionClickListener(TaskAction action, ViewHolder viewHolder) {
            this.action = action;
            this.viewHolder = viewHolder;
        }

        public void onClick(View v) {
            flushSpecific(viewHolder.task.getId());
            try {
                action.intent.send();
            } catch (Exception e) {
                exceptionService.displayAndReportError(activity,
                        "Error launching action", e); //$NON-NLS-1$
            }
            decorationManager.request(viewHolder);
            extendedDetailManager.request(viewHolder);
            taskActionManager.request(viewHolder);
        }
    };
*/

    private final class QuickActionListener implements OnQuickActionClickListener {
        private final TaskAction action;
        private final ViewHolder viewHolder;

        public QuickActionListener(TaskAction action, ViewHolder viewHolder) {
            this.action = action;
            this.viewHolder = viewHolder;
        }

        public void onQuickActionClicked(QuickActionWidget widget, int position){
            mBar.dismiss();
            mBar = null;

            if(position == 0){

                Intent intent = new Intent(activity, TaskEditActivity.class);
                intent.putExtra(TaskEditActivity.TOKEN_ID, viewHolder.task.getId());
                activity.startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
            }
            else{
                flushSpecific(viewHolder.task.getId());
                try {
                    action.intent.send();
                } catch (Exception e) {
                    exceptionService.displayAndReportError(activity,
                            "Error launching action", e); //$NON-NLS-1$
                }
                decorationManager.request(viewHolder);
                extendedDetailManager.request(viewHolder);
                taskActionManager.request(viewHolder);
            }
            clearSelection();
            notifyDataSetChanged();

        }
    }

    private class TaskRowListener implements OnCreateContextMenuListener, OnClickListener {

        //prep quick action bar
        private void prepareQuickActionBar(ViewHolder viewHolder, Collection<TaskAction> collection){

            mBar = new QuickActionBar(viewHolder.view.getContext());
            QuickAction editAction = new QuickAction(activity, R.drawable.tango_edit, "   Edit   ");
            mBar.addQuickAction(editAction);


            if(collection != null) {
                for(TaskAction item : collection) {
                    Drawable drawable = new BitmapDrawable(activity.getResources(), item.icon);
                    mBar.addQuickAction(new QuickAction(drawable, item.text));
                    mBar.setOnQuickActionClickListener(new QuickActionListener(item, viewHolder));
                }
            }


        }

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

            Collection<TaskAction> actions = taskActionManager.get(taskId);
            prepareQuickActionBar(viewHolder, actions);
            //mBarAnchor = v;
            System.err.println("view is ID " + v.getId());
            if(actions != null)
                mBar.show(v);
            System.err.println("! Request for " + taskId);
            taskActionManager.request(viewHolder);


            notifyDataSetChanged();
        }
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

        TextView name = viewHolder.nameView;
        if(state) {
            name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle_Completed);
        } else {
            name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle);
        }
        name.setTextSize(fontSize);
        float detailTextSize = Math.max(12, fontSize * 14 / 20);
        if(viewHolder.details != null)
            viewHolder.details.setTextSize(detailTextSize);
        if(viewHolder.dueDate != null)
            viewHolder.dueDate.setTextSize(detailTextSize);
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
            completedItems.put(task.getId(), newState);
            taskService.setComplete(task, newState);

            if(onCompletedTaskListener != null)
                onCompletedTaskListener.onCompletedTask(task, newState);
        }
    }

    public void clearSelection() {
        expanded = -1;
    }

}
