package org.tasks.activities;

import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.activities.CalendarSelectionDialog.newCalendarSelectionDialog;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import javax.inject.Inject;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;

public class CalendarSelectionActivity extends ThemedInjectingAppCompatActivity implements
    CalendarSelectionDialog.CalendarSelectionHandler {

  public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
  public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";
  private static final String FRAG_TAG_CALENDAR_PREFERENCE_SELECTION = "frag_tag_calendar_preference_selection";
  @Inject ActivityPermissionRequestor permissionRequestor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (permissionRequestor.requestCalendarPermissions()) {
      showDialog();
    }
  }

  private void showDialog() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    CalendarSelectionDialog fragmentByTag = (CalendarSelectionDialog) fragmentManager
        .findFragmentByTag(FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
    if (fragmentByTag == null) {
      Intent intent = getIntent();
      fragmentByTag = newCalendarSelectionDialog(intent.getStringExtra(EXTRA_CALENDAR_NAME));
      fragmentByTag.show(fragmentManager, FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
    }
    fragmentByTag.setCalendarSelectionHandler(this);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void selectedCalendar(final AndroidCalendar androidCalendar) {
    Intent data = new Intent();
    data.putExtra(EXTRA_CALENDAR_ID, androidCalendar.getId());
    data.putExtra(EXTRA_CALENDAR_NAME, androidCalendar.getName());
    setResult(RESULT_OK, data);
    finish();
  }

  @Override
  public void cancel() {
    finish();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
      if (verifyPermissions(grantResults)) {
        showDialog();
      } else {
        finish();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }
}
