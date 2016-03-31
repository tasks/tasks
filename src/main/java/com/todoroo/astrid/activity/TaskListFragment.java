/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.FilterSettingsActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.helper.SyncActionHelper;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskDuplicator;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.timers.TimerPlugin;
import com.todoroo.astrid.voice.VoiceInputAssistant;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.activities.SortActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingListFragment;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.MenuColorizer;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static com.todoroo.astrid.voice.VoiceInputAssistant.voiceInputAvailable;

/**
 * Primary activity for the Bente application. Shows a list of upcoming tasks
 * and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListFragment extends InjectingListFragment implements SwipeRefreshLayout.OnRefreshListener, Toolbar.OnMenuItemClickListener {

    public static TaskListFragment newTaskListFragment(Filter filter) {
        TaskListFragment fragment = new TaskListFragment();
        fragment.filter = filter;
        return fragment;
    }

    private static final String EXTRA_FILTER = "extra_filter";

    public static final String TAG_TASKLIST_FRAGMENT = "tasklist_fragment"; //$NON-NLS-1$

    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private static final int REQUEST_EDIT_FILTER = 11544;
    private static final int REQUEST_SORT = 11545;

    // --- activities

    public static final long AUTOSYNC_INTERVAL = 90000L;
    public static final int ACTIVITY_REQUEST_NEW_FILTER = 5;

    // --- menu codes

    protected static final int CONTEXT_MENU_COPY_TASK_ID = R.string.TAd_contextCopyTask;
    protected static final int CONTEXT_MENU_DELETE_TASK_ID = R.string.TAd_contextDeleteTask;
    protected static final int CONTEXT_MENU_UNDELETE_TASK_ID = R.string.TAd_contextUndeleteTask;
    protected static final int CONTEXT_MENU_PURGE_TASK_ID = R.string.TAd_contextPurgeTask;

    // --- instance variables

    @Inject TaskService taskService;
    @Inject TaskDeleter taskDeleter;
    @Inject TaskDuplicator taskDuplicator;
    @Inject @ForActivity Context context;
    @Inject Preferences preferences;
    @Inject NotificationManager notificationManager;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject DialogBuilder dialogBuilder;
    @Inject SyncActionHelper syncActionHelper;
    @Inject CheckBoxes checkBoxes;
    @Inject VoiceInputAssistant voiceInputAssistant;
    @Inject TaskCreator taskCreator;
    @Inject Broadcaster broadcaster;

    @Bind(R.id.swipe_layout) SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.swipe_layout_empty) SwipeRefreshLayout emptyView;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private TaskAdapter taskAdapter = null;
    private RefreshReceiver refreshReceiver = new RefreshReceiver();
    private TaskListFragmentCallbackHandler callbacks;

    protected final AtomicReference<String> sqlQueryTemplate = new AtomicReference<>();
    protected Filter filter;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    @Override
    public void onRefresh() {
        if (!syncActionHelper.performSyncAction()) {
            refresh();
        }
    }

    public void setSyncOngoing(final boolean ongoing) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(ongoing);
                    emptyView.setRefreshing(ongoing);
                }
            });
        }
    }

    /**
     * Container Activity must implement this interface and we ensure that it
     * does during the onAttach() callback
     */
    public interface TaskListFragmentCallbackHandler {
        void onTaskListItemClicked(long taskId);

        void onNavigationIconClicked();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callbacks = (TaskListFragmentCallbackHandler) activity;
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }

    /**
     * @return view to attach to the body of the task list. must contain two
     *         elements, a view with id android:id/empty and a list view with id
     *         android:id/list. It should NOT be attached to root
     */
    protected int getListBody() {
        return R.layout.task_list_body_standard;
    }

    /** Called when loading up the activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            filter = savedInstanceState.getParcelable(EXTRA_FILTER);
        }

        if (filter == null) {
            filter = BuiltInFilterExposer.getMyTasksFilter(getResources());
        }

        setTaskAdapter();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_FILTER, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_task_list, container, false);
        ((ViewGroup) parent.findViewById(R.id.task_list_body)).addView(inflater.inflate(getListBody(), null), 0);
        ButterKnife.bind(this, parent);
        setupRefresh(swipeRefreshLayout);
        setupRefresh(emptyView);
        ListView listView = (ListView) swipeRefreshLayout.findViewById(android.R.id.list);
        listView.setEmptyView(emptyView);

        toolbar.setTitle(filter.listingTitle);
        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_menu_24dp));
        DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
        toolbar.setNavigationIcon(drawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callbacks.onNavigationIconClicked();
            }
        });
        inflateMenu(toolbar);
        Menu menu = toolbar.getMenu();
        for (int i = 0 ; i < menu.size() ; i++) {
            MenuColorizer.colorMenuItem(menu.getItem(i), getResources().getColor(android.R.color.white));
        }
        toolbar.setOnMenuItemClickListener(this);
        setupMenu(menu);

        return parent;
    }

    protected void inflateMenu(Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.menu_task_list_fragment);
        if (filter instanceof CustomFilter && ((CustomFilter) filter).getId() > 0) {
            toolbar.inflateMenu(R.menu.menu_custom_filter);
        }
    }

    private void setupMenu(Menu menu) {
        MenuItem hidden = menu.findItem(R.id.menu_show_hidden);
        if (preferences.getBoolean(R.string.p_show_hidden_tasks, false)) {
            hidden.setChecked(true);
        }
        MenuItem completed = menu.findItem(R.id.menu_show_completed);
        if (preferences.getBoolean(R.string.p_show_completed_tasks, false)) {
            completed.setChecked(true);
        }
        if (this instanceof GtasksListFragment) {
            menu.findItem(R.id.menu_sort).setVisible(false);
            completed.setChecked(true);
            completed.setEnabled(false);
        }

        if (this instanceof SubtasksTagListFragment || this instanceof SubtasksListFragment) {
            hidden.setChecked(true);
            hidden.setEnabled(false);
        }

        menu.findItem(R.id.menu_voice_add).setVisible(voiceInputAvailable(getActivity()));
        final MenuItem item = menu.findItem(R.id.menu_search);
        final SearchView actionView = (SearchView) MenuItemCompat.getActionView(item);
        actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                query = query.trim();
                String title = getString(R.string.FLA_search_filter, query);
                Filter savedFilter = new Filter(title,
                        new QueryTemplate().where
                                (Criterion.or(Task.NOTES.
                                                        like(
                                                                "%" + //$NON-NLS-1$
                                                                        query + "%"
                                                        ),
                                                Task.TITLE.
                                                        like(
                                                                "%" + //$NON-NLS-1$
                                                                        query + "%"
                                                        )
                                        )
                                ), null);

                ((TaskListActivity) getActivity()).onFilterItemClicked(savedFilter);
                MenuItemCompat.collapseActionView(item);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_voice_add:
                voiceInputAssistant.startVoiceRecognitionActivity(R.string.voice_create_prompt);
                return true;
            case R.id.menu_sort:
                startActivityForResult(new Intent(getActivity(), SortActivity.class) {{
                    putExtra(SortActivity.EXTRA_MANUAL_ENABLED, hasDraggableOption());
                }}, REQUEST_SORT);
                return true;
            case R.id.menu_show_hidden:
                item.setChecked(!item.isChecked());
                preferences.setBoolean(R.string.p_show_hidden_tasks, item.isChecked());
                reconstructCursor();
                broadcaster.refresh();
                return true;
            case R.id.menu_show_completed:
                item.setChecked(!item.isChecked());
                preferences.setBoolean(R.string.p_show_completed_tasks, item.isChecked());
                reconstructCursor();
                broadcaster.refresh();
                return true;
            case R.id.menu_filter_settings:
                startActivityForResult(new Intent(getActivity(), FilterSettingsActivity.class) {{
                    putExtra(FilterSettingsActivity.TOKEN_FILTER, filter);
                }}, REQUEST_EDIT_FILTER);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.fab)
    void createNewTask() {
        Task task = addTask("");
        onTaskListItemClicked(task.getId());
    }

    public Task addTask(String title) {
        return taskService.createWithValues(filter.valuesForNewTasks, title);
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
        layout.setColorSchemeColors(checkBoxes.getPriorityColorsArray());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        final ListView listView = getListView();
        registerForContextMenu(listView);

        // set listener for quick-changing task priority
        listView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP || view == null) {
                    return false;
                }

                boolean filterOn = listView.isTextFilterEnabled();
                View selected = listView.getSelectedView();

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
                    listView.setTextFilterEnabled(true);
                    listView.setFilterText(
                            Character.toString((char) event.getUnicodeChar()));
                }
                // turn off filter if nothing is selected
                else if (filterOn
                        && TextUtils.isEmpty(listView.getTextFilter())) {
                    listView.setTextFilterEnabled(false);
                }

                return false;
            }
        });
        filter.setFilterQueryOverride(null);

        setListAdapter(taskAdapter);

        loadTaskListContent();

        if (getResources().getBoolean(R.bool.two_pane_layout)) {
            // In dual-pane mode, the list view highlights the selected item.
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemsCanFocus(false);
        }

        if ((this instanceof SubtasksListFragment) || (this instanceof SubtasksTagListFragment)) {
            return;
        }

        listView.setOnItemClickListener(new OnItemClickListener() {
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

    /*
     * ======================================================================
     * ============================================================ lifecycle
     * ======================================================================
     */

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(refreshReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));

        refreshFilterCount();

        initiateAutomaticSyncImpl();

        refresh();
    }

    /**
     * Implementation of initiation automatic sync. Subclasses should override this method;
     * the above method takes care of calling it in the correct way
     */
    protected void initiateAutomaticSyncImpl() {
        if (BuiltInFilterExposer.isInbox(context, filter)) {
            syncActionHelper.initiateAutomaticSync();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(getActivity(), refreshReceiver);
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
        loadTaskListContent();

        setSyncOngoing(gtasksPreferenceService.isOngoing());
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
            setTaskAdapter();
            return;
        }

        Cursor taskCursor = taskAdapter.getCursor();

        taskCursor.requery();
        taskAdapter.notifyDataSetChanged();
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
    public void setTaskAdapter() {
        if (filter == null) {
            return;
        }

        TodorooCursor<Task> currentCursor = constructCursor();
        if (currentCursor == null) {
            return;
        }

        // set up list adapters
        taskAdapter = createTaskAdapter(currentCursor);
    }

    public Property<?>[] taskProperties() {
        return TaskAdapter.PROPERTIES;
    }

    public Filter getFilter() {
        return filter;
    }

    private TodorooCursor<Task> constructCursor() {
        Criterion tagsJoinCriterion = Criterion.and(
                Field.field(TAGS_METADATA_JOIN + "." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY), //$NON-NLS-1$
                Field.field(TAGS_METADATA_JOIN + "." + Metadata.DELETION_DATE.name).eq(0),
                Task.ID.eq(Field.field(TAGS_METADATA_JOIN + "." + Metadata.TASK.name)));

        if (this instanceof TagViewFragment) {
            tagsJoinCriterion = Criterion.and(tagsJoinCriterion, Field.field(TAGS_METADATA_JOIN + "." + TaskToTagMetadata.TAG_NAME.name).neq(filter.listingTitle));
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
        TaskListActivity activity = (TaskListActivity) getActivity();
        TaskEditFragment tef = activity.getTaskEditFragment();
        if (tef != null) {
            if (task.getId() == tef.model.getId()) {
                tef.discard();
            }
        }
        TimerPlugin.stopTimer(notificationManager, taskService, context, task);
    }

    public void refreshFilterCount() {
        if (getActivity() instanceof TaskListActivity) {
            ((TaskListActivity) getActivity()).refreshFilterCount();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (getResources().getBoolean(R.bool.two_pane_layout)) {
            setSelection(position);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Callback<String> quickAddTask = new Callback<String>() {
                    @Override
                    public void apply(String title) {
                        Task task = addTask(title);
                        taskCreator.addToCalendar(task);
                        onTaskListItemClicked(task.getId());
                        loadTaskListContent();
                        onTaskCreated(task.getId(), task.getUUID());
                    }
                };
                voiceInputAssistant.handleActivityResult(data, quickAddTask);
            }
        } else if (requestCode == REQUEST_EDIT_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                String action = data.getAction();
                if (AstridApiConstants.BROADCAST_EVENT_FILTER_RENAMED.equals(action)) {
                    CustomFilter customFilter = data.getParcelableExtra(FilterSettingsActivity.TOKEN_FILTER);
                    ((TaskListActivity) getActivity()).onFilterItemClicked(customFilter);
                } else if(AstridApiConstants.BROADCAST_EVENT_FILTER_DELETED.equals(action)) {
                    ((TaskListActivity) getActivity()).onFilterItemClicked(BuiltInFilterExposer.getMyTasksFilter(getResources()));
                }

                ((TaskListActivity) getActivity()).refreshNavigationDrawer();
                broadcaster.refresh();
            }
        } else if (requestCode == REQUEST_SORT) {
            if (resultCode == Activity.RESULT_OK) {
                broadcaster.refresh();
                ((TaskListActivity) getActivity()).onFilterItemClicked(filter);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        long itemId;

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
            TimerPlugin.stopTimer(notificationManager, taskService, getActivity(), task);
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
        onTaskListItemClicked(cloneId);
    }

    public void onTaskListItemClicked(long taskId) {
        callbacks.onTaskListItemClicked(taskId);
    }

    protected boolean hasDraggableOption() {
        return BuiltInFilterExposer.isInbox(context, filter) || BuiltInFilterExposer.isTodayFilter(context, filter);
    }
}
