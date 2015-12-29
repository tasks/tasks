/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.helper.SyncActionHelper;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskDuplicator;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.subtasks.SubtasksUpdater;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.ui.QuickAddBar;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingListFragment;
import org.tasks.injection.Injector;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.NavigationDrawerFragment;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.intents.TaskIntents.getNewTaskIntent;

/**
 * Primary activity for the Bente application. Shows a list of upcoming tasks
 * and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListFragment extends InjectingListFragment implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG_TASKLIST_FRAGMENT = "tasklist_fragment"; //$NON-NLS-1$

    // --- activities

    public static final long AUTOSYNC_INTERVAL = 90000L;
    private static final long BACKGROUND_REFRESH_INTERVAL = 120000L;
    private static final long WAIT_BEFORE_AUTOSYNC = 2000L;
    public static final int ACTIVITY_EDIT_TASK = 0;
    public static final int ACTIVITY_REQUEST_NEW_FILTER = 5;

    // --- menu codes

    protected static final int CONTEXT_MENU_COPY_TASK_ID = R.string.TAd_contextCopyTask;
    protected static final int CONTEXT_MENU_DELETE_TASK_ID = R.string.TAd_contextDeleteTask;
    protected static final int CONTEXT_MENU_UNDELETE_TASK_ID = R.string.TAd_contextUndeleteTask;
    protected static final int CONTEXT_MENU_PURGE_TASK_ID = R.string.TAd_contextPurgeTask;

    // --- constants

    /** token for passing a {@link Filter} object through extras */
    public static final String TOKEN_FILTER = "filter"; //$NON-NLS-1$

    private static final String TOKEN_EXTRAS = "extras"; //$NON-NLS-1$

    // --- instance variables

    @Inject TaskService taskService;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject SyncV2Service syncService;
    @Inject TaskDeleter taskDeleter;
    @Inject TaskDuplicator taskDuplicator;
    @Inject @ForActivity Context context;
    @Inject ActivityPreferences preferences;
    @Inject NotificationManager notificationManager;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject Injector injector;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject DialogBuilder dialogBuilder;

    protected Resources resources;
    protected TaskAdapter taskAdapter = null;
    protected RefreshReceiver refreshReceiver = new RefreshReceiver();
    protected final AtomicReference<String> sqlQueryTemplate = new AtomicReference<>();
    protected SyncActionHelper syncActionHelper;
    protected Filter filter;
    protected QuickAddBar quickAddBar = new QuickAddBar();

    private Timer backgroundTimer;
    protected Bundle extras;
    protected boolean isInbox;
    protected boolean isTodayFilter;
    protected TaskListMetadata taskListMetadata;

    // --- fragment handling variables
    protected OnTaskListItemClickedListener mListener;
    private boolean mDualFragments = false;

    protected SwipeRefreshLayout listView;
    protected SwipeRefreshLayout emptyView;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    /**
     * Instantiates and returns an instance of TaskListFragment (or some subclass). Custom types of
     * TaskListFragment can be created, with the following precedence:
     *
     * --If the filter is of type {@link FilterWithCustomIntent}, the task list type it specifies will be used
     * --Otherwise, the specified customComponent will be used
     *
     * See also: instantiateWithFilterAndExtras(Filter, Bundle) which uses TaskListFragment as the default
     * custom component.
     */
    public static TaskListFragment instantiateWithFilterAndExtras(Filter filter, Bundle extras, Class<?> customComponent) {
        Class<?> component = customComponent;
        if (filter instanceof FilterWithCustomIntent && component == null) {
            try {
                component = Class.forName(((FilterWithCustomIntent) filter).customTaskList.getClassName());
            } catch (Exception e) {
                // Invalid
                Timber.e(e, e.getMessage());
            }
        }
        if (component == null) {
            component = TaskListFragment.class;
        }

        TaskListFragment newFragment;
        try {
            newFragment = (TaskListFragment) component.newInstance();
        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            Timber.e(e, e.getMessage());
            newFragment = new TaskListFragment();
        }
        Bundle args = new Bundle();
        args.putBundle(TOKEN_EXTRAS, extras);
        newFragment.setArguments(args);
        return newFragment;
    }

    @Override
    public void onRefresh() {
        if (!syncActionHelper.performSyncAction()) {
            refresh();
        }
    }

    public void setSyncOngoing(boolean ongoing) {
        listView.setRefreshing(ongoing);
        emptyView.setRefreshing(ongoing);
    }

    /**
     * Container Activity must implement this interface and we ensure that it
     * does during the onAttach() callback
     */
    public interface OnTaskListItemClickedListener {
        void onTaskListItemClicked(long taskId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback
        // interface
        try {
            mListener = (OnTaskListItemClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTaskListItemClickedListener"); //$NON-NLS-1$
        }
    }

    /**
     * @return view to attach to the body of the task list. must contain two
     *         elements, a view with id android:id/empty and a list view with id
     *         android:id/list. It should NOT be attached to root
     */
    protected View getListBody(ViewGroup root) {
        return getActivity().getLayoutInflater().inflate(
                R.layout.task_list_body_standard, root, false);
    }

    /** Called when loading up the activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extras = getArguments() != null ? getArguments().getBundle(TOKEN_EXTRAS) : null;
        if (extras == null) {
            extras = new Bundle(); // Just need an empty one to prevent potential null pointers
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(
                R.layout.task_list_activity, container, false);
        parent.findViewById(R.id.fab).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getBoolean(R.bool.two_pane_layout)) {
                    Task task = quickAddBar.quickAddTask();
                    onTaskListItemClicked(task.getId());
                } else {
                    ((AstridActivity) getActivity()).startEditActivity(getNewTaskIntent(getActivity(), filter));
                }
            }
        });
        View body = getListBody(parent);
        listView = (SwipeRefreshLayout) body.findViewById(R.id.swipe_layout);
        emptyView = (SwipeRefreshLayout) parent.findViewById(R.id.swipe_layout_empty);
        setupRefresh(listView);
        setupRefresh(emptyView);
        ((ListView) listView.findViewById(android.R.id.list)).setEmptyView(emptyView);
        ((ViewGroup) parent.findViewById(R.id.task_list_body)).addView(body, 0);
        return parent;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = getListView();
        View footer = getActivity().getLayoutInflater().inflate(R.layout.task_list_footer, listView, false);
        listView.addFooterView(footer, null, false);
    }

    private void setupRefresh(SwipeRefreshLayout layout) {
        layout.setOnRefreshListener(this);
        Resources resources = getResources();
        layout.setColorSchemeColors(
                resources.getColor(R.color.importance_1),
                resources.getColor(R.color.importance_2),
                resources.getColor(R.color.importance_3),
                resources.getColor(R.color.importance_4));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        resources = getResources();
        setHasOptionsMenu(true);
        syncActionHelper = new SyncActionHelper(gtasksPreferenceService, syncService, getActivity(), preferences);
        setUpUiComponents();
        initializeData();
        quickAddBar.initialize(injector, (TaskListActivity) getActivity(), this);

        Fragment filterlistFrame = getFragmentManager().findFragmentById(
                NavigationDrawerFragment.FRAGMENT_NAVIGATION_DRAWER);
        mDualFragments = (filterlistFrame != null)
                && filterlistFrame.isInLayout();

        if (mDualFragments) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setItemsCanFocus(false);
        }

        if ((this instanceof SubtasksListFragment) || (this instanceof SubtasksTagListFragment)) {
            return;
        }

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (taskAdapter != null) {
                    TodorooCursor<Task> cursor = (TodorooCursor<Task>) taskAdapter.getItem(position);
                    Task task = new Task(cursor);
                    if (task.isDeleted()) {
                        return;
                    }

                    onTaskListItemClicked(id);
                }
            }
        });
    }

    /**
     * @return the current tag you are viewing, or null if you're not viewing a tag
     */
    public TagData getActiveTagData() {
        return null;
    }

    protected void initializeData() {
        if (extras != null && extras.containsKey(TOKEN_FILTER)) {
            filter = extras.getParcelable(TOKEN_FILTER);
            extras.remove(TOKEN_FILTER); // Otherwise writing this filter to parcel gives infinite recursion
        } else {
            filter = BuiltInFilterExposer.getMyTasksFilter(resources);
        }
        filter.setFilterQueryOverride(null);
        isInbox = BuiltInFilterExposer.isInbox(context, filter);
        isTodayFilter = false;
        if (!isInbox) {
            isTodayFilter = BuiltInFilterExposer.isTodayFilter(context, filter);
        }

        initializeTaskListMetadata();

        setUpTaskList();
        ((AstridActivity) getActivity()).setupActivityFragment(getActiveTagData());
    }

    protected void initializeTaskListMetadata() {
        TagData td = getActiveTagData();
        String tdId;
        if (td == null) {
            String filterId = null;
            String prefId = null;
            if (isInbox) {
                filterId = TaskListMetadata.FILTER_ID_ALL;
                prefId = SubtasksUpdater.ACTIVE_TASKS_ORDER;
            } else if (isTodayFilter) {
                filterId = TaskListMetadata.FILTER_ID_TODAY;
                prefId = SubtasksUpdater.TODAY_TASKS_ORDER;
            }
            if (!TextUtils.isEmpty(filterId)) {
                taskListMetadata = taskListMetadataDao.fetchByTagId(filterId, TaskListMetadata.PROPERTIES);
                if (taskListMetadata == null) {
                    String defaultOrder = preferences.getStringValue(prefId);
                    if (TextUtils.isEmpty(defaultOrder)) {
                        defaultOrder = "[]"; //$NON-NLS-1$
                    }
                    defaultOrder = SubtasksHelper.convertTreeToRemoteIds(taskService, defaultOrder);
                    taskListMetadata = new TaskListMetadata();
                    taskListMetadata.setFilter(filterId);
                    taskListMetadata.setTaskIDs(defaultOrder);
                    taskListMetadataDao.createNew(taskListMetadata);
                }
            }
        } else {
            tdId = td.getUuid();
            taskListMetadata = taskListMetadataDao.fetchByTagId(td.getUuid(), TaskListMetadata.PROPERTIES);
            if (taskListMetadata == null && !RemoteModel.isUuidEmpty(tdId)) {
                taskListMetadata = new TaskListMetadata();
                taskListMetadata.setTagUUID(tdId);
                taskListMetadataDao.createNew(taskListMetadata);
            }
        }
        postLoadTaskListMetadata();
    }

    protected void postLoadTaskListMetadata() {
        // Hook
    }

    protected void setUpUiComponents() {
        // set listener for quick-changing task priority
        getListView().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP || view == null) {
                    return false;
                }

                boolean filterOn = getListView().isTextFilterEnabled();
                View selected = getListView().getSelectedView();

                // hot-key to set task priority - 1-4 or ALT + Q-R
                if (!filterOn && event.getUnicodeChar() >= '1'
                        && event.getUnicodeChar() <= '4' && selected != null) {
                    int importance = event.getNumber() - '1';
                    Task task = ((ViewHolder) selected.getTag()).task;
                    task.setImportance(importance);
                    taskService.save(task);
                    taskAdapter.setFieldContentsAndVisibility(selected);
                }
                // filter
                else if (!filterOn && event.getUnicodeChar() != 0) {
                    getListView().setTextFilterEnabled(true);
                    getListView().setFilterText(
                            Character.toString((char) event.getUnicodeChar()));
                }
                // turn off filter if nothing is selected
                else if (filterOn
                        && TextUtils.isEmpty(getListView().getTextFilter())) {
                    getListView().setTextFilterEnabled(false);
                }

                return false;
            }
        });
    }

    public void transitionForTaskEdit() {
        AndroidUtilities.callOverridePendingTransition(getActivity(),
                R.anim.slide_left_in, R.anim.slide_left_out);
    }

    private void setUpBackgroundJobs() {
        backgroundTimer = new Timer();

        // start a thread to refresh periodically
        backgroundTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // refresh if conditions match
                Flags.checkAndClear(Flags.REFRESH);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            refresh();
                        } catch (IllegalStateException e) {
                            // view may have been destroyed
                            Timber.e(e, e.getMessage());
                        }
                    }
                });
            }
        }, BACKGROUND_REFRESH_INTERVAL, BACKGROUND_REFRESH_INTERVAL);
    }

    /*
     * ======================================================================
     * ============================================================ lifecycle
     * ======================================================================
     */

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(refreshReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));

        if (Flags.checkAndClear(Flags.REFRESH)) {
            refresh();
        }

        setUpBackgroundJobs();

        refreshFilterCount();

        initiateAutomaticSync();
    }

    protected boolean isCurrentTaskListFragment() {
        AstridActivity activity = (AstridActivity) getActivity();
        if (activity != null) {
            return activity.getTaskListFragment() == this;
        }
        return false;
    }

    public final void initiateAutomaticSync() {
        final AstridActivity activity = (AstridActivity) getActivity();
        if (activity == null) {
            return;
        }
        if (activity.fragmentLayout != AstridActivity.LAYOUT_SINGLE) {
            initiateAutomaticSyncImpl();
        } else {
            // In single fragment case, we're using swipe between lists,
            // so wait a couple seconds before initiating the autosync.
            new Thread() {
                @Override
                public void run() {
                    AndroidUtilities.sleepDeep(WAIT_BEFORE_AUTOSYNC);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initiateAutomaticSyncImpl();
                        }
                    });
                }
            }.start();
        }
    }

    /**
     * Implementation of initiation automatic sync. Subclasses should override this method;
     * the above method takes care of calling it in the correct way
     */
    protected void initiateAutomaticSyncImpl() {
        if (isCurrentTaskListFragment() && isInbox) {
            syncActionHelper.initiateAutomaticSync();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(getActivity(), refreshReceiver);

        backgroundTimer.cancel();
    }

    /**
     * Receiver which receives refresh intents
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !AstridApiConstants.BROADCAST_EVENT_REFRESH.equals(intent.getAction())) {
                return;
            }

            final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
            }
        }
    }

    /**
     * Called by the RefreshReceiver when the task list receives a refresh
     * broadcast. Subclasses should override this.
     */
    protected void refresh() {
        try {
            AstridActivity astridActivity = (AstridActivity) getActivity();
            TaskEditFragment taskEditFragment = astridActivity == null ? null : astridActivity.getTaskEditFragment();
            Task model = taskEditFragment == null ? null : taskEditFragment.model;
            taskDeleter.deleteTasksWithEmptyTitles(model == null ? null : model.getId());
        } catch(Exception e) {
            Timber.e(e, e.getMessage());
        }
        loadTaskListContent();
        setSyncOngoing(false);
    }

    /*
     * ======================================================================
     * =================================================== managing list view
     * ======================================================================
     */

    /**
     * Load or re-load action items and update views
     */
    public void loadTaskListContent() {
        if (taskAdapter == null) {
            setUpTaskList();
            return;
        }

        Cursor taskCursor = taskAdapter.getCursor();

        taskCursor.requery();
        taskAdapter.notifyDataSetChanged();

        if (getView() != null) { // This was happening sometimes
            int oldListItemSelected = getListView().getSelectedItemPosition();
            if (oldListItemSelected != ListView.INVALID_POSITION
                    && oldListItemSelected < taskCursor.getCount()) {
                getListView().setSelection(oldListItemSelected);
            }
        }
    }

    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {

        return new TaskAdapter(context, preferences, taskAttachmentDao, taskService, this, cursor, sqlQueryTemplate,
                new OnCompletedTaskListener() {
                    @Override
                    public void onCompletedTask(Task item, boolean newState) {
                    }
                }, dialogBuilder);
    }

    public static final String TAGS_METADATA_JOIN = "for_tags"; //$NON-NLS-1$

    public  static final String FILE_METADATA_JOIN = "for_actions"; //$NON-NLS-1$


    /**
     * Fill in the Task List with current items
     */
    public void setUpTaskList() {
        if (filter == null) {
            return;
        }

        TodorooCursor<Task> currentCursor = constructCursor();
        if (currentCursor == null) {
            return;
        }

        // set up list adapters
        taskAdapter = createTaskAdapter(currentCursor);

        setListAdapter(taskAdapter);
        registerForContextMenu(getListView());

        loadTaskListContent();
    }

    public Property<?>[] taskProperties() {
        return TaskAdapter.PROPERTIES;
    }

    public Filter getFilter() {
        return filter;
    }

    private TodorooCursor<Task> constructCursor() {
        String tagName = null;
        if (getActiveTagData() != null) {
            tagName = getActiveTagData().getName();
        }

        Criterion tagsJoinCriterion = Criterion.and(
                Field.field(TAGS_METADATA_JOIN + "." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY), //$NON-NLS-1$
                Field.field(TAGS_METADATA_JOIN + "." + Metadata.DELETION_DATE.name).eq(0),
                Task.ID.eq(Field.field(TAGS_METADATA_JOIN + "." + Metadata.TASK.name)));
        if (tagName != null) {
            tagsJoinCriterion = Criterion.and(tagsJoinCriterion, Field.field(TAGS_METADATA_JOIN + "." + TaskToTagMetadata.TAG_NAME.name).neq(tagName));
        }

        // TODO: For now, we'll modify the query to join and include the things like tag data here.
        // Eventually, we might consider restructuring things so that this query is constructed elsewhere.
        String joinedQuery =
                Join.left(Metadata.TABLE.as(TAGS_METADATA_JOIN),
                        tagsJoinCriterion).toString() //$NON-NLS-1$
                + Join.left(TaskAttachment.TABLE.as(FILE_METADATA_JOIN), Task.UUID.eq(Field.field(FILE_METADATA_JOIN + "." + TaskAttachment.TASK_UUID.name)))
                + filter.getSqlQuery();

        sqlQueryTemplate.set(SortHelper.adjustQueryForFlagsAndSort(
                preferences, joinedQuery, preferences.getSortMode()));

        String groupedQuery;
        if (sqlQueryTemplate.get().contains("GROUP BY")) {
            groupedQuery = sqlQueryTemplate.get();
        } else if (sqlQueryTemplate.get().contains("ORDER BY")) //$NON-NLS-1$
        {
            groupedQuery = sqlQueryTemplate.get().replace("ORDER BY", "GROUP BY " + Task.ID + " ORDER BY"); //$NON-NLS-1$
        } else {
            groupedQuery = sqlQueryTemplate.get() + " GROUP BY " + Task.ID;
        }
        sqlQueryTemplate.set(groupedQuery);

        // Peform query
        try {
            return taskService.fetchFiltered(
                sqlQueryTemplate.get(), null, taskProperties());
        } catch (SQLiteException e) {
            // We don't show this error anymore--seems like this can get triggered
            // by a strange bug, but there seems to not be any negative side effect.
            // For now, we'll suppress the error
            // See http://astrid.com/home#tags-7tsoi/task-1119pk
            Timber.e(e, e.getMessage());
            return null;
        }
    }

    public void reconstructCursor() {
        TodorooCursor<Task> cursor = constructCursor();
        if (cursor == null || taskAdapter == null) {
            return;
        }
        taskAdapter.changeCursor(cursor);
    }

    /**
     * Select a custom task id in the list. If it doesn't exist, create a new
     * custom filter
     */
    public void selectCustomId(long withCustomId) {
        // if already in the list, select it
        TodorooCursor<Task> currentCursor = (TodorooCursor<Task>) taskAdapter.getCursor();
        for (int i = 0; i < currentCursor.getCount(); i++) {
            currentCursor.moveToPosition(i);
            if (currentCursor.get(Task.ID) == withCustomId) {
                getListView().setSelection(i);
                return;
            }
        }
    }

    /*
     * ======================================================================
     * ============================================================== actions
     * ======================================================================
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
        Task task = ((ViewHolder) adapterInfo.targetView.getTag()).task;
        int id = (int) task.getId();
        menu.setHeaderTitle(task.getTitle());

        if (task.isDeleted()) {
            menu.add(id, CONTEXT_MENU_UNDELETE_TASK_ID, Menu.NONE, R.string.TAd_contextUndeleteTask);
            menu.add(id, CONTEXT_MENU_PURGE_TASK_ID, Menu.NONE, R.string.TAd_contextPurgeTask);
        } else {
            menu.add(id, CONTEXT_MENU_COPY_TASK_ID, Menu.NONE, R.string.TAd_contextCopyTask);
            menu.add(id, CONTEXT_MENU_DELETE_TASK_ID, Menu.NONE, R.string.TAd_contextDeleteTask);
        }
    }

    /** Show a dialog box and delete the task specified */
    private void deleteTask(final Task task) {
        dialogBuilder.newMessageDialog(R.string.delete_tag_confirmation, task.getTitle())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onTaskDelete(task);
                        taskDeleter.delete(task);
                        loadTaskListContent();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onTaskCreated(long id, String uuid) {
    }

    protected void onTaskDelete(Task task) {
        Activity a = getActivity();
        if (a instanceof AstridActivity) {
            AstridActivity activity = (AstridActivity) a;
            TaskEditFragment tef = activity.getTaskEditFragment();
            if (tef != null) {
                if (task.getId() == tef.model.getId()) {
                    tef.discardButtonClick();
                }
            }
        }
        TimerPlugin.updateTimer(notificationManager, taskService, context, task, false);
    }

    public void refreshFilterCount() {
        if (getActivity() instanceof TaskListActivity) {
            ((TaskListActivity) getActivity()).refreshFilterCount();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mDualFragments) {
            setSelection(position);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        long itemId;

        if (!isCurrentTaskListFragment()) {
            return false;
        }

        switch (item.getItemId()) {
        // --- context menu items

        case CONTEXT_MENU_COPY_TASK_ID:
            itemId = item.getGroupId();
            duplicateTask(itemId);
            return true;
        case CONTEXT_MENU_DELETE_TASK_ID: {
            itemId = item.getGroupId();
            Task task = taskService.fetchById(itemId, Task.ID, Task.TITLE, Task.UUID);
            if (task != null) {
                deleteTask(task);
            }
            return true;
        }
        case CONTEXT_MENU_UNDELETE_TASK_ID: {
            itemId = item.getGroupId();
            Task task = new Task();
            task.setId(itemId);
            task.setDeletionDate(0L);
            taskService.save(task);
            loadTaskListContent();
            return true;
        }
        case CONTEXT_MENU_PURGE_TASK_ID: {
            itemId = item.getGroupId();
            Task task = new Task();
            task.setId(itemId);
            TimerPlugin.updateTimer(notificationManager, taskService, getActivity(), task, false);
            taskDeleter.purge(itemId);
            loadTaskListContent();
            return true;
        }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void duplicateTask(long itemId) {
        long cloneId = taskDuplicator.duplicateTask(itemId);

        Intent intent = new Intent(getActivity(), TaskEditActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, cloneId);
        intent.putExtra(TOKEN_FILTER, filter);
        getActivity().startActivityForResult(intent, ACTIVITY_EDIT_TASK);
        transitionForTaskEdit();
    }

    public void onTaskListItemClicked(long taskId) {
        mListener.onTaskListItemClicked(taskId);
    }

    protected boolean hasDraggableOption() {
        return isInbox || isTodayFilter;
    }
}
