package org.tasks.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.tasks.calendars.AndroidCalendar;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;

import javax.inject.Inject;

public class CalendarSelectionActivity extends InjectingAppCompatActivity implements CalendarSelectionDialog.CalendarSelectionHandler {

    private static final String FRAG_TAG_CALENDAR_PREFERENCE_SELECTION = "frag_tag_calendar_preference_selection";

    public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
    public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";
    public static final String EXTRA_SHOW_NONE = "extra_show_none";

    @Inject ActivityPermissionRequestor permissionRequestor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (permissionRequestor.requestCalendarPermissions()) {
            showDialog();
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void selectedCalendar(final AndroidCalendar androidCalendar) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_CALENDAR_ID, androidCalendar.getId());
            putExtra(EXTRA_CALENDAR_NAME, androidCalendar.getName());
        }});
    }

    @Override
    public void dismiss() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showDialog();
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showDialog() {
        FragmentManager fragmentManager = getFragmentManager();
        CalendarSelectionDialog fragmentByTag = (CalendarSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
        if (fragmentByTag == null) {
            fragmentByTag = new CalendarSelectionDialog();
            if (getIntent().getBooleanExtra(EXTRA_SHOW_NONE, false)) {
                fragmentByTag.enableNone();
            }
            fragmentByTag.show(fragmentManager, FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
        }
        fragmentByTag.setCalendarSelectionHandler(this);
    }
}
