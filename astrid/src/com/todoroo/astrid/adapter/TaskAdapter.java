package com.todoroo.astrid.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.text.Html;
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
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
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

    private static final TaskCache<String> detailCache = new TaskCache<String>();
    private static final TaskCache<String> extendedDetailCache = new TaskCache<String>();
    private static final TaskCache<TaskDecoration> decorationCache = new TaskCache<TaskDecoration>();

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
        viewHolder.details.setText(""); //$NON-NLS-1$
        if(viewHolder.decorations != null) {
            for(View decoration: viewHolder.decorations)
                viewHolder.taskRow.removeView(decoration);
        }
        viewHolder.taskRow.setBackgroundDrawable(null);
        if(!isFling) {
            // task details - send out a request for it
            retrieveDetails(viewHolder, false);

            // task decoration - send out a request for it
            retrieveDecorations(viewHolder);
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

    private void retrieveDetails(final ViewHolder viewHolder, final boolean extended) {
        final long taskId = viewHolder.task.getId();

        final TaskCache<String> cache = extended ? extendedDetailCache : detailCache;
        ArrayList<String> list = cache.initialize(taskId);
        if(list != null) {
            spanifyAndAdd(viewHolder, extended, list);
            return;
        }

        // request details
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        // load internal details
        new Thread() {
            @Override
            public void run() {
                for(DetailExposer exposer : EXPOSERS) {
                    final String detail = exposer.getTaskDetails(activity,
                            taskId, extended);
                    if(detail == null)
                        continue;
                    final ArrayList<String> cacheList = cache.addIfNotExists(taskId, detail);
                    if(cacheList != null)  {
                        if(taskId != viewHolder.task.getId())
                            continue;
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                spanifyAndAdd(viewHolder, extended, cacheList);
                            }
                        });
                    }
                }
            };
        }.start();
    }

    @SuppressWarnings("nls")
    private void spanifyAndAdd(ViewHolder viewHolder, boolean extended, ArrayList<String> details) {
        if(details == null)
            return;
        TextView view = extended ? viewHolder.extendedDetails : viewHolder.details;
        view.setVisibility(details.size() > 0 ? View.VISIBLE : View.GONE);
        if(details.size() == 0 || (extended && !viewHolder.expanded)) {
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

    /**
     * Called to tell the cache to be cleared
     */
    public void flushCaches() {
        detailCache.clear();
        extendedDetailCache.clear();
        decorationCache.clear();
    }

    /**
     * Respond to a request to add details for a task
     *
     * @param taskId
     */
    public synchronized void addDetails(ListView listView, long taskId,
            boolean extended, String detail) {
        if(detail == null)
            return;

        final TaskCache<String> cache = extended ? extendedDetailCache : detailCache;

        ArrayList<String> cacheList = cache.addIfNotExists(taskId, detail);
        if(cacheList != null) {
            // update view if it is visible
            int length = listView.getChildCount();
            for(int i = 0; i < length; i++) {
                ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                if(viewHolder == null || viewHolder.task.getId() != taskId)
                    continue;
                spanifyAndAdd(viewHolder, extended, cacheList);
                break;
            }
        }
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

    private void retrieveDecorations(final ViewHolder viewHolder) {
        final long taskId = viewHolder.task.getId();

        ArrayList<TaskDecoration> list = decorationCache.initialize(taskId);
        if(list != null) {
            decorate(viewHolder, list);
            return;
        }

        // request details
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DECORATIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        if(timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(decorationUpdater, 0, 1000L);
        }
    }

    private Timer timer = null;

    /**
     * Task to update decorations every second
     */
    private final TimerTask decorationUpdater = new TimerTask() {
        @Override
        public void run() {
            ListView listView = activity.getListView();
            int length = listView.getChildCount();
            for(int i = 0; i < length; i++) {
                ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                ArrayList<TaskDecoration> list = decorationCache.get(viewHolder.task.getId());
                if(list == null)
                    continue;

                for(int j = 0; j < list.size(); j++) {
                    final TaskDecoration decoration = list.get(j);
                    if(decoration.decoration == null)
                        continue;
                    final View view = viewHolder.decorations[j];
                    if(view == null)
                        continue;
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            decoration.decoration.reapply(activity, view);
                        }
                    });
                }
            }
        }
    };

    /**
     * Respond to a request to add details for a task
     *
     * @param taskId
     */
    public synchronized void addDecorations(ListView listView, long taskId,
            TaskDecoration taskDecoration) {
        if(taskDecoration == null)
            return;

        ArrayList<TaskDecoration> cacheList = decorationCache.addIfNotExists(taskId,
            taskDecoration);
        if(cacheList != null) {
            // update view if it is visible
            int length = listView.getChildCount();
            for(int i = 0; i < length; i++) {
                ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                if(viewHolder == null || viewHolder.task.getId() != taskId)
                    continue;
                decorate(viewHolder, cacheList);
                break;
            }
        }
    }

    private void decorate(ViewHolder viewHolder, ArrayList<TaskDecoration> decorations) {
        if(decorations == null || decorations.size() == 0)
            return;

        // apply decorations in reverse so top priority appears at the ends &
        // color is set bu the most important decoration
        viewHolder.decorations = new View[decorations.size()];
        for(int i = decorations.size() - 1; i >= 0; i--) {
            TaskDecoration deco = decorations.get(i);
            if(deco.color != 0)
                ((View)viewHolder.taskRow.getParent()).setBackgroundColor(deco.color);
            if(deco.decoration != null) {
                View view = deco.decoration.apply(activity, viewHolder.taskRow);
                viewHolder.decorations[i] = view;
                switch(deco.position) {
                case TaskDecoration.POSITION_LEFT:
                    viewHolder.taskRow.addView(view, 1);
                    break;
                case TaskDecoration.POSITION_RIGHT:
                    viewHolder.taskRow.addView(view, viewHolder.taskRow.getChildCount() - 2);
                }
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

    class TaskRowListener implements OnCreateContextMenuListener, OnClickListener {

        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            // this is all a big sham. it's actually handled in Task List Activity
        }

        @Override
        public void onClick(View v) {
            // expand view
            final ViewHolder viewHolder = (ViewHolder)v.getTag();
            viewHolder.expanded = !viewHolder.expanded;
            LinearLayout actions = viewHolder.actions;
            TextView extendedDetails = viewHolder.extendedDetails;

            actions.setVisibility(viewHolder.expanded ? View.VISIBLE : View.GONE);
            if(!viewHolder.expanded) {
                viewHolder.extendedDetails.setVisibility(View.GONE);
                return;
            }

            final long taskId = viewHolder.task.getId();
            extendedDetails.setText(""); //$NON-NLS-1$
            retrieveDetails(viewHolder, true);

            if(actions.getChildCount() == 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f);
                Button edit = new Button(activity);
                edit.setText(R.string.TAd_actionEditTask);
                edit.setLayoutParams(params);
                edit.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(activity, TaskEditActivity.class);
                        intent.putExtra(TaskEditActivity.ID_TOKEN, taskId);
                        activity.startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
                    }
                });
                actions.addView(edit);
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
     * =============================================================== caches
     * ====================================================================== */

    private static class TaskCache<TYPE> {

        private final Map<Long, ArrayList<TYPE>> cache =
            Collections.synchronizedMap(new SoftHashMap<Long, ArrayList<TYPE>>());

        /**
         * Retrieves a list. If it doesn't exist, list is created, but
         * the method will return null
         * @param taskId
         * @return list if there was already one
         */
        public ArrayList<TYPE> initialize(long taskId) {
            if(cache.containsKey(taskId))
                return cache.get(taskId);
            cache.put(taskId, new ArrayList<TYPE>());
            return null;
        }

        /**
         * Adds an item to the cache if it doesn't exist
         * @param taskId
         * @param item
         * @return list if item was added, null if it already existed
         */
        public ArrayList<TYPE> addIfNotExists(long taskId, TYPE item) {
            ArrayList<TYPE> list = cache.get(taskId);
            if(list == null || list.contains(item))
                return null;
            list.add(item);
            return list;
        }

        /**
         * Clears the cache
         */
        public void clear() {
            cache.clear();
        }

        public ArrayList<TYPE> get(long taskId) {
            return cache.get(taskId);
        }

    }

}
