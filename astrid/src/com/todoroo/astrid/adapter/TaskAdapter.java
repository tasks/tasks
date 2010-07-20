package com.todoroo.astrid.adapter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.SoftHashMap;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.notes.NoteDetailExposer;
import com.todoroo.astrid.repeats.RepeatDetailExposer;
import com.todoroo.astrid.rmilk.MilkDetailExposer;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagDetailExposer;
import com.todoroo.astrid.utility.Preferences;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAdapter extends CursorAdapter {

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
    };

    /** Internal Task Detail exposers */
    public static final DetailExposer[] EXPOSERS = new DetailExposer[] {
        new TagDetailExposer(),
        new RepeatDetailExposer(),
        new NoteDetailExposer(),
        new MilkDetailExposer(),
    };

    private static int[] IMPORTANCE_COLORS = null;

    // --- instance variables

    @Autowired
    ExceptionService exceptionService;

    @Autowired
    TaskService taskService;

    @Autowired
    DialogUtilities dialogUtilities;

    protected final ListActivity activity;
    protected final HashMap<Long, Boolean> completedItems;
    public boolean isFling = false;
    private final int resource;
    private final LayoutInflater inflater;
    protected OnCompletedTaskListener onCompletedTaskListener = null;
    private int fontSize;

    // --- task detail and decoration soft caches

    public final DetailManager detailManager = new DetailManager(false);
    public final DetailManager extendedDetailManager = new DetailManager(true);
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
            TodorooCursor<Task> c, boolean autoRequery,
            OnCompletedTaskListener onCompletedTaskListener) {
        super(activity, c, autoRequery);
        DependencyInjectionService.getInstance().inject(this);

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.resource = resource;
        this.activity = activity;
        this.onCompletedTaskListener = onCompletedTaskListener;

        completedItems = new HashMap<Long, Boolean>();
        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize);

        IMPORTANCE_COLORS = Task.getImportanceColors(activity.getResources());
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
        Task actionItem = viewHolder.task;
        actionItem.readFromCursor(cursor);

        setFieldContentsAndVisibility(view);
        setTaskAppearance(viewHolder, actionItem.isCompleted());
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
    public class ViewHolder {
        public Task task;
        public TextView nameView;
        public CheckBox completeBox;
        public TextView dueDate;
        public TextView details;
        public TextView extendedDetails;
        public View importance;
        public LinearLayout actions;
        public LinearLayout taskRow;
        public boolean expanded;

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
                    dateValue = DateUtilities.getDateWithTimeFormat(activity).format(dueDateAsDate);
                } else {
                    dateValue = DateUtilities.getDateFormat(activity).format(dueDateAsDate);
                }
                dueDateView.setText(dateValue);
                setVisibility(dueDateView);
            } else if(task.isCompleted()) {
                String dateValue = DateUtilities.getDateFormat(activity).format(task.getValue(Task.COMPLETION_DATE));
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
            if(completedItems.containsKey(task.getId()))
                task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
            completeBox.setChecked(task.isCompleted());
        }

        // importance bar
        final View importanceView = viewHolder.importance; {
            int value = task.getValue(Task.IMPORTANCE);
            importanceView.setBackgroundColor(IMPORTANCE_COLORS[value]);
        }

        // details and decorations
        if(!isFling) {
            detailManager.request(viewHolder);
            decorationManager.request(viewHolder);
        }
    }

    protected TaskRowListener listener = new TaskRowListener();
    /**
     * Set listeners for this view. This is called once per view when it is
     * created.
     */
    private void addListeners(final View container) {
        // check box listener
        final CheckBox completeBox = ((CheckBox)container.findViewById(R.id.completeBox));
        completeBox.setOnClickListener(completeBoxListener);

        // context menu listener
        container.setOnCreateContextMenuListener(listener);

        // tap listener
        container.setOnClickListener(listener);
    }

    /* ======================================================================
     * ============================================================== add-ons
     * ====================================================================== */

    /**
     * Called to tell the cache to be cleared
     */
    public void flushCaches() {
        detailManager.clearCache();
        extendedDetailManager.clearCache();
        decorationManager.clearCache();
        taskActionManager.clearCache();
    }

    /**
     * AddOnManager for Details
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class DetailManager extends AddOnManager<String> {

        private final boolean extended;
        public DetailManager(boolean extended) {
            this.extended = extended;
        }

        @Override
        Intent createBroadcastIntent(long taskId) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
            return broadcastIntent;
        }

        @Override
        public boolean request(final ViewHolder viewHolder) {
            if(super.request(viewHolder)) {
                final long taskId = viewHolder.task.getId();
                // load internal details
                new Thread() {
                    @Override
                    public void run() {
                        for(DetailExposer exposer : EXPOSERS) {
                            final String detail = exposer.getTaskDetails(activity,
                                    taskId, extended);
                            if(detail == null)
                                continue;
                            final Collection<String> cacheList =
                                addIfNotExists(taskId, exposer.getPluginIdentifier(),
                                        detail);
                            if(cacheList != null)  {
                                if(taskId != viewHolder.task.getId())
                                    continue;
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        draw(viewHolder, cacheList);
                                    }
                                });
                            }
                        }
                    };
                }.start();
                return true;
            }
            return false;
        }

        @SuppressWarnings("nls")
        @Override
        void draw(ViewHolder viewHolder, Collection<String> details) {
            if(details == null)
                return;
            TextView view = extended ? viewHolder.extendedDetails : viewHolder.details;
            view.setVisibility(!details.isEmpty() ? View.VISIBLE : View.GONE);
            if(details.isEmpty() || (extended && !viewHolder.expanded)) {
                view.setVisibility(View.GONE);
                return;
            } else {
                view.setVisibility(View.VISIBLE);
            }
            StringBuilder detailText = new StringBuilder();
            for(Iterator<String> iterator = details.iterator(); iterator.hasNext(); ) {
                detailText.append(iterator.next());
                if(iterator.hasNext())
                    detailText.append(DETAIL_SEPARATOR);
            }
            String string = detailText.toString();
            if(string.contains("<"))
                view.setText(Html.fromHtml(string.trim().replace("\n", "<br>")));
            else
                view.setText(string.trim());
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
        void draw(ViewHolder viewHolder, Collection<TaskDecoration> decorations) {
            if(decorations == null || decorations.size() == 0)
                return;

            if(viewHolder.decorations != null) {
                for(View view : viewHolder.decorations)
                    viewHolder.taskRow.removeView(view);
                ((View)viewHolder.taskRow.getParent()).setBackgroundColor(Color.TRANSPARENT);
            }


            int i = 0;
            boolean colorSet = false;
            viewHolder.decorations = new View[decorations.size()];
            for(TaskDecoration decoration : decorations) {
                if(decoration.color != 0 && !colorSet) {
                    colorSet = true;
                    ((View)viewHolder.taskRow.getParent()).setBackgroundColor(decoration.color);
                }
                if(decoration.decoration != null) {
                    View view = decoration.decoration.apply(activity, viewHolder.taskRow);
                    viewHolder.decorations[i] = view;
                    switch(decoration.position) {
                    case TaskDecoration.POSITION_LEFT:
                        viewHolder.taskRow.addView(view, 1);
                        break;
                    case TaskDecoration.POSITION_RIGHT:
                        viewHolder.taskRow.addView(view, viewHolder.taskRow.getChildCount() - 2);
                    }
                }
                i++;
            }
        }
    }

    /**
     * AddOnManager for TaskActions
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class TaskActionManager extends AddOnManager<TaskAction> {
        @Override
        Intent createBroadcastIntent(long taskId) {
            Intent intent = new Intent(AstridApiConstants.BROADCAST_REQUEST_ACTIONS);
            intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            return intent;
        }

        @Override
        void draw(final ViewHolder viewHolder, Collection<TaskAction> actions) {
            if(actions == null)
                return;

            if(!viewHolder.expanded) {
                viewHolder.actions.setVisibility(View.GONE);
                return;
            }
            viewHolder.actions.setVisibility(View.VISIBLE);
            viewHolder.actions.removeAllViews();

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                        LayoutParams.FILL_PARENT, 1f);

            Button editButton = new Button(activity);
            editButton.setText(R.string.TAd_actionEditTask);
            editButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(activity, TaskEditActivity.class);
                    long taskId = viewHolder.task.getId();
                    intent.putExtra(TaskEditActivity.ID_TOKEN, taskId);
                    activity.startActivity(intent);
                }
            });
            editButton.setLayoutParams(params);
            viewHolder.actions.addView(editButton);

            for(TaskAction action : actions) {
                Button view = new Button(activity);
                view.setText(action.text);
                view.setOnClickListener(new ActionClickListener(action));
                view.setLayoutParams(params);
                viewHolder.actions.addView(view);
            }

        }
    }

    /* ======================================================================
     * ======================================================= event handlers
     * ====================================================================== */

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        fontSize = Preferences.getIntegerFromString(R.string.p_fontSize);
    }

    private final View.OnClickListener completeBoxListener = new View.OnClickListener() {
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder)((View)v.getParent().getParent()).getTag();
            Task task = viewHolder.task;

            completeTask(task, ((CheckBox)v).isChecked());

            // set check box to actual action item state
            setTaskAppearance(viewHolder, task.isCompleted());
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

                // refresh ourselves
                getCursor().requery();
                notifyDataSetChanged();
            } catch (Exception e) {
                // oh too bad.
                Log.i("astrid-action-error", "Error launching intent", e); //$NON-NLS-1$ //$NON-NLS-2$
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
            // expand view
            final ViewHolder viewHolder = (ViewHolder)v.getTag();
            viewHolder.expanded = !viewHolder.expanded;

            if(viewHolder.expanded) {
                extendedDetailManager.request(viewHolder);
                taskActionManager.request(viewHolder);
            } else {
                viewHolder.extendedDetails.setVisibility(View.GONE);
                viewHolder.actions.setVisibility(View.GONE);
            }
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
    void setTaskAppearance(ViewHolder viewHolder, boolean state) {
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
    protected void completeTask(final Task actionItem, final boolean newState) {
        if(actionItem == null)
            return;

        if (newState != actionItem.isCompleted()) {
            completedItems.put(actionItem.getId(), newState);
            taskService.setComplete(actionItem, newState);

            if(onCompletedTaskListener != null)
                onCompletedTaskListener.onCompletedTask(actionItem, newState);
        }
    }

    /* ======================================================================
     * ========================================================= addon helper
     * ====================================================================== */

    abstract private class AddOnManager<TYPE> {

        private final Map<Long, HashMap<String, TYPE>> cache =
            Collections.synchronizedMap(new SoftHashMap<Long,
                    HashMap<String, TYPE>>());

        // --- interface

        /**
         * Request add-ons for the given task
         * @return true if cache miss, false if cache hit
         */
        public boolean request(ViewHolder viewHolder) {
            long taskId = viewHolder.task.getId();

            Collection<TYPE> list = initialize(taskId);
            if(list != null) {
                draw(viewHolder, list);
                return false;
            }

            // request details
            Intent broadcastIntent = createBroadcastIntent(taskId);
            activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            draw(viewHolder, get(taskId));
            return true;
        }

        /** creates a broadcast intent for requesting */
        abstract Intent createBroadcastIntent(long taskId);

        /** updates the given view */
        abstract void draw(ViewHolder viewHolder, Collection<TYPE> list);

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
                    draw(viewHolder, cacheList);
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
        protected Collection<TYPE> initialize(long taskId) {
            if(cache.containsKey(taskId))
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
        protected Collection<TYPE> addIfNotExists(long taskId, String addOn,
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
