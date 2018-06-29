package org.tasks.activities;

import static org.tasks.activities.CalendarSelectionDialog.newCalendarSelectionDialog;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;

public class CalendarSelectionActivity extends ThemedInjectingAppCompatActivity
    implements CalendarSelectionDialog.CalendarSelectionHandler {

  public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
  public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";
  private static final String FRAG_TAG_CALENDAR_PREFERENCE_SELECTION =
      "frag_tag_calendar_preference_selection";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(FRAG_TAG_CALENDAR_PREFERENCE_SELECTION) == null) {
      Intent intent = getIntent();
      newCalendarSelectionDialog(intent.getStringExtra(EXTRA_CALENDAR_NAME))
          .show(fragmentManager, FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
    }
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
  public void finish() {
    super.finish();

    overridePendingTransition(0, 0);
  }
}
