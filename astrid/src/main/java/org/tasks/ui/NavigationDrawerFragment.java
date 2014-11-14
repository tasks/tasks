package org.tasks.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.InjectingFragment;
import org.tasks.injection.Injector;

import javax.inject.Inject;

public class NavigationDrawerFragment extends InjectingFragment {

    private static final Logger log = LoggerFactory.getLogger(NavigationDrawerFragment.class);

    public static final int FRAGMENT_NAVIGATION_DRAWER = R.id.navigation_drawer;

    public static final String TOKEN_LAST_SELECTED = "lastSelected"; //$NON-NLS-1$

    private static final int CONTEXT_MENU_SHORTCUT = R.string.FLA_context_shortcut;
    private static final int CONTEXT_MENU_INTENT = Menu.FIRST + 4;

    public static final int REQUEST_CUSTOM_INTENT = 10;
    public static final int REQUEST_NEW_LIST = 4;

    public FilterAdapter adapter = null;

    private final RefreshReceiver refreshReceiver = new RefreshReceiver();

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private OnFilterItemClickedListener mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;

    @Inject FilterCounter filterCounter;
    @Inject Injector injector;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(TOKEN_LAST_SELECTED);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);

        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

        setUpList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    protected void setUpList() {
        adapter.setListView(mDrawerListView);
        mDrawerListView.setAdapter(adapter);
        registerForContextMenu(mDrawerListView);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(FRAGMENT_NAVIGATION_DRAWER);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
//                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(adapter != null) {
            adapter.unregisterRecevier();
        }
        try {
            getActivity().unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Might not have fully initialized
            log.error(e.getMessage(), e);
        }
    }

    private void selectItem(int position) {
        Filter item = adapter.getItem(position);
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        closeMenu();
        if (mCallbacks != null) {
            mCallbacks.onFilterItemClicked(item);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (OnFilterItemClickedListener) activity;
        adapter = new FilterAdapter(injector, filterCounter, getActivity(), null, R.layout.filter_adapter_row, false, false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TOKEN_LAST_SELECTED, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDrawerLayout != null && isDrawerOpen()) {
            menu.clear();
//            showGlobalContextActionBar();
        }
        inflater.inflate(R.menu.global, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        // called when context menu appears
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case CONTEXT_MENU_SHORTCUT: {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                final Intent shortcutIntent = item.getIntent();
                FilterListItem filter = ((FilterAdapter.ViewHolder)info.targetView.getTag()).item;
                if(filter instanceof Filter) {
                    showCreateShortcutDialog(getActivity(), shortcutIntent, (Filter) filter);
                }

                return true;
            }
            case CONTEXT_MENU_INTENT: {
                Intent intent = item.getIntent();
                getActivity().startActivityForResult(intent, REQUEST_CUSTOM_INTENT);
                return true;
            }
            default: {
                TaskListFragment tasklist = (TaskListFragment) getActivity().getSupportFragmentManager().findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);
                if (tasklist != null && tasklist.isInLayout()) {
                    return tasklist.onOptionsItemSelected(item);
                }
            }
        }
        return false;
    }

    public static void showCreateShortcutDialog(final Activity activity, final Intent shortcutIntent,
                                                final Filter filter) {
        FrameLayout frameLayout = new FrameLayout(activity);
        frameLayout.setPadding(10, 0, 10, 0);
        final EditText editText = new EditText(activity);
        if(filter.listingTitle == null) {
            filter.listingTitle = ""; //$NON-NLS-1$
        }
        editText.setText(filter.listingTitle.
                replaceAll("\\(\\d+\\)$", "").trim()); //$NON-NLS-1$ //$NON-NLS-2$
        frameLayout.addView(editText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        final Runnable createShortcut = new Runnable() {
            @Override
            public void run() {
                String label = editText.getText().toString();
                createShortcut(activity, shortcutIntent, label);
            }
        };
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL) {
                    createShortcut.run();
                    return true;
                }
                return false;
            }
        });

        new AlertDialog.Builder(activity)
                .setTitle(R.string.FLA_shortcut_dialog_title)
                .setMessage(R.string.FLA_shortcut_dialog)
                .setView(frameLayout)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createShortcut.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show().setOwnerActivity(activity);
    }

    public static Bitmap superImposeListIcon(Activity activity) {
        return ((BitmapDrawable)activity.getResources().getDrawable(R.drawable.icon)).getBitmap();
    }

    /**
     * Creates a shortcut on the user's home screen
     */
    private static void createShortcut(Activity activity, Intent shortcutIntent, String label) {
        if(label.length() == 0) {
            return;
        }

        Bitmap bitmap = superImposeListIcon(activity);

        Intent createShortcutIntent = new Intent();
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
        createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$

        activity.sendBroadcast(createShortcutIntent);
        Toast.makeText(activity,
                activity.getString(R.string.FLA_toast_onCreateShortcut, label), Toast.LENGTH_LONG).show();
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    public void closeMenu() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
    }

    public void refreshFilterCount() {
        adapter.refreshFilterCount();
    }

    public interface OnFilterItemClickedListener {
        public boolean onFilterItemClicked(FilterListItem item);
    }

    public void clear() {
        adapter.clear();
    }

    public void refresh() {
        adapter.clear();
        adapter.getLists();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Filter item = adapter.getItem(info.position);

        MenuItem menuItem = menu.add(0, CONTEXT_MENU_SHORTCUT, 0, R.string.FLA_context_shortcut);
        menuItem.setIntent(ShortcutActivity.createIntent(item));

        for(int i = 0; i < item.contextMenuLabels.length; i++) {
            if(item.contextMenuIntents.length <= i) {
                break;
            }
            menuItem = menu.add(0, CONTEXT_MENU_INTENT, 0, item.contextMenuLabels[i]);
            menuItem.setIntent(item.contextMenuIntents[i]);
        }

        if(menu.size() > 0) {
            menu.setHeaderTitle(item.listingTitle);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null) {
            adapter.registerRecevier();
        }

        // also load sync actions
        getActivity().registerReceiver(refreshReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
    }

    public void restoreLastSelected() {
        selectItem(mCurrentSelectedPosition);
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
            if(intent == null || !AstridApiConstants.BROADCAST_EVENT_REFRESH.equals(intent.getAction())) {
                return;
            }

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
}
