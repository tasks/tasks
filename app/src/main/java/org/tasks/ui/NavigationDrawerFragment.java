package org.tasks.ui;

import static android.app.Activity.RESULT_OK;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.preferences.AppearancePreferences;
import timber.log.Timber;

public class NavigationDrawerFragment extends InjectingFragment {

  public static final int FRAGMENT_NAVIGATION_DRAWER = R.id.navigation_drawer;
  public static final int REQUEST_NEW_LIST = 4;
  public static final int ACTIVITY_REQUEST_NEW_FILTER = 5;
  public static final int REQUEST_NEW_GTASK_LIST = 6;
  public static final int REQUEST_NEW_CALDAV_COLLECTION = 7;
  private static final String TOKEN_LAST_SELECTED = "lastSelected"; // $NON-NLS-1$
  private final RefreshReceiver refreshReceiver = new RefreshReceiver();
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject FilterAdapter adapter;
  /** A pointer to the current callbacks instance (the Activity). */
  private OnFilterItemClickedListener mCallbacks;

  private DrawerLayout mDrawerLayout;
  private ListView mDrawerListView;
  private View mFragmentContainerView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      adapter.setSelected(savedInstanceState.getParcelable(TOKEN_LAST_SELECTED));
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
    if (requestCode == FilterAdapter.REQUEST_SETTINGS) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        if (data.getBooleanExtra(AppearancePreferences.EXTRA_RESTART, false)) {
          ((MainActivity) getActivity()).restart();
        }
      }
    } else if (requestCode == FilterAdapter.REQUEST_PURCHASE) {
      if (resultCode == Activity.RESULT_OK) {
        ((MainActivity) getActivity()).restart();
      }
    } else if (requestCode == REQUEST_NEW_LIST
        || requestCode == ACTIVITY_REQUEST_NEW_FILTER
        || requestCode == REQUEST_NEW_GTASK_LIST
        || requestCode == REQUEST_NEW_CALDAV_COLLECTION) {
      if (resultCode == RESULT_OK && data != null) {
        Filter newList = data.getParcelableExtra(MainActivity.OPEN_FILTER);
        if (newList != null) {
          mCallbacks.onFilterItemClicked(newList);
        }
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    if (atLeastLollipop()) {
      ((ScrimInsetsFrameLayout) layout.findViewById(R.id.scrim_layout))
          .setOnInsetsCallback(insets -> mDrawerListView.setPadding(0, insets.top, 0, 0));
    }
    mDrawerListView = layout.findViewById(android.R.id.list);
    mDrawerListView.setOnItemClickListener(
        (parent, view, position, id) -> {
          mDrawerLayout.addDrawerListener(
              new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                  mDrawerLayout.removeDrawerListener(this);
                  selectItem(position);
                }
              });
          close();
        });

    return layout;
  }

  private void setUpList() {
    adapter.setNavigationDrawer();
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

  public void setSelected(Filter selected) {
    adapter.setSelected(selected);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (adapter != null) {
      localBroadcastManager.unregisterReceiver(adapter.getFilterListUpdateReceiver());
    }
    try {
      localBroadcastManager.unregisterReceiver(refreshReceiver);
    } catch (IllegalArgumentException e) {
      // Might not have fully initialized
      Timber.e(e);
    }
  }

  private void selectItem(int position) {
    FilterListItem item = adapter.getItem(position);
    if (item instanceof Filter) {
      if (!item.equals(adapter.getSelected())) {
        adapter.setSelected((Filter) item);
        if (mCallbacks != null) {
          mCallbacks.onFilterItemClicked(item);
        }
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
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(TOKEN_LAST_SELECTED, adapter.getSelected());
  }

  public void closeDrawer() {
    if (mDrawerLayout != null) {
      mDrawerLayout.setDrawerListener(null);
      close();
    }
  }

  private void close() {
    mDrawerLayout.closeDrawer(mFragmentContainerView);
  }

  public void openDrawer() {
    if (mDrawerLayout != null) {
      mDrawerLayout.openDrawer(mFragmentContainerView);
    }
  }

  private void repopulateList() {
    adapter.populateList();
  }

  @Override
  public void onResume() {
    super.onResume();
    localBroadcastManager.registerRefreshReceiver(refreshReceiver);
    localBroadcastManager.registerRefreshListReceiver(refreshReceiver);

    if (adapter != null) {
      localBroadcastManager.registerRefreshReceiver(adapter.getFilterListUpdateReceiver());
      repopulateList();
    }
  }

  public interface OnFilterItemClickedListener {

    void onFilterItemClicked(FilterListItem item);
  }

  /**
   * Receiver which receives refresh intents
   *
   * @author Tim Su <tim@todoroo.com>
   */
  protected class RefreshReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent == null) {
        return;
      }
      String action = intent.getAction();
      if (LocalBroadcastManager.REFRESH.equals(action)) {
        adapter.refreshFilterCount();
      } else if (LocalBroadcastManager.REFRESH_LIST.equals(action)) {
        repopulateList();
      }
    }
  }
}
