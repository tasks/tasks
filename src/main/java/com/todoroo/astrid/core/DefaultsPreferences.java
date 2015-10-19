/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.gcal.AndroidCalendar;
import com.todoroo.astrid.gcal.GCalHelper;

import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DefaultsPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_CALENDAR_SELECTION = 10412;

    @Inject Preferences preferences;
    @Inject GCalHelper calendarHelper;
    private Preference defaultCalendarPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_defaults);

        defaultCalendarPref = findPreference(getString(R.string.gcal_p_default));
        defaultCalendarPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(DefaultsPreferences.this, CalendarSelectionActivity.class), REQUEST_CALENDAR_SELECTION);
                return false;
            }
        });
        setCalendarSummary(preferences.getStringValue(R.string.gcal_p_default));
    }

    private void setCalendarSummary(String calendarId) {
        List<AndroidCalendar> calendars = calendarHelper.getCalendars();
        for (AndroidCalendar calendar : calendars) {
            if (calendar.getId().equals(calendarId)) {
                defaultCalendarPref.setSummary(calendar.getName());
                return;
            }
        }
        defaultCalendarPref.setSummary(getString(R.string.none));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CALENDAR_SELECTION && resultCode == RESULT_OK) {
            String calendarId = data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_ID);
            preferences.setString(R.string.gcal_p_default, calendarId);
            setCalendarSummary(calendarId);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
