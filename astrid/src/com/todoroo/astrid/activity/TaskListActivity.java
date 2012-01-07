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

import android.app.AlertDialog;
import android.app.ListActivity;
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
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
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
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceInputAssistant;
import com.todoroo.astrid.welcome.HelpInfoPopover;
import com.todoroo.astrid.welcome.WelcomeLogin;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Primary activity for the Bente application. Shows a list of upcoming
 * tasks and a user's coaches.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListActivity extends ListActivity implements OnScrollListener,
        GestureInterface, OnSortSelectedListener {

    // --- activities

    private static final long BACKGROUND_REFRESH_INTERVAL = 120000L;
    public static final int ACTIVITY_EDIT_TASK = 0;
    public static final int ACTIVITY_SETTINGS = 1;
    public static final int ACTIVITY_SORT = 2;
    public static final int ACTIVITY_ADDONS = 3;
    public static final int ACTIVITY_MENU_EXTERNAL = 4;

    // --- menu codes

    protected static final int MENU_LISTS_ID = Menu.FIRST + 1;
    protected static final int MENU_ADDONS_ID = Menu.FIRST + 2;
    protected static final int MENU_SETTINGS_ID = Menu.FIRST + 3;
    protected static final int MENU_SYNC_ID = Menu.FIRST + 5;
    protected static final int MENU_SUPPORT_ID = Menu.FIRST + 6;
    protected static final int MENU_ADDON_INTENT_ID = Menu.FIRST + 7;

    protected static final int CONTEXT_MENU_EDIT_TASK_ID = Menu.FIRST + 20;
    protected static final int CONTEXT_MENU_COPY_TASK_ID = Menu.FIRST + 21;
    protected static final int CONTEXT_MENU_DELETE_TASK_ID = Menu.FIRST + 22;
    protected static final int CONTEXT_MENU_UNDELETE_TASK_ID = Menu.FIRST + 23;
    protected static final int CONTEXT_MENU_PURGE_TASK_ID = Menu.FIRST + 24;
    protected static final int CONTEXT_MENU_BROADCAST_INTENT_ID = Menu.FIRST + 25;
    protected static final int CONTEXT_MENU_PLUGIN_ID_FIRST = Menu.FIRST + 26;

    // --- constants

    /** token for passing a {@link Filter} object through extras */
    public static final String TOKEN_FILTER = "filter"; //$NON-NLS-1$

    /** token for indicating source of TLA launch */
    public static final String TOKEN_SOURCE = "source"; //$NON-NLS-1$

    public static final String TOKEN_OVERRIDE_ANIM = "finishAnim"; //$NON-NLS-1$

    private static final String LAST_AUTOSYNC_ATTEMPT = "last-autosync"; //$NON-NLS-1$

    // --- instance variables

    @Autowired ExceptionService exceptionService;

    @Autowired protected TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired Database database;

    @Autowired AddOnService addOnService;

    @Autowired UpgradeService upgradeService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

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

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    static {
        AstridDependencyInjector.initialize();
    }

    /**
     * @return view to attach to the body of the task list. must contain two
     * elements, a view with id android:id/empty and a list view with id
     * android:id/list. It should NOT be attached to root
     */
    protected View getListBody(ViewGroup root) {
        if(AndroidUtilities.getSdkVersion() > 3)
            return getLayoutInflater().inflate(R.layout.task_list_body_standard, root, false);
        else
            return getLayoutInflater().inflate(R.layout.task_list_body_api3, root, false);
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContextManager.setContext(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);

        new StartupService().onStartupApplication(this);
        ThemeService.applyTheme(this);
        ViewGroup parent = (ViewGroup) getLayoutInflater().inflate(R.layout.task_list_activity, null);
        parent.addView(getListBody(parent), 1);
        setContentView(parent);

        if(database == null)
            return;

        database.openForWriting();
        setUpUiComponents();
        onNewIntent(getIntent());

        if(Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1) > -1)
            upgradeService.showChangeLog(this, Preferences.getInt(AstridPreferences.P_UPGRADE_FROM, -1));

        if(getIntent().hasExtra(TOKEN_SOURCE)) {
            switch(getIntent().getIntExtra(TOKEN_SOURCE, Constants.SOURCE_DEFAULT)) {
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

        runOnUiThread(new Runnable() {
           public void run() {
               Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
           }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            overrideFinishAnim = extras.getBoolean(TOKEN_OVERRIDE_ANIM);
        } else {
            overrideFinishAnim = false;
        }
        String intentAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(intentAction)) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            Filter searchFilter = new Filter(null, getString(R.string.FLA_search_filter, query),
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like("%" + //$NON-NLS-1$
                            query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
            intent = new Intent(this, TaskListActivity.class);
            intent.putExtra(TaskListActivity.TOKEN_FILTER, searchFilter);
            startActivity(intent);
            finish();
            return;
        } else if(extras != null && extras.containsKey(TOKEN_FILTER)) {
            filter = extras.getParcelable(TOKEN_FILTER);
            isFilter = true;
        } else {
            filter = CoreFilterExposer.buildInboxFilter(getResources());
            isFilter = false;
        }

        setUpTaskList();
        if(Constants.DEBUG)
            setTitle("[D] " + filter.title); //$NON-NLS-1$

        contextMenuExtensionLoader.loadInNewThread(this);
    }

    protected void addSyncRefreshMenuItem(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, MENU_SYNC_ID, Menu.NONE,
                R.string.TLA_menu_sync);
        item.setIcon(R.drawable.ic_menu_refresh);
    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            menu.clear();

        MenuItem item;

        addSyncRefreshMenuItem(menu);

        item = menu.add(Menu.NONE, MENU_LISTS_ID, Menu.NONE,
                R.string.tag_TLA_menu);
        item.setIcon(R.drawable.ic_menu_lists);

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

    protected void setUpUiComponents() {
        ((ImageView)findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Preferences.setBoolean(R.string.p_showed_lists_help, true);
                showFilterListActivity();
            }
        });

        findViewById(R.id.sort_settings).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SORT);
                AlertDialog dialog = SortSelectionActivity.createDialog(TaskListActivity.this,
                        TaskListActivity.this, sortFlags, sortSort);
                dialog.show();
            }
        });

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
        quickAddBox = (EditText) findViewById(R.id.quickAddText);
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


        quickAddButton = ((ImageButton)findViewById(R.id.quickAddButton));

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
        voiceAddButton = (ImageButton) findViewById(R.id.voiceAddButton);
        int prompt = R.string.voice_edit_title_prompt;
        if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
            prompt = R.string.voice_create_prompt;
        voiceInputAssistant = new VoiceInputAssistant(this,voiceAddButton,quickAddBox);
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
        ((TextView)findViewById(android.R.id.empty)).setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View v) {
                quickAddButton.performClick();
            }
         });


        // gestures / animation
        try {
            GestureService.registerGestureDetector(this, R.id.gestures, R.raw.gestures, this);
        } catch (VerifyError e) {
            // failed check, no gestures :P
        }

        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(this);
        sortFlags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
        sortSort = publicPrefs.getInt(SortHelper.PREF_SORT_SORT, 0);

        // dithering
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    private void initiateAutomaticSync() {
        if (!actFmPreferenceService.isLoggedIn()) return;
        long lastFetchDate = actFmPreferenceService.getLastSyncDate();
        long lastAutosyncAttempt = Preferences.getLong(LAST_AUTOSYNC_ATTEMPT, 0);

        long lastTry = Math.max(lastFetchDate, lastAutosyncAttempt);
        if(DateUtilities.now() < lastTry + 300000L)
            return;
        new Thread() {
            @Override
            public void run() {
                Preferences.setLong(LAST_AUTOSYNC_ATTEMPT, DateUtilities.now());
                new ActFmSyncProvider().synchronize(TaskListActivity.this, false);
            }
        }.start();
    }

    // Subclasses can override these to customize extras in quickadd intent
    protected Intent getOnClickQuickAddIntent(Task t) {
        Intent intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, t.getId());
        return intent;
    }

    protected Intent getOnLongClickQuickAddIntent(Task t) {
        Intent intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, t.getId());
        return intent;
    }

    public void transitionForTaskEdit() {
        AndroidUtilities.callApiMethod(5, this, "overridePendingTransition",
                new Class<?>[] { Integer.TYPE, Integer.TYPE },
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
                runOnUiThread(new Runnable() {
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        StatisticsService.sessionStop(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AndroidUtilities.copyDatabases(this, "/sdcard/tmp");

        StatisticsService.sessionStart(this);
        if (addOnService.hasPowerPack() &&
                Preferences.getBoolean(R.string.p_voiceInputEnabled, true) &&
                voiceInputAssistant.isVoiceInputAvailable()) {
            voiceAddButton.setVisibility(View.VISIBLE);
        } else {
            voiceAddButton.setVisibility(View.GONE);
        }

        registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DETAILS));
        registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_DECORATIONS));
        registerReceiver(detailReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_ACTIONS));
        registerReceiver(refreshReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        registerReceiver(syncActionReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS));
        setUpBackgroundJobs();

        if (!Preferences.getBoolean(WelcomeLogin.KEY_SHOWED_WELCOME_LOGIN, false)) {
            Intent showWelcomeLogin = new Intent(this, WelcomeLogin.class);
            showWelcomeLogin.putExtra(ActFmLoginActivity.SHOW_TOAST, false);
            startActivity(showWelcomeLogin);
            Preferences.setBoolean(WelcomeLogin.KEY_SHOWED_WELCOME_LOGIN, true);
            return;
        }

        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            HelpInfoPopover.showPopover(TaskListActivity.this, quickAddBox, R.string.help_popover_add_task, null);
            Preferences.setBoolean(R.string.p_showed_add_task_help, true);
        } else if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            showTaskEditHelpPopover();
        } else if (Preferences.isSet(getString(R.string.p_showed_lists_help)) &&
                !Preferences.getBoolean(R.string.p_showed_lists_help, false)) {
            HelpInfoPopover.showPopover(TaskListActivity.this, findViewById(R.id.back), R.string.help_popover_lists, null);
            Preferences.setBoolean(R.string.p_showed_lists_help, true);
        }

        if (filter.title != null && filter.title.equals(getString(R.string.BFE_Active))) {
            initiateAutomaticSync();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
        try {
            unregisterReceiver(detailReceiver);
            unregisterReceiver(refreshReceiver);
            unregisterReceiver(syncActionReceiver);
        } catch (IllegalArgumentException e) {
            // might not have fully initialized
        }
        backgroundTimer.cancel();
    }

    @Override
    public void finish() {
        super.finish();
        if (overrideFinishAnim) {
            AndroidUtilities.callApiMethod(5, this, "overridePendingTransition",
                    new Class<?>[] { Integer.TYPE, Integer.TYPE },
                    R.anim.slide_right_in, R.anim.slide_right_out);
        }
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

            runOnUiThread(new Runnable() {
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && Flags.checkAndClear(Flags.REFRESH)) {
            taskAdapter.flushCaches();
            loadTaskListContent(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            finish();
            startActivity(getIntent());
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (!Preferences.getBoolean(R.string.p_showed_add_task_help, false)) {
            HelpInfoPopover.showPopover(TaskListActivity.this, quickAddBox, R.string.help_popover_add_task, null);
            Preferences.setBoolean(R.string.p_showed_add_task_help, true);
        }

        if(resultCode != RESULT_CANCELED) {
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

                Intent intent = new Intent(TaskListActivity.this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.TOKEN_FILTER, assignedFilter);
                intent.putExtra(TaskListActivity.TOKEN_OVERRIDE_ANIM, true);
                startActivityForResult(intent, 0);
                transitionForTaskEdit();
            }
        };
        DialogUtilities.okCancelCustomDialog(this,
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
        startManagingCursor(taskCursor);

        if(oldListItemSelected != ListView.INVALID_POSITION &&
                oldListItemSelected < taskCursor.getCount())
            getListView().setSelection(oldListItemSelected);

        // also load sync actions
        syncActions.clear();
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_SYNC_ACTIONS);
        sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public void setTitle(CharSequence title) {
        ((TextView)findViewById(R.id.listLabel)).setText(title);
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
        setTitle(filter.title);

        // perform query
        TodorooCursor<Task> currentCursor = taskService.fetchFiltered(
                sqlQueryTemplate.get(), null, TaskAdapter.PROPERTIES);
        startManagingCursor(currentCursor);

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
                showTaskEditHelpPopover();
                break;
            }
        }
    }

    private void showTaskEditHelpPopover() {
        if (!Preferences.getBoolean(R.string.p_showed_tap_task_help, false)) {
            Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
                                        HelpInfoPopover.showPopover(TaskListActivity.this, findViewById(R.id.back), R.string.help_popover_lists, null);
                                    }
                                }
                            };
                            HelpInfoPopover.showPopover(TaskListActivity.this, view, R.string.help_popover_tap_task, onDismiss);
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
                Uri calendarUri = GCalHelper.createTaskEvent(task, getContentResolver(), new ContentValues());
                task.setValue(Task.CALENDAR_URI, calendarUri.toString());
                taskService.save(task);
            }

            TextView quickAdd = (TextView)findViewById(R.id.quickAddText);
            quickAdd.setText(""); //$NON-NLS-1$

            if(selectNewTask) {
                loadTaskListContent(true);
                selectCustomId(task.getId());
            }

            StatisticsService.reportEvent(StatisticsConstants.TASK_CREATED_TASKLIST);
            return task;
        } catch (Exception e) {
            exceptionService.displayAndReportError(this, "quick-add-task", e);
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
                MenuItem menuItem = menu.add(id, CONTEXT_MENU_BROADCAST_INTENT_ID, Menu.NONE,
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

    private void performSyncAction() {
        if (syncActions.size() == 0) {
            String desiredCategory = getString(R.string.SyP_label);

            // Get a list of all sync plugins and bring user to the prefs pane
            // for one of them
            Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            PackageManager pm = getPackageManager();
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
        else if(syncActions.size() == 1) {
            SyncAction syncAction = syncActions.iterator().next();
            try {
                syncAction.intent.send();
                Toast.makeText(this, R.string.SyP_progress_toast,
                        Toast.LENGTH_LONG).show();
            } catch (CanceledException e) {
                //
            }
        } else {
            // We have >1 sync actions, pop up a dialogue so the user can
            // select just one of them (only sync one at a time)
            final SyncAction[] actions = syncActions.toArray(new SyncAction[syncActions.size()]);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface click, int which) {
                    try {
                        actions[which].intent.send();
                        Toast.makeText(TaskListActivity.this, R.string.SyP_progress_toast,
                                Toast.LENGTH_LONG).show();
                    } catch (CanceledException e) {
                        //
                    }
                }
            };
            showSyncOptionMenu(actions, listener);
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
        ArrayAdapter<TYPE> adapter = new ArrayAdapter<TYPE>(this,
                android.R.layout.simple_spinner_dropdown_item, items);

        // show a menu of available options
        new AlertDialog.Builder(this)
        .setTitle(R.string.SyP_label)
        .setAdapter(adapter, listener)
        .show().setOwnerActivity(this);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        Intent intent;
        long itemId;

        // handle my own menus
        switch (item.getItemId()) {
        case MENU_LISTS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_LISTS);
            showFilterListActivity();
            return true;
        case MENU_ADDONS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_ADDONS);
            intent = new Intent(this, AddOnActivity.class);
            startActivityForResult(intent, ACTIVITY_ADDONS);
            return true;
        case MENU_SETTINGS_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SETTINGS);
            intent = new Intent(this, EditPreferences.class);
            startActivityForResult(intent, ACTIVITY_SETTINGS);
            return true;
        case MENU_SYNC_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_SYNC);
            performSyncAction();
            return true;
        case MENU_SUPPORT_ID:
            StatisticsService.reportEvent(StatisticsConstants.TLA_MENU_HELP);
            intent = new Intent(this, FeedbackActivity.class);
            startActivity(intent);
            return true;
        case MENU_ADDON_INTENT_ID:
            intent = item.getIntent();
            AndroidUtilities.startExternalIntent(this, intent, ACTIVITY_MENU_EXTERNAL);
            return true;

        // --- context menu items

        case CONTEXT_MENU_BROADCAST_INTENT_ID: {
            intent = item.getIntent();
            sendBroadcast(intent);
            return true;
        }

        case CONTEXT_MENU_EDIT_TASK_ID: {
            itemId = item.getGroupId();
            intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
            intent.putExtra(TaskEditActivity.TOKEN_ID, itemId);
            startActivityForResult(intent, ACTIVITY_EDIT_TASK);
            transitionForTaskEdit();
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

            intent = new Intent(TaskListActivity.this, TaskEditActivity.class);
            intent.putExtra(TaskEditActivity.TOKEN_ID, clone.getId());
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

    @SuppressWarnings("nls")
    @Override
    public void gesturePerformed(String gesture) {
        if("nav_right".equals(gesture)) {
            showFilterListActivity();
        }
    }

    @SuppressWarnings("nls")
    private void showFilterListActivity() {
        Intent intent = new Intent(TaskListActivity.this,
                FilterListActivity.class);
        startActivity(intent);
        AndroidUtilities.callApiMethod(5, this, "overridePendingTransition",
                new Class<?>[] { Integer.TYPE, Integer.TYPE },
                R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public void onSortSelected(boolean always, int flags, int sort) {
        sortFlags = flags;
        sortSort = sort;

        if(always) {
            SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(this);
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
