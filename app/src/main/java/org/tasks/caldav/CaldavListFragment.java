package org.tasks.caldav;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.injection.FragmentComponent;

public class CaldavListFragment extends TaskListFragment {

  private static final String EXTRA_CALDAV_CALENDAR = "extra_caldav_calendar";
  private static final int REQUEST_ACCOUNT_SETTINGS = 10101;
  private CaldavCalendar calendar;

  public static TaskListFragment newCaldavListFragment(
      CaldavFilter filter, CaldavCalendar calendar) {
    CaldavListFragment fragment = new CaldavListFragment();
    fragment.filter = filter;
    fragment.calendar = calendar;
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      this.calendar = savedInstanceState.getParcelable(EXTRA_CALDAV_CALENDAR);
    }
  }

  @Override
  protected void inflateMenu(Toolbar toolbar) {
    super.inflateMenu(toolbar);
    toolbar.inflateMenu(R.menu.menu_caldav_list_fragment);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_caldav_list_fragment:
        Intent intent = new Intent(getActivity(), CaldavCalendarSettingsActivity.class);
        intent.putExtra(EXTRA_CALDAV_CALENDAR, calendar);
        startActivityForResult(intent, REQUEST_ACCOUNT_SETTINGS);
        return true;
      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ACCOUNT_SETTINGS) {
      if (resultCode == RESULT_OK) {
        MainActivity activity = (MainActivity) getActivity();
        String action = data.getAction();
        if (CaldavCalendarSettingsActivity.ACTION_DELETED.equals(action)) {
          activity.onFilterItemClicked(null);
        } else if (CaldavCalendarSettingsActivity.ACTION_RELOAD.equals(action)) {
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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(EXTRA_CALDAV_CALENDAR, calendar);
  }

  @Override
  protected boolean hasDraggableOption() {
    return false;
  }

  @Override
  public void inject(FragmentComponent component) {
    component.inject(this);
  }
}
