package org.tasks.ui;

import static com.google.common.collect.Iterables.filter;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;
import static org.tasks.LocalBroadcastManager.REFRESH;
import static org.tasks.LocalBroadcastManager.REFRESH_LIST;
import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.todoroo.astrid.adapter.NavigationDrawerAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.filters.FilterProvider;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.injection.FragmentComponent;
import org.tasks.injection.InjectingFragment;
import org.tasks.intents.TaskIntents;

public class NavigationDrawerFragment extends InjectingFragment {

  public static final int FRAGMENT_NAVIGATION_DRAWER = R.id.navigation_drawer;
  public static final int REQUEST_NEW_LIST = 10100;
  public static final int REQUEST_SETTINGS = 10101;
  public static final int REQUEST_PURCHASE = 10102;
  public static final int REQUEST_DONATE = 10103;
  private static final String FRAG_TAG_PURCHASE_DIALOG = "frag_tag_purchase_dialog";

  private final RefreshReceiver refreshReceiver = new RefreshReceiver();
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject NavigationDrawerAdapter adapter;
  @Inject FilterProvider filterProvider;
  @Inject TaskDao taskDao;
  /** A pointer to the current callbacks instance (the Activity). */
  private DrawerLayout mDrawerLayout;

  private RecyclerView recyclerView;
  private View mFragmentContainerView;
  private CompositeDisposable disposables;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      adapter.restore(savedInstanceState);
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    getActivity().setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

    setUpList();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    recyclerView = layout.findViewById(R.id.recycler_view);
    if (atLeastLollipop()) {
      ((ScrimInsetsFrameLayout) layout.findViewById(R.id.scrim_layout))
          .setOnInsetsCallback(insets -> recyclerView.setPadding(0, insets.top, 0, 0));
    }
    return layout;
  }

  private void setUpList() {
    adapter.setOnClick(this::onFilterItemSelected);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(adapter);
  }

  private void onFilterItemSelected(@Nullable FilterListItem item) {
    mDrawerLayout.addDrawerListener(
        new SimpleDrawerListener() {
          @Override
          public void onDrawerClosed(View drawerView) {
            mDrawerLayout.removeDrawerListener(this);
            if (item instanceof Filter) {
              FragmentActivity activity = getActivity();
              if (activity != null) {
                activity.startActivity(TaskIntents.getTaskListIntent(activity, (Filter) item));
              }
            } else if (item instanceof NavigationDrawerAction) {
              NavigationDrawerAction action = (NavigationDrawerAction) item;
              if (action.requestCode == REQUEST_PURCHASE) {
                newPurchaseDialog().show(getFragmentManager(), FRAG_TAG_PURCHASE_DIALOG);
              } else if (action.requestCode == REQUEST_DONATE) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/donate")));
              } else {
                getActivity().startActivityForResult(action.intent, action.requestCode);
              }
            }
          }
        });
    if (item instanceof Filter) {
      ViewModelProviders.of(getActivity()).get(TaskListViewModel.class).setFilter((Filter) item);
    }
    close();
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

    if (preLollipop()) {
      // set a custom shadow that overlays the main content when the drawer opens
      mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    }
  }

  public void setSelected(Filter selected) {
    adapter.setSelected(selected);
  }

  @Override
  public void onPause() {
    super.onPause();

    localBroadcastManager.unregisterReceiver(refreshReceiver);
  }

  @Override
  public void onStart() {
    super.onStart();

    disposables = new CompositeDisposable();
  }

  @Override
  public void onStop() {
    super.onStop();

    disposables.dispose();
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    adapter.save(outState);
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

  @Override
  public void onResume() {
    super.onResume();
    localBroadcastManager.registerRefreshListReceiver(refreshReceiver);

    disposables.add(updateFilters());
  }

  private Disposable updateFilters() {
    return Single.fromCallable(() -> filterProvider.getItems(true))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(adapter::submitList)
        .observeOn(Schedulers.io())
        .map(this::refreshFilterCount)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(adapter::setCounts);
  }

  private Map<Filter, Integer> refreshFilterCount(List<FilterListItem> items) {
    assertNotMainThread();

    Map<Filter, Integer> result = new HashMap<>();
    for (FilterListItem item : filter(items, i -> i instanceof Filter && i.count == -1)) {
      result.put((Filter) item, taskDao.count((Filter) item));
    }
    return result;
  }

  private class RefreshReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent == null) {
        return;
      }
      String action = intent.getAction();
      if (REFRESH.equals(action) || REFRESH_LIST.equals(action)) {
        disposables.add(updateFilters());
      }
    }
  }
}
