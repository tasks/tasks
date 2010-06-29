/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.service.AstridDependencyInjector;
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
    String search = null;
    FilterReceiver filterReceiver = new FilterReceiver();

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    static {
        AstridDependencyInjector.initialize();
    }

    public FilterListActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

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
            search = intent.getStringExtra(SearchManager.QUERY);
            dialogUtilities.okDialog(this, "TODO!", null); //$NON-NLS-1$
        } else {
            search = null;
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
                Parcelable[] filters = intent.getExtras().
                    getParcelableArray(AstridApiConstants.EXTRAS_ITEMS);
                for (Parcelable item : filters) {
                    adapter.add((FilterListItem)item);
                }
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                exceptionService.reportError("receive-filter-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_PLUGIN), e);
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
     * Broadcast a request for lists. The request is sent to every
     * application registered to listen for this broadcast. Each application
     * can then add lists to the
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
            Intent shortcutIntent = new Intent(this, TaskListActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.putExtra(TaskListActivity.TOKEN_SHORTCUT_TITLE, filter.title);
            shortcutIntent.putExtra(TaskListActivity.TOKEN_SHORTCUT_SQL, filter.sqlQuery);
            shortcutIntent.putExtra(TaskListActivity.TOKEN_SHORTCUT_NEW_TASK_SQL, filter.valuesForNewTasks);
            menuItem.setIntent(shortcutIntent);
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
            Filter filter = (Filter)info.targetView.getTag();

            String dialogText = getString(R.string.FLA_shortcut_dialog);
            final EditText editText = new EditText(this);
            editText.setText(filter.listingTitle);

            dialogUtilities.viewDialog(this, dialogText, editText,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String label = editText.getText().toString();

                            Intent createShortcutIntent = new Intent();
                            createShortcutIntent.putExtra(
                                    Intent.EXTRA_SHORTCUT_INTENT,
                                    shortcutIntent);
                            createShortcutIntent.putExtra(
                                    Intent.EXTRA_SHORTCUT_NAME, label);
                            createShortcutIntent.putExtra(
                                    Intent.EXTRA_SHORTCUT_ICON,
                                    ((BitmapDrawable) getResources().getDrawable(
                                            R.drawable.icon_tag)).getBitmap());
                            createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$

                            sendBroadcast(createShortcutIntent);
                            Toast.makeText(
                                    FilterListActivity.this,
                                    getString(
                                            R.string.FLA_toast_onCreateShortcut,
                                            label), Toast.LENGTH_LONG).show();
                        }
                    }, null);

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
