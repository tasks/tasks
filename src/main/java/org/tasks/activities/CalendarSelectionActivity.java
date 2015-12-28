package org.tasks.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.gcal.AndroidCalendar;

import org.tasks.injection.InjectingAppCompatActivity;

public class CalendarSelectionActivity extends InjectingAppCompatActivity implements CalendarSelectionDialog.CalendarSelectionHandler {

    private static final String FRAG_TAG_CALENDAR_PREFERENCE_SELECTION = "frag_tag_calendar_preference_selection";

    public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
    public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        CalendarSelectionDialog fragmentByTag = (CalendarSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
        if (fragmentByTag == null) {
            fragmentByTag = new CalendarSelectionDialog();
            fragmentByTag.enableNone();
            fragmentByTag.show(fragmentManager, FRAG_TAG_CALENDAR_PREFERENCE_SELECTION);
        }
        fragmentByTag.setCalendarSelectionHandler(this);
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
}
