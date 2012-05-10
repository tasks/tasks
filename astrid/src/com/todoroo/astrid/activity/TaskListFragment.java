package com.todoroo.astrid.activity;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.TagUpdatesActivity;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.SortSelectionActivity.OnSortSelectedListener;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.SyncActionHelper;
import com.todoroo.astrid.helper.TaskListContextMenuExtensionLoader;
import com.todoroo.astrid.helper.TaskListContextMenuExtensionLoader.ContextMenuItem;
import com.todoroo.astrid.reminders.ReminderDebugContextActions;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.ui.QuickAddBar;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.welcome.HelpInfoPopover;
import com.todoroo.astrid.welcome.tutorial.WelcomeWalkthrough;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Primary activity for the Bente application. Shows a list of upcoming tasks
 * and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListFragment extends ListFragment implements OnScrollListener,
        OnSortSelectedListener {

    public static final String TAG_TASKLIST_FRAGMENT = "tasklist_fragment"; //$NON-NLS-1$

    // --- activities

    private static final long BACKGROUND_REFRESH_INTERVAL = 120000L;
    private static final long WAIT_BEFORE_AUTOSYNC = 2000L;
    public static final int ACTIVITY_EDIT_TASK = 0;
    public static final int ACTIVITY_SETTINGS = 1;
    public static final int ACTIVITY_SORT = 2;
    public static final int ACTIVITY_ADDONS = 3;
    public static final int ACTIVITY_MENU_EXTERNAL = 4;
    public static final int ACTIVITY_REQUEST_NEW_FILTER = 5;

    // --- menu codes

    public static final int MENU_ADDONS_ID = R.string.TLA_menu_addons;
    protected static final int MENU_SETTINGS_ID = R.string.TLA_menu_settings;
    protected static final int MENU_SORT_ID = R.string.TLA_menu_sort;
    protected static final int MENU_SYNC_ID = R.string.TLA_menu_sync;
    protected static final int MENU_SUPPORT_ID = R.string.TLA_menu_support;
    public static final int MENU_NEW_FILTER_ID = R.string.FLA_new_filter;
    protected static final int MENU_ADDON_INTENT_ID = Menu.FIRST + 199;

    protected static final int CONTEXT_MENU_EDIT_TASK_ID = R.string.TAd_contextEditTask;
    protected static final int CONTEXT_MENU_COPY_TASK_ID = R.string.TAd_contextCopyTask;
    protected static final int CONTEXT_MENU_DELETE_TASK_ID = R.string.TAd_contextDeleteTask;
    protected static final int CONTEXT_MENU_UNDELETE_TASK_ID = R.string.TAd_contextUndeleteTask;
    protected static final int CONTEXT_MENU_PURGE_TASK_ID = R.string.TAd_contextPurgeTask;
    protected static final int CONTEXT_MENU_BROADCAST_INTENT_ID = Menu.FIRST + 25;
    protected static final int CONTEXT_MENU_PLUGIN_ID_FIRST = Menu.FIRST + 26;

    // --- constants

    /** token for passing a {@link Filter} object through extras */
    public static final String TOKEN_FILTER = "filter"; //$NON-NLS-1$

    private static final String TOKEN_EXTRAS = "extras"; //$NON-NLS-1$

    // --- instance variables

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired Database database;

    @Autowired AddOnService addOnService;

    @Autowired UpgradeService upgradeService;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    private final TaskContextActionExposer[] contextItemExposers = new TaskContextActionExposer[] {
            new ReminderDebugContextActions.MakeNotification(),
            new ReminderDebugContextActions.WhenReminder(),
    };

    protected TaskAdapter taskAdapter = null;
    protected DetailReceiver detailReceiver = new DetailReceiver();
    protected RefreshReceiver refreshReceiver = new RefreshReceiver();
    protected final AtomicReference<String> sqlQueryTemplate = new AtomicReference<String>();
    protected SyncActionHelper syncActionHelper;
    protected Filter filter;
    protected int sortFlags;
    protected int sortSort;
    protected QuickAddBar quickAddBar;

    private Timer backgroundTimer;
    protected Bundle extras;
    private boolean isInbox;

    private final TaskListContextMenuExtensionLoader contextMenuExtensionLoader = new TaskListContextMenuExtensionLoader();

    // --- fragment handling variables
    protected OnTaskListItemClickedListener mListener;
    private boolean mDualFragments = false;

    /*
     * ======================================================================
     * ======================================================= initialization
     * ======================================================================
     */

    static {
        AstridDependencyInjector.initialize();
    }

    /**
     * Instantiates and returns an instance of TaskListFragment (or some subclass). Custom types of
     * TaskListFragment can be created, with the following precedence:
     *
     * --If the filter is of type {@link FilterWithCustomIntent}, the task list type it specifies will be used
     * --Otherwise, the specified customComponent will be used
     *
     * See also: instantiateWithFilterAndExtras(Filter, Bundle) which uses TaskListFragment as the default
     * custom component.
     * @param filter
     * @param extras
     * @param customComponent
     * @return
     */
    @SuppressWarnings("nls")
    public static TaskListFragment instantiateWithFilterAndExtras(Filter filter, Bundle extras, Class<?> customComponent) {
        Class<?> component = customComponent;
        if (filter instanceof FilterWithCustomIntent) {
            try {
                component = Class.forName(((FilterWithCustomIntent) filter).customTaskList.getClassName());
            } catch (Exception e) {
                // Invalid
            }
        }
        TaskListFragment newFragment;
        try {
            newFragment = (TaskListFragment) component.newInstance();
        } catch (java.lang.InstantiationException e) {
            Log.e("tla-instantiate", "tla-instantiate", e);
            newFragment = new TaskListFragment();
        } catch (IllegalAccessException e) {
            Log.e("tla-instantiate", "tla-instantiate", e);
            newFragment = new TaskListFragment();
        }
        Bundle args = new Bundle();
        args.putBundle(TOKEN_EXTRAS, extras);
        newFragment.setArguments(args);
        return newFragment;
    }

    /**
     * Convenience method for calling instantiateWithFilterAndExtras(Filter, Bundle, Class<?>) with
     * TaskListFragment as the default component
     * @param filter
     * @param extras
     * @return
     */
    public static TaskListFragment instantiateWithFilterAndExtras(Filter filter, Bundle extras) {
        return instantiateWithFilterAndExtras(filter, extras, TaskListFragment.class);
    }

    /**
     * Container Activity must implement this interface and we ensure that it
     * does during the onAttach() callback
     */
    public interface OnTaskListItemClickedListener {
        public void onTaskListItemClicked(long taskId);
    }

    @Override
    public void onAttach(SupportActivity activity) {
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
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        extras = getArguments() != null ? getArguments().getBundle(TOKEN_EXTRAS) : null;
        if (extras == null)
            extras = new Bundle(); // Just need an empty one to prevent potential null pointers
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(
                R.layout.task_list_activity, container, false);
        parent.addView(getListBody(parent), 0);

        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);
        syncActionHelper = new SyncActionHelper(getActivity(), this);
        setUpUiComponents();
        initializeData();
        setupQuickAddBar();

        Fragment filterlistFrame = getFragmentManager().findFragmentByTag(
                FilterListFragment.TAG_FILTERLIST_FRAGMENT);
        mDualFragments = (filterlistFrame != null)
                && filterlistFrame.isInLayout();

        if (mDualFragments) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setItemsCanFocus(false);
        }

        if (Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1) > -1)
            upgradeService.showChangeLog(getActivity(),
                    Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1));

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            }
        });

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TodorooCursor<Task> cursor = (TodorooCursor<Task>)taskAdapter.getItem(position);
                Task task = new Task(cursor);
                if(task.isDeleted())
                    return;

                if (!task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY)) {
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
            filter = CoreFilterExposer.buildInboxFilter(getResources());
        }
        isInbox = CoreFilterExposer.isInbox(filter);

        setUpTaskList();
        ((AstridActivity) getActivity()).setupActivityFragment(getActiveTagData());

        contextMenuExtensionLoader.loadInNewThread(getActivity());
    }

    protected void addSyncRefreshMenuItem(Menu menu, int themeFlags) {
        addMenuItem(menu, R.string.TLA_menu_sync,
                ThemeService.getDrawable(R.drawable.icn_menu_refresh, themeFlags), MENU_SYNC_ID, true);
    }

    protected void addMenuItem(Menu menu, int title, int imageRes, int id, boolean showAsAction) {
        AstridActivity activity = (AstridActivity) getActivity();
        if ((activity.getFragmentLayout() != AstridActivity.LAYOUT_SINGLE && showAsAction) || !(activity instanceof TaskListActivity)) {
            MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, title);
            item.setIcon(imageRes);
            if (activity instanceof TaskListActivity)
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            ((TaskListActivity) activity).getMainMenuPopover().addMenuItem(title, imageRes, id);
        }
    }

    protected void addMenuItem(Menu menu, CharSequence title, Drawable image, Intent customIntent, int id) {
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity) {
            ((TaskListActivity) activity).getMainMenuPopover().addMenuItem(title, image, customIntent, id);
        } else {
            MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, title);
            item.setIcon(image);
            item.setIntent(customIntent);
        }
    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Activity activity = getActivity();
        if (activity == null)
            return;
        if (!isCurrentTaskListFragment())
            return;

        boolean isTablet = AndroidUtilities.isTabletSized(activity);
        if (activity instanceof TaskListActivity)
            ((TaskListActivity) activity).getMainMenuPopover().clear();

        // --- sort
        if (allowResorting()) {
            addMenuItem(menu, R.string.TLA_menu_sort,
                    ThemeService.getDrawable(R.drawable.icn_menu_sort_by_size, isTablet ? ThemeService.FLAG_INVERT : 0), MENU_SORT_ID, true);
        }

        // --- sync
        addSyncRefreshMenuItem(menu, isTablet ? ThemeService.FLAG_INVERT : 0);

        // --- new filter
        addMenuItem(menu, R.string.FLA_new_filter,
                ThemeService.getDrawable(R.drawable.icn_menu_filters, isTablet ? ThemeService.FLAG_INVERT : 0), MENU_NEW_FILTER_ID, true);

        // --- addons
        if(Constants.MARKET_STRATEGY.showAddonMenu())
            addMenuItem(menu, R.string.TLA_menu_addons,
                ThemeService.getDrawable(R.drawable.icn_menu_plugins, isTablet ? ThemeService.FLAG_FORCE_DARK : 0), MENU_ADDONS_ID, false);

        // ask about plug-ins
        Intent queryIntent = new Intent(
                AstridApiConstants.ACTION_TASK_LIST_MENU);

        PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(
                queryIntent, 0);
        int length = resolveInfoList.size();
        for (int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            Intent intent = new Intent(AstridApiConstants.ACTION_TASK_LIST_MENU);
            intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            addMenuItem(menu, resolveInfo.loadLabel(pm), resolveInfo.loadIcon(pm), intent, MENU_ADDON_INTENT_ID);
        }
    }

    protected boolean allowResorting() {
        return true;
    }

    protected void setUpUiComponents() {
        // set listener for quick-changing task priority
        getListView().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP || view == null)
                    return false;

                boolean filterOn = getListView().isTextFilterEnabled();
                View selected = getListView().getSelectedView();

                // hot-key to set task priority - 1-4 or ALT + Q-R
                if (!filterOn && event.getUnicodeChar() >= '1'
                        && event.getUnicodeChar() <= '4' && selected != null) {
                    int importance = event.getNumber() - '1';
                    Task task = ((ViewHolder) selected.getTag()).task;
                    task.setValue(Task.IMPORTANCE, importance);
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

        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(getActivity());
        sortFlags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
        sortSort = publicPrefs.getInt(SortHelper.PREF_SORT_SORT, 0);
        sortFlags = SortHelper.setManualSort(sortFlags, isDraggable());

        getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    protected void setupQuickAddBar() {
        quickAddBar = (QuickAddBar) getView().findViewById(R.id.taskListFooter);
        quickAddBar.initialize(getActivity(), this, mListener);

        getListView().setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                quickAddBar.clearFocus();
                return false;
            }
        });

        // set listener for astrid icon
        ((TextView) getView().findViewById(android.R.id.empty)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                quickAddBar.performButtonClick();
            }
        });
    }

    // Subclasses can override these to customize extras in quickadd intent
    protected Intent getOnClickQuickAddIntent(Task t) {
        Intent intent = new Intent(getActivity(), TaskEditActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, t.getId());
        intent.putExtra(TOKEN_FILTER, filter);
        return intent;
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
                            loadTaskListContent(true);
                        } catch (IllegalStateException e) {
                            // view may have been destroyed
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
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DETAILS));
        getActivity().registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DECORATIONS));
        getActivity().registerReceiver(refreshReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        syncActionHelper.register();

        if (Flags.checkAndClear(Flags.REFRESH)) {
            refresh();
        }

        setUpBackgroundJobs();

        if (!Preferences.getBoolean(
                WelcomeWalkthrough.KEY_SHOWED_WELCOME_LOGIN, false)) {
            Preferences.setBoolean(WelcomeWalkthrough.KEY_SHOWED_WELCOME_LOGIN,
                    true);
            Intent showWelcomeLogin = new Intent(getActivity(),
                    WelcomeWalkthrough.class);
            showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
            startActivity(showWelcomeLogin);
            return;
        }

        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            showTaskCreateHelpPopover();
        } else if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            showTaskEditHelpPopover();
        } else if (!Preferences.getBoolean(R.string.p_showed_lists_help, false)) {
            showListsHelp();
        }
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
        if (activity == null)
            return;
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
        if (isCurrentTaskListFragment())
            syncActionHelper.initiateAutomaticSync(filter);
    }

    // Subclasses should override this
    public void requestCommentCountUpdate() {
        TaskListActivity activity = (TaskListActivity) getActivity();
        if (activity != null)
            activity.setCommentsCount(0);
    }

    @Override
    public void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(getActivity(), detailReceiver);
        AndroidUtilities.tryUnregisterReceiver(getActivity(), refreshReceiver);
        syncActionHelper.unregister();

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
            if (intent == null
                    || !AstridApiConstants.BROADCAST_EVENT_REFRESH.equals(intent.getAction()))
                return;

            Activity activity = getActivity();
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
        if (taskAdapter != null)
            taskAdapter.flushCaches();
        loadTaskListContent(true);
        taskService.cleanup();
    }

    /**
     * Receiver which receives detail or decoration intents
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class DetailReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Bundle receivedExtras = intent.getExtras();
                long taskId = receivedExtras.getLong(AstridApiConstants.EXTRAS_TASK_ID);
                String addOn = receivedExtras.getString(AstridApiConstants.EXTRAS_ADDON);

                if (AstridApiConstants.BROADCAST_SEND_DECORATIONS.equals(intent.getAction())) {
                    TaskDecoration deco = receivedExtras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                    taskAdapter.decorationManager.addNew(taskId, addOn, deco,
                            null);
                } else if (AstridApiConstants.BROADCAST_SEND_DETAILS.equals(intent.getAction())) {
                    String detail = receivedExtras.getString(AstridApiConstants.EXTRAS_RESPONSE);
                    taskAdapter.addDetails(taskId, detail);
                }
            } catch (Exception e) {
                exceptionService.reportError("receive-detail-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                        e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(quickAddBar.onActivityResult(requestCode, resultCode, data))
            return;

        if (requestCode == ACTIVITY_SETTINGS) {
            if (resultCode == EditPreferences.RESULT_CODE_THEME_CHANGED || resultCode == EditPreferences.RESULT_CODE_PERFORMANCE_PREF_CHANGED) {
                getActivity().finish();
                getActivity().startActivity(getActivity().getIntent());
                TasksWidget.updateWidgets(getActivity());
                return;
            } else if (resultCode == SyncProviderPreferences.RESULT_CODE_SYNCHRONIZE) {
                Preferences.setLong(SyncActionHelper.PREF_LAST_AUTO_SYNC, 0); // Forces autosync to occur after login
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            if(!AstridPreferences.canShowPopover())
                return;
            quickAddBar.getQuickAddBox().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        HelpInfoPopover.showPopover(getActivity(), quickAddBar.getQuickAddBox(),
                                R.string.help_popover_add_task, null);
                        Preferences.setBoolean(R.string.p_showed_add_task_help, true);
                    }
                }
            }, 1000);
        }
    }

    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        // do nothing
    }

    /**
     * Detect when user is flinging the task, disable task adapter loading when
     * this occurs to save resources and time.
     */
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
        case OnScrollListener.SCROLL_STATE_IDLE:
            if (taskAdapter.isFling)
                taskAdapter.notifyDataSetChanged();
            taskAdapter.isFling = false;
            break;
        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
            if (taskAdapter.isFling)
                taskAdapter.notifyDataSetChanged();
            taskAdapter.isFling = false;
            break;
        case OnScrollListener.SCROLL_STATE_FLING:
            taskAdapter.isFling = true;
            break;
        }
    }

    /*
     * ======================================================================
     * =================================================== managing list view
     * ======================================================================
     */

    /**
     * Load or re-load action items and update views
     *
     * @param requery
     */
    public void loadTaskListContent(boolean requery) {
        if (taskAdapter == null) {
            setUpTaskList();
            return;
        }

        int oldListItemSelected = getListView().getSelectedItemPosition();
        Cursor taskCursor = taskAdapter.getCursor();

        if (requery) {
            taskCursor.requery();
            taskAdapter.flushCaches();
            taskAdapter.notifyDataSetChanged();
        }

        if (oldListItemSelected != ListView.INVALID_POSITION
                && oldListItemSelected < taskCursor.getCount())
            getListView().setSelection(oldListItemSelected);

        // also load sync actions
        syncActionHelper.request();
    }

    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new TaskAdapter(this, R.layout.task_adapter_row,
                cursor, sqlQueryTemplate, false,
                new OnCompletedTaskListener() {
                    @Override
                    public void onCompletedTask(Task item, boolean newState) {
                        if (newState == true)
                            onTaskCompleted(item);
                    }
                });
    }

    /**
     * Fill in the Task List with current items
     *
     * @param withCustomId
     *            force task with given custom id to be part of list
     */
    protected void setUpTaskList() {
        if (filter == null)
            return;

        sqlQueryTemplate.set(SortHelper.adjustQueryForFlagsAndSort(
                filter.getSqlQuery(), sortFlags, sortSort));

        // perform query
        TodorooCursor<Task> currentCursor;
        try {
            currentCursor = taskService.fetchFiltered(
                sqlQueryTemplate.get(), null, taskProperties());
        } catch (SQLiteException e) {
            StartupService.handleSQLiteColumnMissing(getActivity(), e);
            return;
        }

        // set up list adapters
        taskAdapter = createTaskAdapter(currentCursor);

        setListAdapter(taskAdapter);
        getListView().setOnScrollListener(this);
        registerForContextMenu(getListView());

        loadTaskListContent(true);
    }

    protected Property<?>[] taskProperties() {
        return TaskAdapter.PROPERTIES;
    }

    public Filter getFilter() {
        return filter;
    }

    /**
     * Select a custom task id in the list. If it doesn't exist, create a new
     * custom filter
     *
     * @param withCustomId
     */
    @SuppressWarnings("nls")
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

        // create a custom cursor
        if (!sqlQueryTemplate.get().contains("WHERE"))
            sqlQueryTemplate.set(sqlQueryTemplate.get() + " WHERE "
                    + TaskCriteria.byId(withCustomId));
        else
            sqlQueryTemplate.set(sqlQueryTemplate.get().replace("WHERE ",
                    "WHERE " + TaskCriteria.byId(withCustomId) + " OR "));

        currentCursor = taskService.fetchFiltered(sqlQueryTemplate.get(), null,
                taskProperties());
        getListView().setFilterText("");

        taskAdapter.changeCursor(currentCursor);

        // update title
        if (getActivity() instanceof TaskListActivity)
            ((TaskListActivity) getActivity()).setListsTitle(getString(R.string.TLA_custom));

        // try selecting again
        for (int i = 0; i < currentCursor.getCount(); i++) {
            currentCursor.moveToPosition(i);
            if (currentCursor.get(Task.ID) == withCustomId) {
                getListView().setSelection(i);
                break;
            }
        }
    }

    private void showTaskCreateHelpPopover() {
        if(!AstridPreferences.canShowPopover())
            return;
        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            Preferences.setBoolean(R.string.p_showed_add_task_help, true);
            HelpInfoPopover.showPopover(getActivity(), quickAddBar.getQuickAddBox(),
                            R.string.help_popover_add_task, null);
        }
    }

    public void showTaskEditHelpPopover() {
        if(!AstridPreferences.canShowPopover())
            return;
        if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            quickAddBar.hideKeyboard();
            getListView().postDelayed(new Runnable() {
                public void run() {
                    try {
                        if (taskAdapter != null && taskAdapter.getCount() > 0) {
                            final View view = getListView().getChildAt(
                                    getListView().getChildCount() - 1);
                            if (view != null) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    HelpInfoPopover.showPopover(getActivity(), view,
                                            R.string.help_popover_tap_task, null);
                                    Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
                                }
                            }
                        }
                    } catch (IllegalStateException e) {
                        // Whoops, view is gone. Try again later
                    }
                }
            }, 1000L);

        }
    }

    private void showListsHelp() {
        if(!AstridPreferences.canShowPopover())
            return;
        if (!Preferences.getBoolean(
                R.string.p_showed_lists_help, false)) {
            AstridActivity activity = (AstridActivity) getActivity();
            if (activity != null) {
                if (AndroidUtilities.isTabletSized(activity)) {
                    FilterListFragment flf = activity.getFilterListFragment();
                    if (flf != null)
                        flf.showAddListPopover();
                } else {
                    ActionBar ab = activity.getSupportActionBar();
                    View anchor = ab.getCustomView().findViewById(R.id.lists_nav);
                    HelpInfoPopover.showPopover(activity,
                            anchor, R.string.help_popover_switch_lists, null);
                }
                Preferences.setBoolean(
                        R.string.p_showed_lists_help,
                        true);
            }
        }
    }

    /*
     * ======================================================================
     * ============================================================== actions
     * ======================================================================
     */

    /**
     * A task was completed from the task adapter
     *
     * @param item
     *            task that was completed
     */
    protected void onTaskCompleted(Task item) {
        if (isInbox)
            StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_INBOX);
        else
            StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_FILTER);
    }

    /**
     * Comments button in action bar was clicked
     */
    protected void handleCommentsButtonClicked() {
        Intent intent = new Intent(getActivity(), TagUpdatesActivity.class);
        intent.putExtra(TagViewFragment.EXTRA_TAG_DATA, getActiveTagData());
        startActivity(intent);
        AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
        Task task = ((ViewHolder) adapterInfo.targetView.getTag()).task;
        if (task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY))
            return;

        int id = (int) task.getId();
        menu.setHeaderTitle(task.getValue(Task.TITLE));

        if (task.isDeleted()) {
            menu.add(id, CONTEXT_MENU_UNDELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextUndeleteTask);

            menu.add(id, CONTEXT_MENU_PURGE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextPurgeTask);
        } else {
            menu.add(id, CONTEXT_MENU_EDIT_TASK_ID, Menu.NONE,
                    R.string.TAd_contextEditTask);
            menu.add(id, CONTEXT_MENU_COPY_TASK_ID, Menu.NONE,
                    R.string.TAd_contextCopyTask);

            for (int i = 0; i < contextItemExposers.length; i++) {
                Object label = contextItemExposers[i].getLabel(task);
                if (label != null) {
                    if (label instanceof Integer)
                        menu.add(id, CONTEXT_MENU_PLUGIN_ID_FIRST + i,
                                Menu.NONE, (Integer) label);
                    else
                        menu.add(id, CONTEXT_MENU_PLUGIN_ID_FIRST + i,
                                Menu.NONE, (String) label);
                }
            }

            long taskId = task.getId();
            for (ContextMenuItem item : contextMenuExtensionLoader.getList()) {
                android.view.MenuItem menuItem = menu.add(id,
                        CONTEXT_MENU_BROADCAST_INTENT_ID, Menu.NONE, item.title);
                item.intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
                menuItem.setIntent(item.intent);
            }

            menu.add(id, CONTEXT_MENU_DELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextDeleteTask);

        }
    }

    public boolean isInbox() {
        return isInbox;
    }

    /** Show a dialog box and delete the task specified */
    private void deleteTask(final Task task) {
        new AlertDialog.Builder(getActivity()).setTitle(
                R.string.DLG_confirm_title).setMessage(
                R.string.DLG_delete_this_task_question).setIcon(
                android.R.drawable.ic_dialog_alert).setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onTaskDelete(task);
                        taskService.delete(task);
                        loadTaskListContent(true);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    protected void onTaskDelete(@SuppressWarnings("unused") Task task) {
        // hook
        decrementFilterCount();
    }

    public void incrementFilterCount() {
        if (getActivity() instanceof TaskListActivity) {
            ((TaskListActivity) getActivity()).incrementFilterCount(filter);
        }
    }

    public void decrementFilterCount() {
        if (getActivity() instanceof TaskListActivity) {
            ((TaskListActivity) getActivity()).decrementFilterCount(filter);
        }
    }

    public void refreshFilterCount() {
        if (getActivity() instanceof TaskListActivity) {
            ((TaskListActivity) getActivity()).refreshFilterCount(filter);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mDualFragments)
            setSelection(position);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
        switch(id) {
        case MENU_ADDONS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_ADDONS);
            intent = new Intent(getActivity(), AddOnActivity.class);
            startActivityForResult(intent, ACTIVITY_ADDONS);
            return true;
        case MENU_SORT_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SORT);
            AlertDialog dialog = SortSelectionActivity.createDialog(
                    getActivity(), hasDraggableOption(), this, sortFlags, sortSort);
            dialog.show();
            return true;
        case MENU_SYNC_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SYNC);
            syncActionHelper.performSyncAction();
            return true;
        case MENU_ADDON_INTENT_ID:
            AndroidUtilities.startExternalIntent(getActivity(), intent,
                    ACTIVITY_MENU_EXTERNAL);
            return true;
        case MENU_NEW_FILTER_ID:
            intent = new Intent(getActivity(), CustomFilterActivity.class);
            getActivity().startActivityForResult(intent, ACTIVITY_REQUEST_NEW_FILTER);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Intent intent;
        long itemId;

        if (!isCurrentTaskListFragment())
            return false;

        // handle my own menus
        if (handleOptionsMenuItemSelected(item.getItemId(), item.getIntent()))
            return true;

        switch (item.getItemId()) {
        // --- context menu items

        case CONTEXT_MENU_BROADCAST_INTENT_ID: {
            intent = item.getIntent();
            getActivity().sendBroadcast(intent);
            return true;
        }
        case CONTEXT_MENU_EDIT_TASK_ID: {
            itemId = item.getGroupId();
            mListener.onTaskListItemClicked(itemId);
            return true;
        }
        case CONTEXT_MENU_COPY_TASK_ID: {
            itemId = item.getGroupId();
            duplicateTask(itemId);
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
            taskService.save(task);
            loadTaskListContent(true);
            return true;
        }
        case CONTEXT_MENU_PURGE_TASK_ID: {
            itemId = item.getGroupId();
            taskService.purge(itemId);
            loadTaskListContent(true);
            return true;
        }
        default: {
            if (item.getItemId() < CONTEXT_MENU_PLUGIN_ID_FIRST)
                return false;
            if (item.getItemId() - CONTEXT_MENU_PLUGIN_ID_FIRST >= contextItemExposers.length)
                return false;

            AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) item.getMenuInfo();
            Task task = ((ViewHolder) adapterInfo.targetView.getTag()).task;
            contextItemExposers[item.getItemId() - CONTEXT_MENU_PLUGIN_ID_FIRST].invoke(task);

            return true;
        }
        }
    }

    protected void duplicateTask(long itemId) {
        long cloneId = taskService.duplicateTask(itemId);

        Intent intent = new Intent(getActivity(), TaskEditActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, cloneId);
        intent.putExtra(TOKEN_FILTER, filter);
        getActivity().startActivityForResult(intent, ACTIVITY_EDIT_TASK);
        transitionForTaskEdit();
    }

    public void showSettings() {
        Activity activity = getActivity();
        if (activity == null)
            return;

        StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SETTINGS);
        Intent intent = new Intent(activity, EditPreferences.class);
        startActivityForResult(intent, ACTIVITY_SETTINGS);
    }

    public void onTaskListItemClicked(long taskId) {
        mListener.onTaskListItemClicked(taskId);
    }

    protected void toggleDragDrop(boolean newState) {
        extras.putParcelable(TOKEN_FILTER, filter);
        if(newState)
            ((AstridActivity)getActivity()).setupTasklistFragmentWithFilterAndCustomTaskList(filter,
                    extras, SubtasksListFragment.class);
        else {
            filter.setFilterQueryOverride(null);
            ((AstridActivity)getActivity()).setupTasklistFragmentWithFilterAndCustomTaskList(filter,
                    extras, TaskListFragment.class);
        }
    }

    protected boolean isDraggable() {
        return false;
    }

    protected boolean hasDraggableOption() {
        return isInbox;
    }

    @Override
    public void onSortSelected(boolean always, int flags, int sort) {
        boolean manualSettingChanged = SortHelper.isManualSort(sortFlags) !=
            SortHelper.isManualSort(flags);

        sortFlags = flags;
        sortSort = sort;

        if (always) {
            SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(ContextManager.getContext());
            Editor editor = publicPrefs.edit();
            editor.putInt(SortHelper.PREF_SORT_FLAGS, flags);
            editor.putInt(SortHelper.PREF_SORT_SORT, sort);
            editor.commit();
            TasksWidget.updateWidgets(ContextManager.getContext());
        }

        try {
            if(manualSettingChanged)
                toggleDragDrop(SortHelper.isManualSort(sortFlags));
            else
                setUpTaskList();
        } catch (IllegalStateException e) {
            // TODO: Fragment got detached somehow (rare)
        }
    }
}
