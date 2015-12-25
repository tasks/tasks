package org.tasks.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.injection.InjectingFragment;
import org.tasks.location.GeofenceService;
import org.tasks.preferences.AppearancePreferences;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.AlarmSchedulingIntentService;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class NavigationDrawerFragment extends InjectingFragment {

    public static final int FRAGMENT_NAVIGATION_DRAWER = R.id.navigation_drawer;

    public static final String TOKEN_LAST_SELECTED = "lastSelected"; //$NON-NLS-1$

    public static final int REQUEST_NEW_LIST = 4;

    public FilterAdapter adapter = null;

    private final RefreshReceiver refreshReceiver = new RefreshReceiver();

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private OnFilterItemClickedListener mCallbacks;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;

    @Inject FilterCounter filterCounter;
    @Inject FilterProvider filterProvider;
    @Inject GeofenceService geofenceService;
    @Inject Preferences preferences;

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

        getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

        setUpList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FilterAdapter.REQUEST_SETTINGS && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getBooleanExtra(ReminderPreferences.TOGGLE_GEOFENCES, false)) {
                if (preferences.geofencesEnabled()) {
                    geofenceService.setupGeofences();
                } else {
                    geofenceService.cancelGeofences();
                }
            } else if (data.getBooleanExtra(ReminderPreferences.RESET_GEOFENCES, false)) {
                geofenceService.setupGeofences();
            } else if (data.getBooleanExtra(ReminderPreferences.RESCHEDULE_ALARMS, false)) {
                Context context = getContext();
                context.startService(new Intent(context, AlarmSchedulingIntentService.class));
            }

            if (data.getBooleanExtra(AppearancePreferences.FILTERS_CHANGED, false)) {
                refresh();
            }
            if (data.getBooleanExtra(AppearancePreferences.FORCE_REFRESH, false)) {
                getActivity().finish();
                getActivity().startActivity(getActivity().getIntent());
                refresh();
            }
        } else if ((requestCode == NavigationDrawerFragment.REQUEST_NEW_LIST ||
                requestCode == TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER) &&
                resultCode == Activity.RESULT_OK) {
            if(data == null) {
                return;
            }

            Filter newList = data.getParcelableExtra(TagSettingsActivity.TOKEN_NEW_FILTER);
            if (newList != null) {
                getActivity().getIntent().putExtra(TaskListActivity.TOKEN_SWITCH_TO_FILTER, newList);
                clear();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        if (atLeastLollipop()) {
            ((ScrimInsetsFrameLayout) layout.findViewById(R.id.scrim_layout)).setOnInsetsCallback(new ScrimInsetsFrameLayout.OnInsetsCallback() {
                @Override
                public void onInsetsChanged(Rect insets) {
                    mDrawerListView.setPadding(0, insets.top, 0, 0);
                }
            });
        }
        mDrawerListView = (ListView) layout.findViewById(android.R.id.list);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return layout;
    }

    private void setUpList() {
        adapter = new FilterAdapter(filterProvider, filterCounter, getActivity(), mDrawerListView, true);
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
            Timber.e(e, e.getMessage());
        }
    }

    private void selectItem(int position) {
        closeMenu();
        FilterListItem item = adapter.getItem(position);
        if (item instanceof Filter) {
            mCurrentSelectedPosition = position;
            if (mDrawerListView != null) {
                mDrawerListView.setItemChecked(position, true);
            }
            if (mCallbacks != null) {
                mCallbacks.onFilterItemClicked(item);
            }
        } else if (item instanceof NavigationDrawerAction) {
            NavigationDrawerAction action = (NavigationDrawerAction) item;
            if (action.requestCode > 0) {
                startActivityForResult(action.intent, action.requestCode);
            } else {
                startActivity(action.intent);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (OnFilterItemClickedListener) activity;
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

    public void closeMenu() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
    }

    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }
    }

    public void refreshFilterCount() {
        adapter.refreshFilterCount();
    }

    public interface OnFilterItemClickedListener {
        boolean onFilterItemClicked(FilterListItem item);
    }

    public void clear() {
        adapter.clear();
    }

    public void refresh() {
        adapter.populateList();
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
