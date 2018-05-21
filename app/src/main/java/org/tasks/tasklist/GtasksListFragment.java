package org.tasks.tasklist;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;

public class GtasksListFragment extends TaskListFragment {

  private static final String EXTRA_STORE_OBJECT = "extra_store_object";
  private static final int REQUEST_LIST_SETTINGS = 10101;
  protected GoogleTaskList list;
  @Inject SyncV2Service syncService;

  public static TaskListFragment newGtasksListFragment(GtasksFilter filter, GoogleTaskList list) {
    GtasksListFragment fragment = new GtasksListFragment();
    fragment.filter = filter;
    fragment.list = list;
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      list = savedInstanceState.getParcelable(EXTRA_STORE_OBJECT);
    }
  }

  @Override
  protected void inflateMenu(Toolbar toolbar) {
    super.inflateMenu(toolbar);
    toolbar.inflateMenu(R.menu.menu_gtasks_list_fragment);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_gtasks_list_settings:
        Intent intent = new Intent(getActivity(), GoogleTaskListSettingsActivity.class);
        intent.putExtra(GoogleTaskListSettingsActivity.EXTRA_STORE_DATA, list);
        startActivityForResult(intent, REQUEST_LIST_SETTINGS);
        return true;
      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_LIST_SETTINGS) {
      if (resultCode == RESULT_OK) {
        MainActivity activity = (MainActivity) getActivity();
        String action = data.getAction();
        if (GoogleTaskListSettingsActivity.ACTION_DELETED.equals(action)) {
          activity.onFilterItemClicked(null);
        } else if (GoogleTaskListSettingsActivity.ACTION_RELOAD.equals(action)) {
          activity
              .getIntent()
              .putExtra(
                  MainActivity.OPEN_FILTER,
                  (Filter) data.getParcelableExtra(MainActivity.OPEN_FILTER));
          activity.recreate();
        }
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void clearCompleted() {
    tracker.reportEvent(Tracking.Events.GTASK_CLEAR_COMPLETED);
    syncService.clearCompleted(
        list,
        new SyncResultCallback() {
          @Override
          public void started() {
            setSyncOngoing(true);
          }

          @Override
          public void finished() {
            setSyncOngoing(false);

            onRefresh();
          }
        });
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(EXTRA_STORE_OBJECT, list);
  }

  @Override
  protected boolean hasDraggableOption() {
    return list != null;
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }
}
