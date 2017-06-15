package org.tasks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import org.tasks.calendars.AndroidCalendar;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import static org.tasks.activities.CalendarSelectionDialog.newCalendarSelectionDialog;

public class CalendarSelectionActivity extends InjectingAppCompatActivity implements CalendarSelectionDialog.CalendarSelectionHandler {

    private static final String FRAG_TAG_CALENDAR_PREFERENCE_SELECTION = "frag_tag_calendar_preference_selection";

    public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
    public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";
    public static final String EXTRA_SHOW_NONE = "extra_show_none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fragmentManager = getSupportFragmentManager();
        CalendarSelectionDialog fragmentByTag = (CalendarSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
        if (fragmentByTag == null) {
            fragmentByTag = newCalendarSelectionDialog(getIntent().getBooleanExtra(EXTRA_SHOW_NONE, false));
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
}
