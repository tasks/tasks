package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.weloveastrid.rmilk.MilkPreferences;
import org.weloveastrid.rmilk.MilkUtilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent.CanceledException;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.crittercism.FeedbackActivity;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.widget.GestureService;
import com.todoroo.andlib.widget.GestureService.GestureInterface;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.activity.SortSelectionActivity.OnSortSelectedListener;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.api.SyncAction;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.helper.TaskListContextMenuExtensionLoader;
import com.todoroo.astrid.helper.TaskListContextMenuExtensionLoader.ContextMenuItem;
import com.todoroo.astrid.reminders.ReminderDebugContextActions;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.welcome.HelpInfoPopover;
import com.todoroo.astrid.welcome.WelcomeLogin;
import com.todoroo.astrid.welcome.tutorial.WelcomeWalkthrough;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Primary activity for the Bente application. Shows a list of upcoming
 * tasks and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListActivity extends ListFragment implements OnScrollListener,
        GestureInterface, OnSortSelectedListener {

    public static final String TAG_TASKLIST_FRAGMENT = "tasklist_fragment"; //$NON-NLS-1$

    // --- activities

    private static final long BACKGROUND_REFRESH_INTERVAL = 120000L;
    public static final int ACTIVITY_EDIT_TASK = 0;
    public static final int ACTIVITY_SETTINGS = 1;
    public static final int ACTIVITY_SORT = 2;
    public static final int ACTIVITY_ADDONS = 3;
    public static final int ACTIVITY_MENU_EXTERNAL = 4;

    // --- menu codes

    protected static final int MENU_ADDONS_ID = R.string.TLA_menu_addons;
    protected static final int MENU_SETTINGS_ID = R.string.TLA_menu_settings;
    protected static final int MENU_SORT_ID = R.string.TLA_menu_sort;
    protected static final int MENU_SYNC_ID = R.string.TLA_menu_sync;
    protected static final int MENU_SUPPORT_ID = R.string.TLA_menu_support;
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

    /** token for indicating source of TLA launch */
    public static final String TOKEN_SOURCE = "source"; //$NON-NLS-1$

    public static final String TOKEN_OVERRIDE_ANIM = "finishAnim"; //$NON-NLS-1$

    // --- instance variables

    @Autowired ExceptionService exceptionService;

    @Autowired protected TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired Database database;

    @Autowired AddOnService addOnService;

    @Autowired UpgradeService upgradeService;

    @Autowired protected SyncV2Service syncService;

    @Autowired TagDataService tagDataService;

    private final TaskContextActionExposer[] contextItemExposers = new TaskContextActionExposer[] {
            new ReminderDebugContextActions.MakeNotification(),
            new ReminderDebugContextActions.WhenReminder(),
    };

    protected TaskAdapter taskAdapter = null;
    protected DetailReceiver detailReceiver = new DetailReceiver();
    protected RefreshReceiver refreshReceiver = new RefreshReceiver();
    protected SyncActionReceiver syncActionReceiver = new SyncActionReceiver();
    protected final AtomicReference<String> sqlQueryTemplate = new AtomicReference<String>();
    protected Filter filter;
    protected int sortFlags;
    protected int sortSort;
    protected boolean overrideFinishAnim;

    private ImageButton voiceAddButton;
    private ImageButton quickAddButton;
    private EditText quickAddBox;
    private Timer backgroundTimer;
    private final LinkedHashSet<SyncAction> syncActions = new LinkedHashSet<SyncAction>();
    private boolean isFilter;

    private final TaskListContextMenuExtensionLoader contextMenuExtensionLoader = new TaskListContextMenuExtensionLoader();
    private VoiceInputAssistant voiceInputAssistant;

    // --- fragment handling variables
    OnTaskListItemClickedListener mListener;
    private boolean mDualFragments = false;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    static {
        AstridDependencyInjector.initialize();
    }

    /** Container Activity must implement this interface and we ensure
     * that it does during the onAttach() callback
     */
    public interface OnTaskListItemClickedListener {
        public void onTaskListItemClicked(long taskId);
    }

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback interface
        try {
            mListener = (OnTaskListItemClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTaskListItemClickedListener"); //$NON-NLS-1$
        }
    }

    /**
     * @return view to attach to the body of the task list. must contain two
     * elements, a view with id android:id/empty and a list view with id
     * android:id/list. It should NOT be attached to root
     */
    protected View getListBody(ViewGroup root) {
        if(AndroidUtilities.getSdkVersion() > 3)
            return getActivity().getLayoutInflater().inflate(R.layout.task_list_body_standard, root, false);
        else
            return getActivity().getLayoutInflater().inflate(R.layout.task_list_body_api3, root, false);
    }

    /**  Called when loading up the activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ContextManager.setContext(getActivity());
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        // Tell the framework to try to keep this fragment around
        // during a configuration change.
//        setRetainInstance(true);

        new StartupService().onStartupApplication(getActivity());

        if(database == null)
            return;

        database.openForWriting();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
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

        setUpUiComponents();
        onNewIntent(getActivity().getIntent());

        Fragment filterlistFrame = getFragmentManager().findFragmentByTag(FilterListActivity.TAG_FILTERLIST_FRAGMENT);
        mDualFragments = (filterlistFrame != null) && filterlistFrame.isInLayout();

        if (mDualFragments) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setItemsCanFocus(false);
        }

        if(Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1) > -1)
            upgradeService.showChangeLog(getActivity(), Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1));

        if(getActivity().getIntent().hasExtra(TOKEN_SOURCE)) {
            switch(getActivity().getIntent().getIntExtra(TOKEN_SOURCE, Constants.SOURCE_DEFAULT)) {
            case Constants.SOURCE_NOTIFICATION:
                StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_NOTIFICATION);
                break;
            case Constants.SOURCE_OTHER:
                StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_OTHER);
                break;
            case Constants.SOURCE_PPWIDGET:
                StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_PPW);
                break;
            case Constants.SOURCE_WIDGET:
                StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_WIDGET);
                break;
            case Constants.SOURCE_C2DM:
                StatisticsService.reportEvent(StatisticsConstants.LAUNCH_FROM_C2DM);
                break;
            }
        }

        getActivity().runOnUiThread(new Runnable() {
           public void run() {
               Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
           }
        });
    }

    protected void onNewIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            overrideFinishAnim = extras.getBoolean(TOKEN_OVERRIDE_ANIM);
        } else {
            overrideFinishAnim = false;
        }
        String intentAction = intent.getAction();
        // FIXME maybe SEARCH has to go into the Wrapper-activity and forward to the filterFragment
        if (Intent.ACTION_SEARCH.equals(intentAction)) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            Filter searchFilter = new Filter(null, getString(R.string.FLA_search_filter, query),
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like("%" + //$NON-NLS-1$
                            query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
            intent = new Intent(getActivity(), TaskListWrapperActivity.class);
            intent.putExtra(TaskListActivity.TOKEN_FILTER, searchFilter);
            startActivity(intent);
            getActivity().finish();
            if (overrideFinishAnim) {
                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_right_in, R.anim.slide_right_out);
            }
            return;
        } else if(extras != null && extras.containsKey(TOKEN_FILTER)) {
            filter = extras.getParcelable(TOKEN_FILTER);
            isFilter = true;
        } else {
            filter = CoreFilterExposer.buildInboxFilter(getResources());
            isFilter = false;
        }

        setUpTaskList();
        // FIXME put this into the wrapper activity
        if(Constants.DEBUG)
            getActivity().setTitle("[D] " + filter.title); //$NON-NLS-1$

        contextMenuExtensionLoader.loadInNewThread(getActivity());
    }

    protected void addSyncRefreshMenuItem(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, MENU_SYNC_ID, Menu.NONE,
                R.string.TLA_menu_sync);
        item.setIcon(R.drawable.ic_menu_refresh);
        if (((AstridWrapperActivity) getActivity()).isMultipleFragments())
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() == null)
            return;

        MenuItem item;

        if (!(this instanceof DraggableTaskListActivity)) {
            item = menu.add(Menu.NONE, MENU_SORT_ID, Menu.NONE, R.string.TLA_menu_sort);
            item.setIcon(android.R.drawable.ic_menu_sort_by_size);
            if (((AstridWrapperActivity) getActivity()).isMultipleFragments())
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        addSyncRefreshMenuItem(menu);

        if(!Constants.MARKET_DISABLED) {
            item = menu.add(Menu.NONE, MENU_ADDONS_ID, Menu.NONE,
                    R.string.TLA_menu_addons);
            item.setIcon(android.R.drawable.ic_menu_set_as);
        }

        item = menu.add(Menu.NONE, MENU_SUPPORT_ID, Menu.NONE,
                R.string.TLA_menu_support);
        item.setIcon(android.R.drawable.ic_menu_help);

        item = menu.add(Menu.NONE, MENU_SETTINGS_ID, Menu.NONE,
                R.string.TLA_menu_settings);
        item.setIcon(android.R.drawable.ic_menu_preferences);

        // ask about plug-ins
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_TASK_LIST_MENU);

        PackageManager pm = getActivity().getPackageManager();
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
    }

    protected void setUpUiComponents() {
//        ((ImageView)getView().findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                Preferences.setBoolean(R.string.p_showed_lists_help, true);
//                showFilterListActivity();
//            }
//        });

//        getView().findViewById(R.id.sort_settings).setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SORT);
//                AlertDialog dialog = SortSelectionActivity.createDialog(getActivity(),
//                        TaskListActivity.this, sortFlags, sortSort);
//                dialog.show();
//            }
//        });

        // set listener for quick-changing task priority
        getListView().setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if(event.getAction() != KeyEvent.ACTION_UP || view == null)
                    return false;

                boolean filterOn = getListView().isTextFilterEnabled();
                View selected = getListView().getSelectedView();

                // hot-key to set task priority - 1-4 or ALT + Q-R
                if(!filterOn && event.getUnicodeChar() >= '1' && event.getUnicodeChar() <= '4' && selected != null) {
                    int importance = event.getNumber() - '1';
                    Task task = ((ViewHolder)selected.getTag()).task;
                    task.setValue(Task.IMPORTANCE, importance);
                    taskService.save(task);
                    taskAdapter.setFieldContentsAndVisibility(selected);
                }
                // filter
                else if(!filterOn && event.getUnicodeChar() != 0) {
                    getListView().setTextFilterEnabled(true);
                    getListView().setFilterText(Character.toString((char)event.getUnicodeChar()));
                }
                // turn off filter if nothing is selected
                else if(filterOn && TextUtils.isEmpty(getListView().getTextFilter())) {
                    getListView().setTextFilterEnabled(false);
                }

                return false;
            }
        });

        // set listener for pressing enter in quick-add box
        quickAddBox = (EditText) getView().findViewById(R.id.quickAddText);
        quickAddBox.setOnEditorActionListener(new OnEditorActionListener() {
            /**
             * When user presses enter, quick-add the task
             */
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL && !TextUtils.isEmpty(quickAddBox.getText().toString().trim())) {
                    quickAddTask(quickAddBox.getText().toString(), true);
                    return true;
                }
                return false;
            }
        });


        quickAddButton = ((ImageButton)getView().findViewById(R.id.quickAddButton));

        // set listener for quick add button
        quickAddButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(), true);
                if(task != null && task.getValue(Task.TITLE).length() == 0) {
                    Intent intent = getOnClickQuickAddIntent(task);
                    startActivityForResult(intent, ACTIVITY_EDIT_TASK);
                    transitionForTaskEdit();
                }
            }
        });

        // prepare and set listener for voice add button
        voiceAddButton = (ImageButton) getView().findViewById(R.id.voiceAddButton);
        int prompt = R.string.voice_edit_title_prompt;
        if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
            prompt = R.string.voice_create_prompt;
        voiceInputAssistant = new VoiceInputAssistant(getActivity(),voiceAddButton,quickAddBox);
        voiceInputAssistant.configureMicrophoneButton(prompt);

        // set listener for extended addbutton
        quickAddButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Task task = quickAddTask(quickAddBox.getText().toString(), false);
                if(task == null)
                    return true;
                Intent intent = getOnLongClickQuickAddIntent(task);
                startActivityForResult(intent, ACTIVITY_EDIT_TASK);
                transitionForTaskEdit();
                return true;
            }
        });

        //set listener for astrid icon
        ((TextView)getView().findViewById(android.R.id.empty)).setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                quickAddButton.performClick();
            }
         });


        // gestures / animation
        try {
            GestureService.registerGestureDetector(getActivity(), R.id.gestures, R.raw.gestures, this);
        } catch (VerifyError e) {
            // failed check, no gestures :P
        }

        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(getActivity());
        sortFlags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
        sortSort = publicPrefs.getInt(SortHelper.PREF_SORT_SORT, 0);
    }

    // Subclasses can override these to customize extras in quickadd intent
    protected Intent getOnClickQuickAddIntent(Task t) {
        Intent intent = new Intent(getActivity(), TaskEditWrapperActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, t.getId());
        intent.putExtra(TOKEN_FILTER, filter);
        return intent;
    }

    protected Intent getOnLongClickQuickAddIntent(Task t) {
        Intent intent = new Intent(getActivity(), TaskEditWrapperActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, t.getId());
        intent.putExtra(TOKEN_FILTER, filter);
        return intent;
    }

    public void transitionForTaskEdit() {
        AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
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
                        loadTaskListContent(true);
                    }
                });
            }
        }, BACKGROUND_REFRESH_INTERVAL, BACKGROUND_REFRESH_INTERVAL);
    }

    /* ======================================================================
     * ============================================================ lifecycle
     * ====================================================================== */

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        StatisticsService.sessionStop(getActivity());
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        StatisticsService.sessionStart(getActivity());
        if (addOnService.hasPowerPack() &&
                Preferences.getBoolean(R.string.p_voiceInputEnabled, true) &&
                voiceInputAssistant.isVoiceInputAvailable()) {
            voiceAddButton.setVisibility(View.VISIBLE);
        } else {
            voiceAddButton.setVisibility(View.GONE);
        }

        getActivity().registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DETAILS));
        getActivity().registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DECORATIONS));
        getActivity().registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_ACTIONS));
        getActivity().registerReceiver(refreshReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        getActivity().registerReceiver(syncActionReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS));
        setUpBackgroundJobs();

        if (!Preferences.getBoolean(WelcomeLogin.KEY_SHOWED_WELCOME_LOGIN, false)) {
            Preferences.setBoolean(WelcomeLogin.KEY_SHOWED_WELCOME_LOGIN, true);
            Intent showWelcomeLogin = new Intent(getActivity(), WelcomeWalkthrough.class);
            showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
            startActivity(showWelcomeLogin);
            return;
        }

        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            HelpInfoPopover.showPopover(getActivity(), quickAddBox, R.string.help_popover_add_task, null);
            Preferences.setBoolean(R.string.p_showed_add_task_help, true);
        } else if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            showTaskEditHelpPopover();
        } else if (Preferences.isSet(getString(R.string.p_showed_lists_help)) &&
                !Preferences.getBoolean(R.string.p_showed_lists_help, false)) {
            //HelpInfoPopover.showPopover(getActivity(), getView().findViewById(R.id.back), R.string.help_popover_lists, null);
            Preferences.setBoolean(R.string.p_showed_lists_help, true);
        }

        initiateAutomaticSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
        try {
        	getActivity().unregisterReceiver(detailReceiver);
        	getActivity().unregisterReceiver(refreshReceiver);
        	getActivity().unregisterReceiver(syncActionReceiver);
        } catch (IllegalArgumentException e) {
            // might not have fully initialized
        }
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
            if(intent == null || !AstridApiConstants.BROADCAST_EVENT_REFRESH.equals(intent.getAction()))
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    taskAdapter.flushCaches();
                    loadTaskListContent(true);
                }
            });
        }
    }

    /**
     * Receiver which receives sync provider intents
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class SyncActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null || !AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS.equals(intent.getAction()))
                return;

            try {
                Bundle extras = intent.getExtras();
                SyncAction syncAction = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                syncActions.add(syncAction);
            } catch (Exception e) {
                exceptionService.reportError("receive-sync-action-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON), e);
            }
        }
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
                Bundle extras = intent.getExtras();
                long taskId = extras.getLong(AstridApiConstants.EXTRAS_TASK_ID);
                String addOn = extras.getString(AstridApiConstants.EXTRAS_ADDON);

                if(AstridApiConstants.BROADCAST_SEND_DECORATIONS.equals(intent.getAction())) {
                    TaskDecoration deco = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                    taskAdapter.decorationManager.addNew(taskId, addOn, deco, null);
                } else if(AstridApiConstants.BROADCAST_SEND_DETAILS.equals(intent.getAction())) {
                    String detail = extras.getString(AstridApiConstants.EXTRAS_RESPONSE);
                    taskAdapter.addDetails(taskId, detail);
                } else if(AstridApiConstants.BROADCAST_SEND_ACTIONS.equals(intent.getAction())) {
                    TaskAction action = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                    taskAdapter.taskActionManager.addNew(taskId, addOn, action, null);
                }
            } catch (Exception e) {
                exceptionService.reportError("receive-detail-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON), e);
            }
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        // FIXME: move to parent Activity
//      super.onWindowFocusChanged(hasFocus);
        if(hasFocus && Flags.checkAndClear(Flags.REFRESH)) {
            taskAdapter.flushCaches();
            loadTaskListContent(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle the result of voice recognition, put it into the textfield
        if (voiceInputAssistant.handleActivityResult(requestCode, resultCode, data)) {
            // if user wants, create the task directly (with defaultvalues) after saying it
            if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
                quickAddTask(quickAddBox.getText().toString(), true);
            super.onActivityResult(requestCode, resultCode, data);

            // the rest of onActivityResult is totally unrelated to voicerecognition, so bail out
            return;
        }

        if(requestCode == ACTIVITY_SETTINGS && resultCode == EditPreferences.RESULT_CODE_THEME_CHANGED) {
            getActivity().finish();
            if (overrideFinishAnim) {
                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_right_in, R.anim.slide_right_out);
            }
            getActivity().startActivity(getActivity().getIntent());
        }

        super.onActivityResult(requestCode, resultCode, data);


        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            HelpInfoPopover.showPopover(getActivity(), quickAddBox, R.string.help_popover_add_task, null);
            Preferences.setBoolean(R.string.p_showed_add_task_help, true);
        }

        if(resultCode != Activity.RESULT_CANCELED) {
            if (data != null && data.hasExtra(TaskEditActivity.TOKEN_TASK_WAS_ASSIGNED) && data.getBooleanExtra(TaskEditActivity.TOKEN_TASK_WAS_ASSIGNED, false) && !isFilter) {
                String assignedTo = data.getStringExtra(TaskEditActivity.TOKEN_ASSIGNED_TO);
                switchToAssignedFilter(assignedTo);
            } else {
                taskAdapter.flushCaches();
                loadTaskListContent(true);
                taskService.cleanup();
            }
        }
    }

    private void switchToAssignedFilter(final String assignedEmail) {
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Filter assignedFilter = CustomFilterExposer.getAssignedByMeFilter(getResources());

                Intent intent = new Intent(getActivity(), TaskListWrapperActivity.class);
                intent.putExtra(TaskListActivity.TOKEN_FILTER, assignedFilter);
                intent.putExtra(TaskListActivity.TOKEN_OVERRIDE_ANIM, true);
                startActivityForResult(intent, 0);
                transitionForTaskEdit();
            }
        };
        DialogUtilities.okCancelCustomDialog(getActivity(),
                getString(R.string.actfm_view_task_title), getString(R.string.actfm_view_task_text, assignedEmail),
                R.string.actfm_view_task_ok, R.string.actfm_view_task_cancel, 0,
                okListener, null);
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
        if(taskAdapter == null) {
            setUpTaskList();
            return;
        }

        int oldListItemSelected = getListView().getSelectedItemPosition();
        Cursor taskCursor = taskAdapter.getCursor();

        if(requery) {
            taskCursor.requery();
            taskAdapter.flushCaches();
            taskAdapter.notifyDataSetChanged();
        }
        getActivity().startManagingCursor(taskCursor);

        if(oldListItemSelected != ListView.INVALID_POSITION &&
                oldListItemSelected < taskCursor.getCount())
            getListView().setSelection(oldListItemSelected);

        // also load sync actions
        syncActions.clear();
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_SYNC_ACTIONS);
        getActivity().sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Fill in the Task List with current items
     * @param withCustomId force task with given custom id to be part of list
     */
    protected void setUpTaskList() {
        if(filter == null)
            return;

        sqlQueryTemplate.set(SortHelper.adjustQueryForFlagsAndSort(filter.sqlQuery,
                sortFlags, sortSort));
        //getActivity().setTitle(filter.title);

        // perform query
        TodorooCursor<Task> currentCursor = taskService.fetchFiltered(
                sqlQueryTemplate.get(), null, TaskAdapter.PROPERTIES);
        getActivity().startManagingCursor(currentCursor);

        // set up list adapters
        taskAdapter = new TaskAdapter(this, R.layout.task_adapter_row,
                currentCursor, sqlQueryTemplate, false, new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                if(newState == true)
                    onTaskCompleted(item);
            }
        });
        setListAdapter(taskAdapter);
        getListView().setOnScrollListener(this);
        registerForContextMenu(getListView());

        loadTaskListContent(true);
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
                showTaskEditHelpPopover();
                return;
            }
        }

        // create a custom cursor
        if(!sqlQueryTemplate.get().contains("WHERE"))
            sqlQueryTemplate.set(sqlQueryTemplate.get() + " WHERE " + TaskCriteria.byId(withCustomId));
        else
            sqlQueryTemplate.set(sqlQueryTemplate.get().replace("WHERE ", "WHERE " +
                    TaskCriteria.byId(withCustomId) + " OR "));

        currentCursor = taskService.fetchFiltered(sqlQueryTemplate.get(), null, TaskAdapter.PROPERTIES);
        getListView().setFilterText("");
        getActivity().startManagingCursor(currentCursor);

        taskAdapter.changeCursor(currentCursor);

        // update title
        filter.title = getString(R.string.TLA_custom);
        ((TextView)getView().findViewById(R.id.listLabel)).setText(filter.title);

        // try selecting again
        for(int i = 0; i < currentCursor.getCount(); i++) {
            currentCursor.moveToPosition(i);
            if(currentCursor.get(Task.ID) == withCustomId) {
                getListView().setSelection(i);
                showTaskEditHelpPopover();
                break;
            }
        }
    }

    private void showTaskEditHelpPopover() {
        if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(quickAddBox.getWindowToken(), 0);
            getListView().postDelayed(new Runnable() {
                public void run() {
                    if (taskAdapter.getCount() > 0) {
                        final View view = getListView().getChildAt(getListView().getChildCount() - 1);
                        if (view != null) {
                            OnDismissListener onDismiss = new OnDismissListener() {
                                @Override
                                public void onDismiss() {
                                    if (!Preferences.isSet(getString(R.string.p_showed_lists_help))) {
                                        Preferences.setBoolean(R.string.p_showed_lists_help, false);
                                    } else if (!Preferences.getBoolean(R.string.p_showed_lists_help, false)) {
                                        Preferences.setBoolean(R.string.p_showed_lists_help, true);
                                        //HelpInfoPopover.showPopover(getActivity(), getView().findViewById(R.id.back), R.string.help_popover_lists, null);
                                    }
                                }
                            };
                            HelpInfoPopover.showPopover(getActivity(), view, R.string.help_popover_tap_task, onDismiss);
                        }
                    }
                }
            }, 1000L);

        }
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    /**
     * A task was completed from the task adapter
     * @param item task that was completed
     */
    protected void onTaskCompleted(Task item) {
        if(isFilter)
            StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_INBOX);
        else
            StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_FILTER);
    }

    /**
     * Quick-add a new task
     * @param title
     * @return
     */
    @SuppressWarnings("nls")
    protected Task quickAddTask(String title, boolean selectNewTask) {
        try {
            if(title != null)
                title = title.trim();
            Task task = createWithValues(filter.valuesForNewTasks,
                    title, taskService, metadataService);

            boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null &&
                                             !Preferences.getStringValue(R.string.gcal_p_default).equals("-1");
            if (title.length()>0 && gcalCreateEventEnabled) {
                Uri calendarUri = GCalHelper.createTaskEvent(task, getActivity().getContentResolver(), new ContentValues());
                task.setValue(Task.CALENDAR_URI, calendarUri.toString());
                taskService.save(task);
            }

            TextView quickAdd = (TextView)getView().findViewById(R.id.quickAddText);
            quickAdd.setText(""); //$NON-NLS-1$

            if(selectNewTask) {
                loadTaskListContent(true);
                selectCustomId(task.getId());
            }

            StatisticsService.reportEvent(StatisticsConstants.TASK_CREATED_TASKLIST);
            return task;
        } catch (Exception e) {
            exceptionService.displayAndReportError(getActivity(), "quick-add-task", e);
            return new Task();
        }
    }

    /**
     * Create task from the given content values, saving it.
     * @param values
     * @param title
     * @param taskService
     * @param metadataService
     * @return
     */
    public static Task createWithValues(ContentValues values, String title, TaskService taskService,
            MetadataService metadataService) {
        Task task = new Task();
        if(title != null)
            task.setValue(Task.TITLE, title);

        ContentValues forMetadata = null;
        if(values != null && values.size() > 0) {
            ContentValues forTask = new ContentValues();
            forMetadata = new ContentValues();
            outer: for(Entry<String, Object> item : values.valueSet()) {
                String key = item.getKey();
                Object value = item.getValue();
                if(value instanceof String)
                    value = PermaSql.replacePlaceholders((String)value);

                for(Property<?> property : Metadata.PROPERTIES)
                    if(property.name.equals(key)) {
                        AndroidUtilities.putInto(forMetadata, key, value);
                        continue outer;
                    }

                AndroidUtilities.putInto(forTask, key, value);
            }
            task.mergeWith(forTask);
        }
        taskService.quickAdd(task);
        if(forMetadata != null && forMetadata.size() > 0) {
            Metadata metadata = new Metadata();
            metadata.setValue(Metadata.TASK, task.getId());
            metadata.mergeWith(forMetadata);
            metadataService.save(metadata);
        }
        return task;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo)menuInfo;
        Task task = ((ViewHolder)adapterInfo.targetView.getTag()).task;
        if (task.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY))
            return;

        int id = (int)task.getId();
        menu.setHeaderTitle(task.getValue(Task.TITLE));

        if(task.isDeleted()) {
            menu.add(id, CONTEXT_MENU_UNDELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextUndeleteTask);

            menu.add(id, CONTEXT_MENU_PURGE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextPurgeTask);
        } else {
            menu.add(id, CONTEXT_MENU_EDIT_TASK_ID, Menu.NONE,
                        R.string.TAd_contextEditTask);
            menu.add(id, CONTEXT_MENU_COPY_TASK_ID, Menu.NONE,
                    R.string.TAd_contextCopyTask);

            for(int i = 0; i < contextItemExposers.length; i++) {
                Object label = contextItemExposers[i].getLabel(task);
                if(label != null) {
                    if(label instanceof Integer)
                        menu.add(id, CONTEXT_MENU_PLUGIN_ID_FIRST + i, Menu.NONE, (Integer)label);
                    else
                        menu.add(id, CONTEXT_MENU_PLUGIN_ID_FIRST + i, Menu.NONE, (String)label);
                }
            }

            long taskId = task.getId();
            for(ContextMenuItem item : contextMenuExtensionLoader.getList()) {
                android.view.MenuItem menuItem = menu.add(id, CONTEXT_MENU_BROADCAST_INTENT_ID, Menu.NONE,
                        item.title);
                item.intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
                menuItem.setIntent(item.intent);
            }

            menu.add(id, CONTEXT_MENU_DELETE_TASK_ID, Menu.NONE,
                    R.string.TAd_contextDeleteTask);

        }
    }

    /** Show a dialog box and delete the task specified */
    private void deleteTask(final Task task) {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.DLG_confirm_title)
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

    /**
     * Intent object with custom label returned by toString.
     * @author joshuagross <joshua.gross@gmail.com>
     */
    protected static class IntentWithLabel extends Intent {
        private final String label;
        public IntentWithLabel (Intent in, String labelIn) {
            super(in);
            label = labelIn;
        }
        @Override
        public String toString () {
            return label;
        }
    }

    private static final String PREF_LAST_AUTO_SYNC = "taskListLastAutoSync"; //$NON-NLS-1$

    protected void initiateAutomaticSync() {
        if (filter.title == null || !filter.title.equals(getString(R.string.BFE_Active)))
            return;

        long lastAutoSync = Preferences.getLong(PREF_LAST_AUTO_SYNC, 0);
        if(DateUtilities.now() - lastAutoSync > DateUtilities.ONE_HOUR) {
            performSyncServiceV2Sync(false);
        }
    }

    protected void performSyncServiceV2Sync(boolean manual) {
        syncService.synchronizeActiveTasks(manual, new ProgressBarSyncResultCallback(getActivity(),
                R.id.progressBar, new Runnable() {
            @Override
            public void run() {
                loadTaskListContent(true);
            }
        }));
        Preferences.setLong(PREF_LAST_AUTO_SYNC, DateUtilities.now());
    }

    protected void performSyncAction() {
        if (syncActions.size() == 0 && !syncService.isActive()) {
            String desiredCategory = getString(R.string.SyP_label);

            // Get a list of all sync plugins and bring user to the prefs pane
            // for one of them
            Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            PackageManager pm = getActivity().getPackageManager();
            List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(
                    queryIntent, PackageManager.GET_META_DATA);
            int length = resolveInfoList.size();
            ArrayList<Intent> syncIntents = new ArrayList<Intent>();

            // Loop through a list of all packages (including plugins, addons)
            // that have a settings action: filter to sync actions
            for (int i = 0; i < length; i++) {
                ResolveInfo resolveInfo = resolveInfoList.get(i);
                Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
                intent.setClassName(resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);

                String category = MetadataHelper.resolveActivityCategoryName(resolveInfo, pm);
                if(MilkPreferences.class.getName().equals(resolveInfo.activityInfo.name) &&
                        !MilkUtilities.INSTANCE.isLoggedIn())
                    continue;

                if (category.equals(desiredCategory)) {
                    syncIntents.add(new IntentWithLabel(intent,
                            resolveInfo.activityInfo.loadLabel(pm).toString()));
                }
            }

            final Intent[] actions = syncIntents.toArray(new Intent[syncIntents.size()]);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface click, int which) {
                    startActivity(actions[which]);
                }
            };

            showSyncOptionMenu(actions, listener);
        }
        else {
            performSyncServiceV2Sync(true);

            if(syncActions.size() > 0) {
                for(SyncAction syncAction : syncActions) {
                    try {
                        syncAction.intent.send();
                    } catch (CanceledException e) {
                        //
                    }
                }
                Toast.makeText(getActivity(), R.string.SyP_progress_toast,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Show menu of sync options. This is shown when you're not logged into any services, or logged into
     * more than one.
     * @param <TYPE>
     * @param items
     * @param listener
     */
    private <TYPE> void showSyncOptionMenu(TYPE[] items, DialogInterface.OnClickListener listener) {
        ArrayAdapter<TYPE> adapter = new ArrayAdapter<TYPE>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items);

        // show a menu of available options
        new AlertDialog.Builder(getActivity())
        .setTitle(R.string.SyP_label)
        .setAdapter(adapter, listener)
        .show().setOwnerActivity(getActivity());
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mDualFragments)
            setSelection(position);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // called when context menu appears
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Intent intent;
        long itemId;

        // handle my own menus
        switch (item.getItemId()) {
        case MENU_ADDONS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_ADDONS);
            intent = new Intent(getActivity(), AddOnActivity.class);
            startActivityForResult(intent, ACTIVITY_ADDONS);
            return true;
        case MENU_SETTINGS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SETTINGS);
            intent = new Intent(getActivity(), EditPreferences.class);
            startActivityForResult(intent, ACTIVITY_SETTINGS);
            return true;
        case MENU_SORT_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SORT);
            AlertDialog dialog = SortSelectionActivity.createDialog(getActivity(),
                    this, sortFlags, sortSort);
            dialog.show();
            return true;
        case MENU_SYNC_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SYNC);
            performSyncAction();
            return true;
        case MENU_SUPPORT_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_HELP);
            intent = new Intent(getActivity(), FeedbackActivity.class);
            startActivity(intent);
            return true;
        case MENU_ADDON_INTENT_ID:
            intent = item.getIntent();
            AndroidUtilities.startExternalIntent(getActivity(), intent, ACTIVITY_MENU_EXTERNAL);
            return true;

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
            Task original = new Task();
            original.setId(itemId);

            Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
            Flags.set(Flags.GTASKS_SUPPRESS_SYNC);
            Task clone = taskService.clone(original);
            clone.setValue(Task.CREATION_DATE, DateUtilities.now());
            clone.setValue(Task.COMPLETION_DATE, 0L);
            clone.setValue(Task.DELETION_DATE, 0L);

            clone.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
            GCalHelper.createTaskEventIfEnabled(clone);
            taskService.save(clone);

            intent = new Intent(getActivity(), TaskEditWrapperActivity.class);
            intent.putExtra(TaskEditActivity.TOKEN_ID, clone.getId());
            intent.putExtra(TOKEN_FILTER, filter);
            startActivityForResult(intent, ACTIVITY_EDIT_TASK);
            transitionForTaskEdit();

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
            if(item.getItemId() < CONTEXT_MENU_PLUGIN_ID_FIRST)
                return false;
            if(item.getItemId() - CONTEXT_MENU_PLUGIN_ID_FIRST >= contextItemExposers.length)
                return false;

            AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) item.getMenuInfo();
            Task task = ((ViewHolder)adapterInfo.targetView.getTag()).task;
            contextItemExposers[item.getItemId() - CONTEXT_MENU_PLUGIN_ID_FIRST].invoke(task);

            return true;
        }

        }
    }

    public void onTaskListItemClicked(long taskId) {
        mListener.onTaskListItemClicked(taskId);
    }

    @SuppressWarnings("nls")
    @Override
    public void gesturePerformed(String gesture) {
        if("nav_right".equals(gesture)) {
            //showFilterListActivity();
        }
    }

    @Override
    public void onSortSelected(boolean always, int flags, int sort) {
        sortFlags = flags;
        sortSort = sort;

        if(always) {
            SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(getActivity());
            Editor editor = publicPrefs.edit();
            editor.putInt(SortHelper.PREF_SORT_FLAGS, flags);
            editor.putInt(SortHelper.PREF_SORT_SORT, sort);
            editor.commit();
            ContextManager.getContext().startService(new Intent(ContextManager.getContext(),
                    TasksWidget.WidgetUpdateService.class));
        }

        setUpTaskList();
    }
}
