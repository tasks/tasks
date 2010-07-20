/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.SearchFilter;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.utility.Constants;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListActivity extends ExpandableListActivity {

    // --- menu codes

    private static final int MENU_SEARCH_ID = Menu.FIRST + 0;
    private static final int MENU_HELP_ID = Menu.FIRST + 1;

    private static final int CONTEXT_MENU_SHORTCUT = Menu.FIRST + 2;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 3;

    // --- instance variables

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected DialogUtilities dialogUtilities;

    FilterAdapter adapter = null;
    FilterReceiver filterReceiver = new FilterReceiver();

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    public FilterListActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new StartupService().onStartupApplication(this);

        setContentView(R.layout.filter_list_activity);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setTitle(R.string.FLA_title);
        setUpList();
        getLists();

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

        /*item = menu.add(Menu.NONE, MENU_HELP_ID, Menu.NONE,
                R.string.FLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);*/

        return true;
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
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(filterReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(filterReceiver);
    }

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class FilterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final Parcelable[] filters = intent.getExtras().
                    getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                for (Parcelable item : filters) {
                    adapter.add((FilterListItem)item);
                }
                adapter.notifyDataSetChanged();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expandList(filters);
                    }
                });
            } catch (Exception e) {
                exceptionService.reportError("receive-filter-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON), e);
            }
        }
    }

    /* ======================================================================
     * ===================================================== populating lists
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        adapter = new FilterAdapter(this);
        setListAdapter(adapter);

        registerForContextMenu(getExpandableListView());
        getExpandableListView().setGroupIndicator(
                getResources().getDrawable(R.drawable.expander_group));
    }

    /**
     * Expand the first category filter in this group
     * @param filters
     */
    protected void expandList(Parcelable[] filters) {
        ExpandableListView list = getExpandableListView();
        for(Parcelable filter : filters) {
            if(filter instanceof FilterCategory) {
                for(int i = 0; i < adapter.getGroupCount(); i++)
                    if(adapter.getGroup(i) == filter) {
                        list.expandGroup(i);
                        return;
                    }
            }
        }
    }

    /**
     * Broadcast a request for lists. The request is sent to every
     * application registered to listen for this broadcast. Each application
     * can then add lists to this activity
     */
    protected void getLists() {
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS);
        sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
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
            Intent intent = new Intent(FilterListActivity.this, TaskListActivity.class);
            intent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
            startActivity(intent);
            return true;
        } else if(item instanceof SearchFilter) {
            onSearchRequested();
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
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        onItemClicked(item);
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
            info.targetView.setTag(filter);
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
                    R.drawable.filter_tags1)).getBitmap();
        // create icon by superimposing astrid w/ icon
        Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
                R.drawable.icon_blank)).getBitmap();
        bitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(emblem, new Rect(0, 0, emblem.getWidth(), emblem.getHeight()),
                new Rect(bitmap.getWidth() - 22, bitmap.getHeight() - 22,
                        bitmap.getWidth(), bitmap.getHeight()), null);

        Intent createShortcutIntent = new Intent();
        createShortcutIntent.putExtra(
                Intent.EXTRA_SHORTCUT_INTENT,
                shortcutIntent);
        createShortcutIntent.putExtra(
                Intent.EXTRA_SHORTCUT_NAME, label);
        createShortcutIntent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON, bitmap);
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

        case MENU_HELP_ID: {
            // TODO
            return true;
        }

        case CONTEXT_MENU_SHORTCUT: {
            ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();

            final Intent shortcutIntent = item.getIntent();
            final Filter filter = (Filter)info.targetView.getTag();

            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setPadding(10, 0, 10, 0);
            final EditText editText = new EditText(this);
            editText.setText(filter.listingTitle);
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
            .show();

            return true;
        }

        case CONTEXT_MENU_INTENT: {
            Intent intent = item.getIntent();
            startActivity(intent);
            return true;
        }
        }

        return false;
    }

}
