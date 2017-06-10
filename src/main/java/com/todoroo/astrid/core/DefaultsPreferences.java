/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;

import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarProvider;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static org.tasks.PermissionUtil.verifyPermissions;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DefaultsPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_CALENDAR_SELECTION = 10412;

    @Inject Preferences preferences;
    @Inject CalendarProvider calendarProvider;
    @Inject ActivityPermissionRequestor permissionRequester;
    private Preference defaultCalendarPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_defaults);

        defaultCalendarPref = findPreference(getString(R.string.gcal_p_default));
        defaultCalendarPref.setOnPreferenceClickListener(preference -> {
            if (permissionRequester.requestCalendarPermissions()) {
                startCalendarSelectionActivity();
            }
            return false;
        });
        setCalendarSummary(preferences.getStringValue(R.string.gcal_p_default));
    }

    private void setCalendarSummary(String calendarId) {
        AndroidCalendar calendar = calendarProvider.getCalendar(calendarId);
        defaultCalendarPref.setSummary(calendar == null
                ? getString(R.string.none)
                : calendar.getName());
    }

    private void startCalendarSelectionActivity() {
        Intent intent = new Intent(DefaultsPreferences.this, CalendarSelectionActivity.class);
        intent.putExtra(CalendarSelectionActivity.EXTRA_SHOW_NONE, true);
        startActivityForResult(intent, REQUEST_CALENDAR_SELECTION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (verifyPermissions(grantResults)) {
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

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
