/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.tags.TagsPlugin;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListActivity extends ListFragment {

    public static final String TAG_FILTERLIST_FRAGMENT = "filterlist_fragment";

    public static final String TOKEN_LAST_SELECTED = "lastSelected";

    // -- extra codes
    //public static final String SHOW_BACK_BUTTON = "show_back"; //$NON-NLS-1$

    // --- menu codes

    private static final int MENU_SEARCH_ID = R.string.FLA_menu_search;
    private static final int MENU_HELP_ID = R.string.FLA_menu_help;
    private static final int MENU_NEW_FILTER_ID = R.string.FLA_new_filter;
    //private static final int MENU_NEW_LIST_ID = R.string.FLA_new_list;


    private static final int CONTEXT_MENU_SHORTCUT = R.string.FLA_context_shortcut;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 4;

    private static final int REQUEST_CUSTOM_INTENT = 1;
    static final int REQUEST_VIEW_TASKS = 2;
    public static final int REQUEST_NEW_BUTTON = 3;
    public static final int REQUEST_NEW_LIST = 4;
    public static final int REQUEST_NEW_FILTER = 5;

    // --- instance variables

    @Autowired ExceptionService exceptionService;

    protected FilterAdapter adapter = null;

    private final RefreshReceiver refreshReceiver = new RefreshReceiver();

    private OnFilterItemClickedListener mListener;

    private View newListButton;

    private boolean mDualFragments;

    private int mSelectedIndex;

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
        Activity activity = getActivity();
        ViewGroup parent = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.filter_list_activity, container, false);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
        //ImageView backButton = (ImageView) getView().findViewById(R.id.back);
        newListButton = getView().findViewById(R.id.new_list_button);

//        backButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getActivity().finish();
//                AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
//            }
//        });

        newListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = TagsPlugin.newTagDialog(getActivity());
                getActivity().startActivityForResult(intent, REQUEST_NEW_LIST);
                if (!AndroidUtilities.isTabletSized(getActivity()))
                    AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
            }
        });

        AstridWrapperActivity activity = (AstridWrapperActivity) getActivity();
        mDualFragments = activity.isMultipleFragments();
        if (mDualFragments)
            mSelectedIndex = activity.getIntent().getIntExtra(TOKEN_LAST_SELECTED, 0);
        onNewIntent(activity.getIntent());

        if (mDualFragments) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setItemsCanFocus(false);
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
        if (((AstridWrapperActivity) getActivity()).isMultipleFragments())
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);


//        item = menu.add(Menu.NONE, MENU_HELP_ID, 1,
//                R.string.FLA_menu_help);
//        item.setIcon(android.R.drawable.ic_menu_help);
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
        Activity activity = getActivity();

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_SYNC_ACTIONS);
        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        if (activity instanceof TaskListWrapperActivity) {
            ((TaskListWrapperActivity) activity).setupPopoverWithFilterList(this);
        }

        activity.registerReceiver(refreshReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));

    }

    @Override
    public void onPause() {
        StatisticsService.sessionPause();
        super.onPause();
        if(adapter != null)
            adapter.unregisterRecevier();
        try {
            getActivity().unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Might not have fully initialized
        }
    }

    /* ======================================================================
     * ===================================================== populating lists
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        adapter = new FilterAdapter(getActivity(), getListView(),
                R.layout.filter_adapter_row, false);
        setListAdapter(adapter);

        adapter.setLastSelected(mSelectedIndex);

        registerForContextMenu(getListView());
    }


    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */


    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        if (mDualFragments)
            getListView().setItemChecked(position, true);
        Filter item = adapter.getItem(position);
        mSelectedIndex = position;
        adapter.setLastSelected(mSelectedIndex);
        getActivity().getIntent().putExtra(TOKEN_LAST_SELECTED, mSelectedIndex);
        mListener.onFilterItemClicked(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        Filter item = adapter.getItem(info.position);

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
    public boolean onContextItemSelected(MenuItem item) {
        // called when context menu appears
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
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
                getActivity().startActivityForResult(intent, REQUEST_NEW_FILTER);
                return true;
            }
            case CONTEXT_MENU_SHORTCUT: {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
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

    public void refresh() {
        adapter.clear();
        adapter.getLists();
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
                    refresh();
                }
            });
        }
    }
}
