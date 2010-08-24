package com.todoroo.astrid.adapter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.text.Html.ImageGetter;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
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
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;

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
    };

    private static int[] IMPORTANCE_COLORS = null;

    // --- instance variables

    @Autowired
    private ExceptionService exceptionService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AddOnService addOnService;

    protected final ListActivity activity;
    protected final HashMap<Long, Boolean> completedItems = new HashMap<Long, Boolean>();
    private OnCompletedTaskListener onCompletedTaskListener = null;
    public boolean isFling = false;
    private final int resource;
    private final LayoutInflater inflater;
    private int fontSize;
    private DetailLoaderThread detailLoader;

    private final AtomicReference<String> query;

    // the task that's expanded
    private long expanded = -1;

    // --- task detail and decoration soft caches

    public final DetailManager extendedDetailManager = new DetailManager();
    public final DecorationManager decorationManager =
        new DecorationManager();
    public final TaskActionManager taskActionManager = new TaskActionManager();

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
            viewHolder.nameView.setFocusable(false);
            viewHolder.nameView.setClickable(false);
            Linkify.addLinks(nameView, Linkify.ALL);
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

                String dateValue;
                Date dueDateAsDate = DateUtilities.unixtimeToDate(dueDate);
                if (task.hasDueTime()) {
                    dateValue = DateUtilities.getDateStringWithTimeAndWeekday(activity, dueDateAsDate);
                } else {
                    dateValue = DateUtilities.getDateStringWithWeekday(activity, dueDateAsDate);
                }
                dueDateView.setText(dateValue);
                setVisibility(dueDateView);
            } else if(task.isCompleted()) {
                String dateValue = DateUtilities.getDateStringWithWeekday(activity,
                        new Date(task.getValue(Task.COMPLETION_DATE)));
                dueDateView.setText(r.getString(R.string.TAd_completed, dateValue));
                dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDetails);
                setVisibility(dueDateView);
            } else {
                dueDateView.setVisibility(View.GONE);
            }
        }

        // complete box
        final CheckBox completeBox = viewHolder.completeBox; {
            // show item as completed if it was recently checked
            if(completedItems.containsKey(task.getId())) {
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
        if(taskDetailLoader.containsKey(task.getId()))
            details = taskDetailLoader.get(task.getId()).toString();
        else
            details = task.getValue(Task.DETAILS);
        if(TextUtils.isEmpty(details) || DETAIL_SEPARATOR.equals(details)) {
            viewHolder.details.setVisibility(View.GONE);
        } else {
            viewHolder.details.setVisibility(View.VISIBLE);
            viewHolder.details.setText(Html.fromHtml(details.trim().replace("\n", //$NON-NLS-1$
                    "<br>"), detailImageGetter, null)); //$NON-NLS-1$
        }

        // details and decorations, expanded
        if(!isFling) {
            decorationManager.request(viewHolder);
            if(expanded == task.getId()) {
                extendedDetailManager.request(viewHolder);
                taskActionManager.request(viewHolder);
            } else {
                viewHolder.extendedDetails.setVisibility(View.GONE);
                viewHolder.actions.setVisibility(View.GONE);
            }
        } else {
            long taskId = viewHolder.task.getId();
            decorationManager.reset(viewHolder, taskId);
            viewHolder.extendedDetails.setVisibility(View.GONE);
            viewHolder.actions.setVisibility(View.GONE);
        }
    }

    protected TaskRowListener listener = new TaskRowListener();
    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    private void addListeners(final View container) {
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

    // implementation note: this map is really costly if users have
    // a large number of tasks to load, since it all goes into memory.
    // it's best to do this, though, in order to append details to each other
    private final Map<Long, StringBuilder> taskDetailLoader = Collections.synchronizedMap(new HashMap<Long, StringBuilder>());

    private final Task taskDetailContainer = new Task();

    public class DetailLoaderThread extends Thread {
        @Override
        public void run() {
            // for all of the tasks returned by our cursor, verify details
            TodorooCursor<Task> fetchCursor = taskService.fetchFiltered(
                    query.get(), null, Task.ID, Task.DETAILS, Task.COMPLETION_DATE);
            activity.startManagingCursor(fetchCursor);
            try {
                Task task = new Task();
                for(fetchCursor.moveToFirst(); !fetchCursor.isAfterLast(); fetchCursor.moveToNext()) {
                    task.clear();
                    task.readFromCursor(fetchCursor);
                    if(task.isCompleted())
                        continue;
                    if(TextUtils.isEmpty(task.getValue(Task.DETAILS))) {
                        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
                        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
                        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
                        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

                        taskDetailLoader.put(task.getId(), new StringBuilder());

                        task.setValue(Task.DETAILS, DETAIL_SEPARATOR);
                        taskService.save(task);
                    }
                }
            } catch (Exception e) {
                // suppress silently
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
            taskDetailContainer.setId(id);
            taskDetailContainer.setValue(Task.DETAILS, details.toString());
            taskService.save(taskDetailContainer);
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView listView = activity.getListView();
                int scrollPos = listView.getScrollY();
                notifyDataSetInvalidated();
                listView.scrollTo(0, scrollPos);
            }
        });
    }

    private final ImageGetter detailImageGetter = new ImageGetter() {
        public Drawable getDrawable(String source) {
            Resources r = activity.getResources();
            int drawable = r.getIdentifier("drawable/" + source, null, Constants.PACKAGE); //$NON-NLS-1$
            if(drawable == 0)
                return null;
            Drawable d = r.getDrawable(drawable);
            d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
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
        extendedDetailManager.clearCache();
        decorationManager.clearCache();
        taskActionManager.clearCache();
        detailLoader = new DetailLoaderThread();
        detailLoader.start();
    }

    /**
     * AddOnManager for Details
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DetailManager extends AddOnManager<String> {

        public DetailManager() {
            //
        }

        @Override
        Intent createBroadcastIntent(long taskId) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, true);
            return broadcastIntent;
        }

        @Override
        public void addNew(long taskId, String addOn, String item) {
            super.addNew(taskId, addOn, item);
        }

        @SuppressWarnings("nls")
        @Override
        void draw(ViewHolder viewHolder, long taskId, Collection<String> details) {
            if(details == null || viewHolder.task.getId() != taskId)
                return;
            TextView view = viewHolder.extendedDetails;
            if(details.isEmpty() || (expanded != taskId)) {
                reset(viewHolder, taskId);
                return;
            }
            view.setVisibility(View.VISIBLE);
            StringBuilder detailText = new StringBuilder();
            for(Iterator<String> iterator = details.iterator(); iterator.hasNext(); ) {
                detailText.append(iterator.next());
                if(iterator.hasNext())
                    detailText.append(DETAIL_SEPARATOR);
            }
            String string = detailText.toString();
            if(string.contains("<"))
                view.setText(Html.fromHtml(string.trim().replace("\n", "<br>"),
                        detailImageGetter, null));
            else
                view.setText(string.trim());
            Linkify.addLinks(view, Linkify.ALL);
        }

        @Override
        void reset(ViewHolder viewHolder, long taskId) {
            TextView view = viewHolder.extendedDetails;
            view.setVisibility(View.GONE);
        }
    }

    /**
     * AddOnManager for TaskDecorations
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DecorationManager extends AddOnManager<TaskDecoration> {
        @Override
        Intent createBroadcastIntent(long taskId) {
            Intent intent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DECORATIONS);
            intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            return intent;
        }

        @Override
        void draw(ViewHolder viewHolder, long taskId, Collection<TaskDecoration> decorations) {
            if(decorations == null || viewHolder.task.getId() != taskId)
                return;

            reset(viewHolder, taskId);
            if(decorations.size() == 0)
                return;

            int i = 0;
            boolean colorSet = false;
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
        void reset(ViewHolder viewHolder, long taskId) {
            if(viewHolder.decorations != null) {
                for(View view : viewHolder.decorations)
                    viewHolder.taskRow.removeView(view);
            }
            if(taskId == expanded)
                viewHolder.view.setBackgroundColor(Color.argb(20, 255, 255, 255));
            else
                viewHolder.view.setBackgroundResource(android.R.drawable.list_selector_background);
        }
    }

    /**
     * AddOnManager for TaskActions
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TaskActionManager extends AddOnManager<TaskAction> {

        private final LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT, 1f);

        @Override
        Intent createBroadcastIntent(long taskId) {
            Intent intent = new Intent(AstridApiConstants.BROADCAST_REQUEST_ACTIONS);
            intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            return intent;
        }

        @Override
        void draw(final ViewHolder viewHolder, final long taskId, Collection<TaskAction> actions) {
            if(actions == null || viewHolder.task.getId() != taskId)
                return;

            // hack because we know we have > 1 button
            if(addOnService.hasPowerPack() && actions.size() == 0)
                return;

            for(int i = viewHolder.actions.getChildCount(); i < actions.size() + 1; i++) {
                Button editButton = new Button(activity);
                editButton.setLayoutParams(params);
                viewHolder.actions.addView(editButton);
            }
            if(actions.size() + 1 < viewHolder.actions.getChildCount())
                viewHolder.actions.removeViews(0, viewHolder.actions.getChildCount() -
                        actions.size() - 1);

            int i = 0;
            Button button = (Button) viewHolder.actions.getChildAt(i++);

            button.setText(R.string.TAd_actionEditTask);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(activity, TaskEditActivity.class);
                    intent.putExtra(TaskEditActivity.TOKEN_ID, taskId);
                    activity.startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
                }
            });

            for(TaskAction action : actions) {
                button = (Button) viewHolder.actions.getChildAt(i++);
                button.setText(action.text);
                button.setOnClickListener(new ActionClickListener(action));
            }

            reset(viewHolder, taskId);
        }

        @Override
        void reset(ViewHolder viewHolder, long taskId) {
            if(expanded != taskId) {
                viewHolder.actions.setVisibility(View.GONE);
                return;
            }
            viewHolder.actions.setVisibility(View.VISIBLE);
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

    private final View.OnClickListener completeBoxListener = new View.OnClickListener() {
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            Task task = viewHolder.task;

            completeTask(task, ((CheckBox)v).isChecked());

            // set check box to actual action item state
            setTaskAppearance(viewHolder, task);
        }
    };

    private final class ActionClickListener implements View.OnClickListener {
        TaskAction action;

        public ActionClickListener(TaskAction action) {
            this.action = action;
        }

        public void onClick(View v) {
            try {
                action.intent.send();
            } catch (Exception e) {
                exceptionService.displayAndReportError(activity,
                        "Error launching action", e); //$NON-NLS-1$
            }
        }
    };

    private class TaskRowListener implements OnCreateContextMenuListener, OnClickListener {

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
            if(expanded == taskId) {
                expanded = -1;
            } else {
                expanded = taskId;
            }
            ListView listView = activity.getListView();
            int scrollPos = listView.getScrollY();
            notifyDataSetChanged();
            listView.scrollTo(0, scrollPos);
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

    /* ======================================================================
     * ========================================================= addon helper
     * ====================================================================== */

    abstract public class AddOnManager<TYPE> {

        private final Map<Long, HashMap<String, TYPE>> cache =
            new HashMap<Long, HashMap<String, TYPE>>();

        // --- interface

        /**
         * Request add-ons for the given task
         * @return true if cache miss, false if cache hit
         */
        public boolean request(ViewHolder viewHolder) {
            long taskId = viewHolder.task.getId();

            Collection<TYPE> list = initialize(taskId);
            if(list != null) {
                draw(viewHolder, taskId, list);
                return false;
            }

            // request details
            draw(viewHolder, taskId, get(taskId));
            Intent broadcastIntent = createBroadcastIntent(taskId);
            activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            return true;
        }

        /** creates a broadcast intent for requesting */
        abstract Intent createBroadcastIntent(long taskId);

        /** updates the given view */
        abstract void draw(ViewHolder viewHolder, long taskId, Collection<TYPE> list);

        /** resets the view as if there was nothing */
        abstract void reset(ViewHolder viewHolder, long taskId);

        /** on receive an intent */
        public void addNew(long taskId, String addOn, TYPE item) {
            if(item == null)
                return;

            Collection<TYPE> cacheList = addIfNotExists(taskId, addOn, item);
            if(cacheList != null) {
                ListView listView = activity.getListView();
                // update view if it is visible
                int length = listView.getChildCount();
                for(int i = 0; i < length; i++) {
                    ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                    if(viewHolder == null || viewHolder.task.getId() != taskId)
                        continue;
                    draw(viewHolder, taskId, cacheList);
                    break;
                }
            }
        }

        /**
         * Clears the cache
         */
        public void clearCache() {
            cache.clear();
        }

        // --- internal goodies

        /**
         * Retrieves a list. If it doesn't exist, list is created, but
         * the method will return null
         * @param taskId
         * @return list if there was already one
         */
        protected synchronized Collection<TYPE> initialize(long taskId) {
            if(cache.containsKey(taskId) && cache.get(taskId) != null)
                return get(taskId);
            cache.put(taskId, new HashMap<String, TYPE>());
            return null;
        }

        /**
         * Adds an item to the cache if it doesn't exist
         * @param taskId
         * @param item
         * @return iterator if item was added, null if it already existed
         */
        protected synchronized Collection<TYPE> addIfNotExists(long taskId, String addOn,
                TYPE item) {
            HashMap<String, TYPE> list = cache.get(taskId);
            if(list == null)
                return null;
            if(list.containsKey(addOn) && list.get(addOn).equals(item))
                return null;
            list.put(addOn, item);
            return get(taskId);
        }

        /**
         * Gets an item at the given index
         * @param taskId
         * @return
         */
        protected Collection<TYPE> get(long taskId) {
            return cache.get(taskId).values();
        }

    }

}
