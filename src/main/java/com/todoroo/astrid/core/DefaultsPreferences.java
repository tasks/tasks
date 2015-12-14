/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.gcal.AndroidCalendar;
import com.todoroo.astrid.gcal.GCalHelper;

import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
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
    @Inject PermissionChecker permissionChecker;
    @Inject PermissionRequestor permissionRequester;
    private Preference defaultCalendarPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_defaults);

        defaultCalendarPref = findPreference(getString(R.string.gcal_p_default));
        defaultCalendarPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (permissionRequester.requestCalendarPermissions()) {
                    startCalendarSelectionActivity();
                }
                return false;
            }
        });
        setCalendarSummary(preferences.getStringValue(R.string.gcal_p_default));
    }

    private void setCalendarSummary(String calendarId) {
        if (permissionChecker.canAccessCalendars()) {
            List<AndroidCalendar> calendars = calendarHelper.getCalendars();
            for (AndroidCalendar calendar : calendars) {
                if (calendar.getId().equals(calendarId)) {
                    defaultCalendarPref.setSummary(calendar.getName());
                    return;
                }
            }
        }
        defaultCalendarPref.setSummary(getString(R.string.none));
    }

    private void startCalendarSelectionActivity() {
        startActivityForResult(new Intent(DefaultsPreferences.this, CalendarSelectionActivity.class), REQUEST_CALENDAR_SELECTION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCalendarSelectionActivity();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
