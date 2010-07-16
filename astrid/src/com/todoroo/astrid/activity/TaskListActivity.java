package com.todoroo.astrid.activity;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TaskDetail;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.reminders.ReminderService.AlarmScheduler;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Primary activity for the Bente application. Shows a list of upcoming
 * tasks and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListActivity extends ListActivity implements OnScrollListener {

    // --- activities

    public static final int ACTIVITY_EDIT_TASK = 0;
    public static final int ACTIVITY_SETTINGS = 1;
    public static final int ACTIVITY_PLUGINS = 2;
    public static final int ACTIVITY_MENU_EXTERNAL = 3;

    // --- menu codes

    private static final int MENU_ADDONS_ID = Menu.FIRST + 1;
    private static final int MENU_SETTINGS_ID = Menu.FIRST + 2;
    private static final int MENU_HELP_ID = Menu.FIRST + 3;
    private static final int MENU_ADDON_INTENT_ID = Menu.FIRST + 4;

    private static final int CONTEXT_MENU_EDIT_TASK_ID = Menu.FIRST + 5;
    private static final int CONTEXT_MENU_DELETE_TASK_ID = Menu.FIRST + 6;
    private static final int CONTEXT_MENU_UNDELETE_TASK_ID = Menu.FIRST + 7;
    private static final int CONTEXT_MENU_ADDON_INTENT_ID = Menu.FIRST + 8;

    /** menu code indicating the end of the context menu */
    private static final int CONTEXT_MENU_DEBUG = Menu.FIRST + 9;

    // --- constants

    /** token for passing a {@link Filter} object through extras */
    public static final String TOKEN_FILTER = "filter"; //$NON-NLS-1$

    /** token for passing a reminder string through extras */
    public static final String TOKEN_REMINDER = "reminder"; //$NON-NLS-1$

    // --- instance variables

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected MetadataService metadataService;

    @Autowired
    protected DialogUtilities dialogUtilities;

    @Autowired
    protected Database database;

    protected TaskAdapter taskAdapter = null;
    protected DetailReceiver detailReceiver = new DetailReceiver();

    ImageButton quickAddButton;
    EditText quickAddBox;
    Filter filter;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public TaskListActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        new StartupService().onStartupApplication(this);
        setContentView(R.layout.task_list_activity);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey(TOKEN_FILTER)) {
            filter = extras.getParcelable(TOKEN_FILTER);
        } else {
            filter = CoreFilterExposer.buildInboxFilter(getResources());
        }

        if(database == null)
            return;

        database.openForWriting();
        setUpUiComponents();
        setUpTaskList();
        if(Constants.DEBUG)
            setTitle("[D] " + filter.title); //$NON-NLS-1$

        // cache some stuff
        new Thread(new Runnable() {
            public void run() {
                loadContextMenuIntents();
            }
        });
    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            return true;

        MenuItem item;

        /*item = menu.add(Menu.NONE, MENU_ADDONS_ID, Menu.NONE,
                R.string.TLA_menu_addons);
        item.setIcon(android.R.drawable.ic_menu_set_as);*/

        item = menu.add(Menu.NONE, MENU_SETTINGS_ID, Menu.NONE,
                R.string.TLA_menu_settings);
        item.setIcon(android.R.drawable.ic_menu_preferences);

        /*item = menu.add(Menu.NONE, MENU_HELP_ID, Menu.NONE,
                R.string.TLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);*/

        // ask about plug-ins
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_TASK_LIST_MENU);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent, 0);
        int length = resolveInfoList.size();
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            item = menu.add(Menu.NONE, MENU_ADDON_INTENT_ID, Menu.NONE,
                        resolveInfo.loadLabel(pm));
            item.setIcon(resolveInfo.loadIcon(pm));
            Intent intent = new Intent(AstridApiConstants.ACTION_TASK_LIST_MENU);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            item.setIntent(intent);
        }

        return true;
    }

    private void setUpUiComponents() {
        ((ImageView)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(TaskListActivity.this,
                        FilterListActivity.class);
                startActivity(intent);
                finish();
            }
        });

        ((TextView)findViewById(R.id.listLabel)).setText(filter.title);

        getListView().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if(event.getAction() != KeyEvent.ACTION_UP)
                    return false;

                // hot-key to set task priority - 1-4 or ALT + Q-R
                if(event.getNumber() >= '1' && event.getNumber() <= '4') {
                    view = getListView().getSelectedView();
                    int importance = event.getNumber() - '1';
                    Task task = ((ViewHolder)view.getTag()).task;
                    task.setValue(Task.IMPORTANCE, importance);
                    taskService.save(task, false);
                    taskAdapter.setFieldContentsAndVisibility(view);
                }

                return false;
            }
        });

        quickAddBox = (EditText) findViewById(R.id.quickAddText);
        quickAddBox.setOnEditorActionListener(new OnEditorActionListener() {
            /**
             * When user presses enter, quick-add the task
             */
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL && quickAddBox.getText().length() > 0) {
                    quickAddTask(quickAddBox.getText().toString(), true);
                    return true;
                }
                return false;
            }
        });

        quickAddButton = ((ImageButton)findViewById(R.id.quickAddButton));
        quickAddBox.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event != null && event.isPrintingKey())
                    quickAddButton.setVisibility(View.VISIBLE);
                return false;
            }
        });

        quickAddButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(quickAddBox.getText().length() > 0) {
                    quickAddTask(quickAddBox.getText().toString(), true);
                }
            }
        });

        ((ImageButton)findViewById(R.id.extendedAddButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(), false);
                Intent intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
                intent.putExtra(TaskEditActivity.ID_TOKEN, task.getId());
                startActivityForResult(intent, ACTIVITY_EDIT_TASK);
            }
        });

        String reminder = getIntent().getStringExtra(TOKEN_REMINDER);
        if(reminder != null) {
            findViewById(R.id.reminderContainer).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.reminderLabel)).setText(reminder);
        }
    }

    /**
     * Quick-add a new task
     * @param title
     * @return
     */
    @SuppressWarnings("nls")
    protected Task quickAddTask(String title, boolean selectNewTask) {
        try {
            Task task = new Task();
            task.setValue(Task.TITLE, title.trim());
            ContentValues forMetadata = null;
            if(filter.valuesForNewTasks != null && filter.valuesForNewTasks.size() > 0) {
                ContentValues forTask = new ContentValues();
                forMetadata = new ContentValues();
                for(Entry<String, Object> item : filter.valuesForNewTasks.valueSet()) {
                    if(item.getKey().startsWith(Task.TABLE.name))
                        AndroidUtilities.putInto(forTask, item.getKey(), item.getValue());
                    else
                        AndroidUtilities.putInto(forMetadata, item.getKey(), item.getValue());
                }
                task.mergeWith(forTask);
            }
            taskService.save(task, false);
            if(forMetadata != null && forMetadata.size() > 0) {
                Metadata metadata = new Metadata();
                metadata.setValue(Metadata.TASK, task.getId());
                metadata.mergeWith(forMetadata);
                metadataService.save(metadata);
            }

            TextView quickAdd = (TextView)findViewById(R.id.quickAddText);
            quickAdd.setText(""); //$NON-NLS-1$
            quickAddButton.setVisibility(View.GONE);

            if(selectNewTask) {
                loadTaskListContent(true);
                selectCustomId(task.getId());
            }

            return task;
        } catch (Exception e) {
            exceptionService.displayAndReportError(this, "quick-add-task", e);
            return new Task();
        }
    }

    /* ======================================================================
     * ============================================================ lifecycle
     * ====================================================================== */

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        // update the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = new TasksWidget.UpdateService().buildUpdate(this);
        ComponentName widgetName = new ComponentName(this, TasksWidget.class);
        appWidgetManager.updateAppWidget(widgetName, views);

        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DETAILS));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(detailReceiver);
    }

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class DetailReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Bundle extras = intent.getExtras();
                long taskId = extras.getLong(AstridApiConstants.EXTRAS_TASK_ID);
                TaskDetail detail = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                taskAdapter.addDetails(getListView(), taskId, detail);
            } catch (Exception e) {
                exceptionService.reportError("receive-detail-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON), e);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)
            AndroidUtilities.setCurrentlyActive(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadTaskListContent(true);
        taskService.cleanup();
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        // do nothing
    }

    /**
     * Detect when user is flinging the task, disable task adapter loading
     * when this occurs to save resources and time.
     */
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
        case OnScrollListener.SCROLL_STATE_IDLE:
            if(taskAdapter.isFling)
                taskAdapter.notifyDataSetChanged();
            taskAdapter.isFling = false;
            break;
        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
            if(taskAdapter.isFling)
                taskAdapter.notifyDataSetChanged();
            taskAdapter.isFling = false;
            break;
        case OnScrollListener.SCROLL_STATE_FLING:
            taskAdapter.isFling = true;
            break;
        }
    }

    /* ======================================================================
     * =================================================== managing list view
     * ====================================================================== */

    /**
     * Load or re-load action items and update views
     * @param requery
     */
    public void loadTaskListContent(boolean requery) {
        int oldListItemSelected = getListView().getSelectedItemPosition();
        Cursor taskCursor = taskAdapter.getCursor();

        if(requery) {
            taskCursor.requery();
            taskAdapter.notifyDataSetChanged();
            taskAdapter.flushDetailCache();
        }
        startManagingCursor(taskCursor);

        if(oldListItemSelected != ListView.INVALID_POSITION &&
                oldListItemSelected < taskCursor.getCount())
            getListView().setSelection(oldListItemSelected);
    }

    /**
     * Fill in the Task List with current items
     * @param withCustomId force task with given custom id to be part of list
     */
    @SuppressWarnings("nls")
    protected void setUpTaskList() {
        // use default ordering if none specified
        if(!filter.sqlQuery.toUpperCase().contains("ORDER BY")) {
            filter.sqlQuery += " ORDER BY " + TaskService.defaultTaskOrder();
        }

        // perform query
        TodorooCursor<Task> currentCursor = taskService.fetchFiltered(
                filter, TaskAdapter.PROPERTIES);
        startManagingCursor(currentCursor);

        // set up list adapters
        taskAdapter = new TaskAdapter(this, R.layout.task_adapter_row,
                currentCursor, false, null);
        setListAdapter(taskAdapter);
        getListView().setOnScrollListener(this);
        registerForContextMenu(getListView());

        loadTaskListContent(false);
    }

    /**
     * Select a custom task id in the list. If it doesn't exist, create
     * a new custom filter
     * @param withCustomId
     */
    @SuppressWarnings("nls")
    private void selectCustomId(long withCustomId) {
        // if already in the list, select it
        TodorooCursor<Task> currentCursor = (TodorooCursor<Task>)taskAdapter.getCursor();
        for(int i = 0; i < currentCursor.getCount(); i++) {
            currentCursor.moveToPosition(i);
            if(currentCursor.get(Task.ID) == withCustomId) {
                getListView().setSelection(i);
                return;
            }
        }

        // create a custom cursor
        if(!filter.sqlQuery.contains("WHERE"))
            filter.sqlQuery += " WHERE " + TaskCriteria.byId(withCustomId);
        else
            filter.sqlQuery = filter.sqlQuery.replace("WHERE ", "WHERE " +
                    TaskCriteria.byId(withCustomId) + " OR ");
        currentCursor = taskService.fetchFiltered(filter, TaskAdapter.PROPERTIES);
        startManagingCursor(currentCursor);

        taskAdapter.changeCursor(currentCursor);

        // update title
        filter.title = getString(R.string.TLA_custom);
        ((TextView)findViewById(R.id.listLabel)).setText(filter.title);

        // try selecting again
        for(int i = 0; i < currentCursor.getCount(); i++) {
            currentCursor.moveToPosition(i);
            if(currentCursor.get(Task.ID) == withCustomId) {
                getListView().setSelection(i);
                break;
            }
        }
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    protected Pair<CharSequence, Intent>[] contextMenuItemCache = null;

    protected void loadContextMenuIntents() {
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(queryIntent, 0);
        int length = resolveInfoList.size();
        contextMenuItemCache = new Pair[length];
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            Intent intent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            CharSequence title = resolveInfo.loadLabel(pm);
            contextMenuItemCache[i] = Pair.create(title, intent);
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo)menuInfo;
        Task task = ((ViewHolder)adapterInfo.targetView.getTag()).task;
        int id = (int)task.getId();
        menu.setHeaderTitle(task.getValue(Task.TITLE));

        if(task.isDeleted()) {
            menu.add(id, CONTEXT_MENU_UNDELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextUndeleteTask);
        } else {
            menu.add(id, CONTEXT_MENU_EDIT_TASK_ID, Menu.NONE,
                        R.string.TAd_contextEditTask);
            menu.add(id, CONTEXT_MENU_DELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextDeleteTask);

            if(Constants.DEBUG) {
                menu.add("--- debug ---");
                menu.add(id, CONTEXT_MENU_DEBUG, Menu.NONE,
                        "when alarm?");
                menu.add(id, CONTEXT_MENU_DEBUG + 1, Menu.NONE,
                        "make notification");
            }

            if(contextMenuItemCache == null)
                return;

            // ask about plug-ins
            long taskId = task.getId();
            for(int i = 0; i < contextMenuItemCache.length; i++) {
                Intent intent = contextMenuItemCache[i].getRight();
                MenuItem item = menu.add(id, CONTEXT_MENU_ADDON_INTENT_ID, Menu.NONE,
                        contextMenuItemCache[i].getLeft());
                intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
                item.setIntent(intent);
            }
        }
    }

    /** Show a dialog box and delete the task specified */
    private void deleteTask(final Task task) {
        new AlertDialog.Builder(this).setTitle(R.string.DLG_confirm_title)
                .setMessage(R.string.DLG_delete_this_task_question).setIcon(
                        android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                taskService.delete(task);
                                loadTaskListContent(true);
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        Intent intent;
        long itemId;

        // handle my own menus
        switch (item.getItemId()) {
        case MENU_ADDONS_ID:
            dialogUtilities.okDialog(
                    this,
                    "if this were real life, I would display your " + //$NON-NLS-1$
                    "add-ons so you could enable/disable/rearrange them.", //$NON-NLS-1$
                    null);
            return true;
        case MENU_SETTINGS_ID:
            intent = new Intent(this, EditPreferences.class);
            startActivityForResult(intent, ACTIVITY_SETTINGS);
            return true;
        case MENU_HELP_ID:
            // TODO
            return true;


        // context menu items
        case CONTEXT_MENU_ADDON_INTENT_ID: {
            intent = item.getIntent();
            AndroidUtilities.startExternalIntent(this, intent, ACTIVITY_MENU_EXTERNAL);
            return true;
        }

        case CONTEXT_MENU_EDIT_TASK_ID: {
            itemId = item.getGroupId();
            intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
            intent.putExtra(TaskEditActivity.ID_TOKEN, itemId);
            startActivityForResult(intent, ACTIVITY_EDIT_TASK);
            return true;
        }

        case CONTEXT_MENU_DELETE_TASK_ID: {
            itemId = item.getGroupId();
            Task task = new Task();
            task.setId(itemId);
            deleteTask(task);
            return true;
        }

        case CONTEXT_MENU_UNDELETE_TASK_ID: {
            itemId = item.getGroupId();
            Task task = new Task();
            task.setId(itemId);
            task.setValue(Task.DELETION_DATE, 0L);
            taskService.save(task, false);
            loadTaskListContent(true);
            return true;
        }

        // --- debug

        case CONTEXT_MENU_DEBUG: {
            itemId = item.getGroupId();
            Task task = new Task();
            task.setId(itemId);
            final ReminderService reminderService = new ReminderService();
            reminderService.setScheduler(new AlarmScheduler() {
                @Override
                public void createAlarm(Task theTask, long time, int type) {
                    Toast.makeText(TaskListActivity.this, "Scheduled Alarm: " + //$NON-NLS-1$
                            new Date(time), Toast.LENGTH_LONG).show();
                    reminderService.setScheduler(null);
                }
            });
            reminderService.scheduleAlarm(task);
            if(reminderService.getScheduler() != null)
                Toast.makeText(this, "No alarms", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            return true;
        }

        case CONTEXT_MENU_DEBUG + 1: {
            itemId = item.getGroupId();
            new Notifications().showNotification(itemId, 0, "test reminder"); //$NON-NLS-1$
            return true;
        }

        }

        return false;
    }

}