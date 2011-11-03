/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.io.IOException;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmPreferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.IntentFilter;
import com.todoroo.astrid.core.SearchFilter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.utility.Constants;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListActivity extends ExpandableListActivity {

    // -- extra codes
    public static final String SHOW_BACK_BUTTON = "show_back"; //$NON-NLS-1$

    // --- menu codes

    private static final int MENU_SEARCH_ID = Menu.FIRST + 0;
    private static final int MENU_HELP_ID = Menu.FIRST + 1;
    private static final int MENU_REFRESH_ID = Menu.FIRST + 2;

    private static final String LAST_TAG_REFRESH_KEY = "last_tag_refresh"; //$NON-NLS-1$

    private static final int CONTEXT_MENU_SHORTCUT = Menu.FIRST + 3;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 4;

    private static final int REQUEST_CUSTOM_INTENT = 1;
    private static final int REQUEST_VIEW_TASKS = 2;
    public static final int REQUEST_NEW_BUTTON = 3;

    // --- instance variables

    @Autowired ExceptionService exceptionService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired ActFmSyncService actFmSyncService;

    protected FilterAdapter adapter = null;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public FilterListActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);
        ThemeService.applyTheme(this);

        setContentView(R.layout.filter_list_activity);
        ImageView backButton = (ImageView) findViewById(R.id.back);
        if (!getIntent().getBooleanExtra(SHOW_BACK_BUTTON, true)) {
            backButton.setVisibility(View.GONE);
            findViewById(R.id.headerLogo).setPadding(0, 0, 0, 0);
        }
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                AndroidUtilities.callApiMethod(5, FilterListActivity.this, "overridePendingTransition", //$NON-NLS-1$
                        new Class<?>[] { Integer.TYPE, Integer.TYPE },
                        R.anim.slide_none, R.anim.slide_bottom_out);
            }
        });

        onNewIntent(getIntent());
    }

    /**
     * Called when receiving a new intent. Intents this class handles:
     * <ul>
     * <li>ACTION_SEARCH - displays a search bar
     * <li>ACTION_ADD_LIST - adds new lists to the merge adapter
     * </ul>
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final String intentAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(intentAction)) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            Filter filter = new Filter(null, getString(R.string.FLA_search_filter, query),
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like("%" + //$NON-NLS-1$
                            query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
            intent = new Intent(FilterListActivity.this, TaskListActivity.class);
            intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
            startActivity(intent);
        } else {
            setUpList();
            if (actFmPreferenceService.isLoggedIn())
                onRefreshRequested(false);
        }
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

        item = menu.add(Menu.NONE, MENU_SEARCH_ID, Menu.NONE,
                R.string.FLA_menu_search);
        item.setIcon(android.R.drawable.ic_menu_search);

        if(actFmPreferenceService.isLoggedIn()) {
            item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                    R.string.actfm_FLA_menu_refresh);
            item.setIcon(R.drawable.ic_menu_refresh);
        }

        item = menu.add(Menu.NONE, MENU_HELP_ID, Menu.NONE,
                R.string.FLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);

        return true;
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
        StatisticsService.sessionStart(this);
        if(adapter != null)
            adapter.registerRecevier();
    }

    @Override
    protected void onPause() {
        StatisticsService.sessionPause();
        super.onPause();
        if(adapter != null)
            adapter.unregisterRecevier();
    }

    /* ======================================================================
     * ===================================================== populating lists
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        adapter = new FilterAdapter(this, getExpandableListView(),
                R.layout.filter_adapter_row, false);
        setListAdapter(adapter);

        registerForContextMenu(getExpandableListView());
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    /**
     * Handles items being clicked. Return true if item is handled.
     */
    protected boolean onItemClicked(FilterListItem item) {
        if(item instanceof Filter) {
            Filter filter = (Filter)item;
            if(filter instanceof FilterWithCustomIntent) {
                FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
                customFilter.start(this, REQUEST_VIEW_TASKS);
            } else {
                Intent intent = new Intent(FilterListActivity.this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
                startActivityForResult(intent, REQUEST_VIEW_TASKS);
            }
            AndroidUtilities.callApiMethod(5, this, "overridePendingTransition", //$NON-NLS-1$
                    new Class<?>[] { Integer.TYPE, Integer.TYPE },
                    R.anim.slide_left_in, R.anim.slide_left_out);
            StatisticsService.reportEvent(StatisticsConstants.FILTER_LIST);
            return true;
        } else if(item instanceof SearchFilter) {
            onSearchRequested();
            StatisticsService.reportEvent(StatisticsConstants.FILTER_SEARCH);
        } else if(item instanceof IntentFilter) {
            try {
                ((IntentFilter)item).intent.send();
            } catch (CanceledException e) {
                // ignore
            }
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        FilterListItem item = (FilterListItem) adapter.getChild(groupPosition,
                childPosition);
        return onItemClicked(item);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        onItemClicked(item);
        if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, true);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        onItemClicked(item);
        if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;

        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        FilterListItem item;
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
            item = (FilterListItem) adapter.getChild(groupPos, childPos);
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            item = (FilterListItem) adapter.getGroup(groupPos);
        } else {
            return;
        }

        MenuItem menuItem;

        if(item instanceof Filter) {
            Filter filter = (Filter) item;
            menuItem = menu.add(0, CONTEXT_MENU_SHORTCUT, 0, R.string.FLA_context_shortcut);
            menuItem.setIntent(ShortcutActivity.createIntent(filter));
        }

        for(int i = 0; i < item.contextMenuLabels.length; i++) {
            if(item.contextMenuIntents.length <= i)
                break;
            menuItem = menu.add(0, CONTEXT_MENU_INTENT, 0, item.contextMenuLabels[i]);
            menuItem.setIntent(item.contextMenuIntents[i]);
        }

        if(menu.size() > 0)
            menu.setHeaderTitle(item.listingTitle);
    }

    /**
     * Creates a shortcut on the user's home screen
     *
     * @param shortcutIntent
     * @param label
     */
    private void createShortcut(Filter filter, Intent shortcutIntent, String label) {
        if(label.length() == 0)
            return;

        Bitmap emblem = filter.listingIcon;
        if(emblem == null)
            emblem = ((BitmapDrawable) getResources().getDrawable(
                    R.drawable.gl_list)).getBitmap();

        // create icon by superimposing astrid w/ icon
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
                R.drawable.icon_blank)).getBitmap();
        bitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        int dimension = 22;
        canvas.drawBitmap(emblem, new Rect(0, 0, emblem.getWidth(), emblem.getHeight()),
                new Rect(bitmap.getWidth() - dimension, bitmap.getHeight() - dimension,
                        bitmap.getWidth(), bitmap.getHeight()), null);

        Intent createShortcutIntent = new Intent();
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$

        sendBroadcast(createShortcutIntent);
        Toast.makeText(
                FilterListActivity.this,
                getString(
                        R.string.FLA_toast_onCreateShortcut,
                        label), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {

        // handle my own menus
        switch (item.getItemId()) {
        case MENU_SEARCH_ID: {
            onSearchRequested();
            return true;
        }

        case MENU_REFRESH_ID: {
            onRefreshRequested(true);
            return true;
        }

        case MENU_HELP_ID: {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://weloveastrid.com/help-user-guide-astrid-v3/filters/")); //$NON-NLS-1$
            startActivity(intent);
            return true;
        }

        case CONTEXT_MENU_SHORTCUT: {
            ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();

            final Intent shortcutIntent = item.getIntent();
            FilterListItem filter = ((FilterAdapter.ViewHolder)info.targetView.getTag()).item;
            if(filter instanceof Filter)
                showCreateShortcutDialog(shortcutIntent, (Filter)filter);

            return true;
        }

        case CONTEXT_MENU_INTENT: {
            Intent intent = item.getIntent();
            startActivityForResult(intent, REQUEST_CUSTOM_INTENT);
            return true;
        }
        }

        return false;
    }

    /**
     * Refresh user tags
     */
    private void onRefreshRequested(final boolean manual) {
        if (!manual) {
            long lastFetchDate = Preferences.getLong(LAST_TAG_REFRESH_KEY, 0);
            if(DateUtilities.now() < lastFetchDate + 300000L) {
                return;
            }
        }
        final ProgressDialog progressDialog;

        final NotificationManager nm = new NotificationManager.AndroidNotificationManager(this);
        final Notification notification = new Notification(android.R.drawable.stat_notify_sync, null, System.currentTimeMillis());
        final int notificationId = updateNotification(this, notification);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        if (manual) {
            progressDialog = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        } else {
            progressDialog = null;
            nm.notify(notificationId, notification);
        }

        new Thread(new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                try {
                    Preferences.setLong(LAST_TAG_REFRESH_KEY, DateUtilities.now());
                    actFmSyncService.fetchTags(0);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.clear();
                            adapter.getLists();
                        }
                    });

                } catch (IOException e) {
                    if (manual)
                        exceptionService.displayAndReportError(FilterListActivity.this, "refresh-tags-io", e);
                    else
                        exceptionService.reportError("refresh-tags-io", e);
                } catch (JSONException e) {
                    if (manual)
                        exceptionService.displayAndReportError(FilterListActivity.this, "refresh-tags-json", e);
                    else
                        exceptionService.reportError("refresh-tags-io", e);
                } finally {
                    if (manual)
                        DialogUtilities.dismissDialog(FilterListActivity.this, progressDialog);
                    else
                        nm.cancel(notificationId);
                }
            }
        }).start();
    }

    private int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.actfm_notification_title);
        Intent intent = new Intent(context, ActFmPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return Constants.NOTIFICATION_SYNC;
    }

    private void showCreateShortcutDialog(final Intent shortcutIntent,
            final Filter filter) {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setPadding(10, 0, 10, 0);
        final EditText editText = new EditText(this);
        if(filter.listingTitle == null)
            filter.listingTitle = ""; //$NON-NLS-1$
        editText.setText(filter.listingTitle.
                replaceAll("\\(\\d+\\)$", "").trim()); //$NON-NLS-1$ //$NON-NLS-2$
        frameLayout.addView(editText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        final Runnable createShortcut = new Runnable() {
            @Override
            public void run() {
                String label = editText.getText().toString();
                createShortcut(filter, shortcutIntent, label);
            }
        };
        editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL) {
                    createShortcut.run();
                    return true;
                }
                return false;
            }
        });

        new AlertDialog.Builder(this)
        .setTitle(R.string.FLA_shortcut_dialog_title)
        .setMessage(R.string.FLA_shortcut_dialog)
        .setView(frameLayout)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                createShortcut.run();
            }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show().setOwnerActivity(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // will get lists automatically
        adapter.clear();

        super.onActivityResult(requestCode, resultCode, data);
    }

}
