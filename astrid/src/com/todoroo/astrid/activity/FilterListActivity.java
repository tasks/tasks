/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.json.JSONException;
import org.weloveastrid.rmilk.MilkPreferences;
import org.weloveastrid.rmilk.MilkUtilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.todoroo.astrid.activity.TaskListActivity.IntentWithLabel;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.SyncAction;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.MetadataHelper;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.tags.TagsPlugin;
import com.todoroo.astrid.utility.Constants;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListActivity extends ExpandableListFragment {

    public static final String TAG_FILTERLIST_FRAGMENT = "filterlist_fragment";

    // -- extra codes
    //public static final String SHOW_BACK_BUTTON = "show_back"; //$NON-NLS-1$

    // --- menu codes

    private static final int MENU_SEARCH_ID = R.string.FLA_menu_search;
    private static final int MENU_HELP_ID = R.string.FLA_menu_help;
    private static final int MENU_REFRESH_ID = R.string.actfm_FLA_menu_refresh;
    private static final int MENU_NEW_FILTER_ID = R.string.FLA_new_filter;

    private static final String LAST_TAG_REFRESH_KEY = "last_tag_refresh"; //$NON-NLS-1$

    private static final int CONTEXT_MENU_SHORTCUT = R.string.FLA_context_shortcut;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 4;

    private static final int REQUEST_CUSTOM_INTENT = 1;
    static final int REQUEST_VIEW_TASKS = 2;
    public static final int REQUEST_NEW_BUTTON = 3;

    // --- instance variables

    @Autowired ExceptionService exceptionService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired ActFmSyncService actFmSyncService;

    protected SyncActionReceiver syncActionReceiver = new SyncActionReceiver();
    private final LinkedHashSet<SyncAction> syncActions = new LinkedHashSet<SyncAction>();
    protected FilterAdapter adapter = null;
    private boolean mDualFragments;

    private OnFilterItemClickedListener mListener;

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    /** Container Activity must implement this interface and we ensure
     * that it does during the onAttach() callback
     */
    public interface OnFilterItemClickedListener {
        public boolean onFilterItemClicked(FilterListItem item);
    }

    public FilterListActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback interface
        try {
            mListener = (OnFilterItemClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFilterItemClickedListener"); //$NON-NLS-1$
        }
    }

    /**  Called when loading up the activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tell the framework to try to keep this fragment around
        // during a configuration change.
//        setRetainInstance(true);

        new StartupService().onStartupApplication(getActivity());
    }

    /* (non-Javadoc)
     * @see com.todoroo.astrid.fragment.ExpandableListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.filter_list_activity, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
        ImageView backButton = (ImageView) getView().findViewById(R.id.back);
        Button newListButton = (Button) getView().findViewById(R.id.new_list_button);

        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
            }
        });

        newListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = TagsPlugin.newTagDialog(getActivity());
                startActivity(intent);
                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
            }
        });

        onContentChanged();

        onNewIntent(getActivity().getIntent());

        Fragment tasklistFrame = getFragmentManager().findFragmentByTag(TaskListActivity.TAG_TASKLIST_FRAGMENT);
        mDualFragments = (tasklistFrame != null) && tasklistFrame.isInLayout();

        if (mDualFragments) {
            // In dual-pane mode, the list view highlights the selected item.
            getExpandableListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getExpandableListView().setItemsCanFocus(false);
        }
    }

    /**
     * Called when receiving a new intent. Intents this class handles:
     * <ul>
     * <li>ACTION_SEARCH - displays a search bar
     * <li>ACTION_ADD_LIST - adds new lists to the merge adapter
     * </ul>
     */
    public void onNewIntent(Intent intent) {
        final String intentAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(intentAction)) {
            String query = intent.getStringExtra(SearchManager.QUERY).trim();
            Filter filter = new Filter(null, getString(R.string.FLA_search_filter, query),
                    new QueryTemplate().where(Functions.upper(Task.TITLE).like("%" + //$NON-NLS-1$
                            query.toUpperCase() + "%")), //$NON-NLS-1$
                    null);
            intent = new Intent(getActivity(), TaskListWrapperActivity.class);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item;

        item = menu.add(Menu.NONE, MENU_NEW_FILTER_ID, Menu.NONE,
                R.string.FLA_new_filter);
        item.setIcon(android.R.drawable.ic_menu_add);

        item = menu.add(Menu.NONE, MENU_SEARCH_ID, Menu.NONE,
                R.string.FLA_menu_search);
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                R.string.TLA_menu_sync);
        item.setIcon(R.drawable.ic_menu_refresh);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, MENU_HELP_ID, 1,
                R.string.FLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);
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
        if(adapter != null)
            adapter.registerRecevier();

        // also load sync actions
        getActivity().registerReceiver(syncActionReceiver, new android.content.IntentFilter(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS));
        syncActions.clear();
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_SYNC_ACTIONS);
        getActivity().sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public void onPause() {
        StatisticsService.sessionPause();
        super.onPause();
        if(adapter != null)
            adapter.unregisterRecevier();
        getActivity().unregisterReceiver(syncActionReceiver);
    }

    /* ======================================================================
     * ===================================================== populating lists
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        adapter = new FilterAdapter(getActivity(), getExpandableListView(),
                R.layout.filter_adapter_row, false);
        setListAdapter(adapter);

        registerForContextMenu(getExpandableListView());
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
//        if (mDualFragments)
//        {
//            setSelectedChild(groupPosition, childPosition, false);
//            setItemChecked((int) getSelectedPosition(), true);
//        }
        FilterListItem item = (FilterListItem) adapter.getChild(groupPosition,
                childPosition);
        return mListener.onFilterItemClicked(item);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        mListener.onFilterItemClicked(item);
        if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, true);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        mListener.onFilterItemClicked(item);
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

        android.view.MenuItem menuItem;

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
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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

        getActivity().sendBroadcast(createShortcutIntent);
        Toast.makeText(getActivity(),
                getString(R.string.FLA_toast_onCreateShortcut, label), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
            case MENU_REFRESH_ID: {
                performSyncAction();
                //onRefreshRequested(true);
                return true;
            }
            case MENU_SEARCH_ID: {
                getActivity().onSearchRequested();
                return true;
            }
            case MENU_HELP_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://weloveastrid.com/help-user-guide-astrid-v3/filters/")); //$NON-NLS-1$
                startActivity(intent);
                return true;
            }
            case MENU_NEW_FILTER_ID : {
                Intent intent = new Intent(getActivity(), CustomFilterActivity.class);
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
            case android.R.id.home: {
                // TODO: maybe invoke a dashboard later
                return true;
            }
            default: {
                Fragment tasklist = getSupportFragmentManager().findFragmentByTag(TaskListActivity.TAG_TASKLIST_FRAGMENT);
                if (tasklist != null && tasklist.isInLayout())
                    return tasklist.onOptionsItemSelected(item);
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

        final NotificationManager nm = new NotificationManager.AndroidNotificationManager(getActivity());
        final Notification notification = new Notification(android.R.drawable.stat_notify_sync, null, System.currentTimeMillis());
        final int notificationId = updateNotification(getActivity(), notification);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        if (manual) {
            progressDialog = DialogUtilities.progressDialog(getActivity(), getString(R.string.DLG_please_wait));
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

                    Activity activity = getActivity();
                    if (activity != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.clear();
                                adapter.getLists();
                            }
                        });
                    }

                } catch (IOException e) {
                    if (manual)
                        exceptionService.displayAndReportError(getActivity(), "refresh-tags-io", e);
                    else
                        exceptionService.reportError("refresh-tags-io", e);
                } catch (JSONException e) {
                    if (manual)
                        exceptionService.displayAndReportError(getActivity(), "refresh-tags-json", e);
                    else
                        exceptionService.reportError("refresh-tags-io", e);
                } finally {
                    if (manual)
                        DialogUtilities.dismissDialog(getActivity(), progressDialog);
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
        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.setPadding(10, 0, 10, 0);
        final EditText editText = new EditText(getActivity());
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

        new AlertDialog.Builder(getActivity())
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
        .show().setOwnerActivity(getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_CANCELED)
            // will get lists automatically
            adapter.clear();

        super.onActivityResult(requestCode, resultCode, data);
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

    private void performSyncAction() {
        if (syncActions.size() == 0) {
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
        else if(syncActions.size() == 1) {
            SyncAction syncAction = syncActions.iterator().next();
            try {
                if (actFmPreferenceService.isLoggedIn())
                    onRefreshRequested(true);
                else {
                    syncAction.intent.send();
                    Toast.makeText(getActivity(), R.string.SyP_progress_toast,
                            Toast.LENGTH_LONG).show();
                }
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
                        SyncAction action = actions[which];
                        if (action.label.contains("Astrid"))
                            onRefreshRequested(true);
                        else {
                            action.intent.send();
                            Toast.makeText(getActivity(), R.string.SyP_progress_toast,
                                    Toast.LENGTH_LONG).show();
                        }
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
        ArrayAdapter<TYPE> syncAdapter = new ArrayAdapter<TYPE>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items);

        // show a menu of available options
        new AlertDialog.Builder(getActivity())
        .setTitle(R.string.SyP_label)
        .setAdapter(syncAdapter, listener)
        .show().setOwnerActivity(getActivity());
    }

}
