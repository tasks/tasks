/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DefaultsPreferences extends TodorooPreferenceActivity {

    @Inject Preferences preferences;
    @Inject GCalHelper calendarHelper;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_defaults;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initCalendarsPreference((ListPreference) findPreference(getString(R.string.gcal_p_default)));
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        // defaults options
        if(r.getString(R.string.p_default_urgency_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_urgency,
                    R.array.EPr_default_urgency_values, R.string.EPr_default_urgency_desc);
        } else if(r.getString(R.string.p_default_importance_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_importance,
                    R.array.EPr_default_importance_values, R.string.EPr_default_importance_desc);
        } else if(r.getString(R.string.p_default_hideUntil_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_hideUntil,
                    R.array.EPr_default_hideUntil_values, R.string.EPr_default_hideUntil_desc);
        } else if(r.getString(R.string.p_default_reminders_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_reminders,
                    R.array.EPr_default_reminders_values, R.string.EPr_default_reminders_desc);
        } else if(r.getString(R.string.p_default_reminders_mode_key).equals(preference.getKey())) {
            updateTaskListPreference(preference, value, r, R.array.EPr_default_reminders_mode,
                    R.array.EPr_default_reminders_mode_values, R.string.EPr_default_reminders_mode_desc);
        } else if(r.getString(R.string.p_rmd_default_random_hours).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_reminder_random_hours), value);
            if(index <= 0) {
                preference.setSummary(r.getString(R.string.rmd_EPr_defaultRemind_desc_disabled));
            } else {
                String setting = r.getStringArray(R.array.EPr_reminder_random)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_defaultRemind_desc, setting));
            }
        } else if(r.getString(R.string.gcal_p_default).equals(preference.getKey())) {
            ListPreference listPreference = (ListPreference) preference;
            int index = AndroidUtilities.indexOf(listPreference.getEntryValues(), value);
            if(index <= 0) {
                preference.setSummary(r.getString(R.string.EPr_default_addtocalendar_desc_disabled));
            } else {
                String setting = listPreference.getEntries()[index].toString();
                preference.setSummary(r.getString(R.string.EPr_default_addtocalendar_desc, setting));
            }
        } else if (r.getString(R.string.p_voiceInputCreatesTask).equals(preference.getKey())) {
            preference.setEnabled(preferences.getBoolean(R.string.p_voiceInputEnabled, false));
            if (value != null && !(Boolean)value) {
                preference.setSummary(R.string.EPr_voiceInputCreatesTask_desc_disabled);
            } else {
                preference.setSummary(R.string.EPr_voiceInputCreatesTask_desc_enabled);
            }
        }
    }

    private void updateTaskListPreference(Preference preference, Object value,
            Resources r, int keyArray, int valueArray, int summaryResource) {
        int index = AndroidUtilities.indexOf(r.getStringArray(valueArray), value);
        if(index == -1) {
            // force the zeroth index
            index = 0;
            Editor editor = preference.getEditor();
            editor.putString(preference.getKey(),
                    r.getStringArray(valueArray)[0]);
            editor.commit();
        }
        String setting = r.getStringArray(keyArray)[index];
        preference.setSummary(r.getString(summaryResource,
                setting));

        // if user changed the value, refresh task defaults
        if(!AndroidUtilities.equals(value, preferences.getStringValue(preference.getKey()))) {
            Editor editor = preferences.getPrefs().edit();
            editor.putString(preference.getKey(), (String)value);
            editor.commit();
        }
    }

    /**
     * Appends all user-modifiable calendars to listPreference.
     *
     * @param listPreference
     *            preference to init
     */
    private void initCalendarsPreference(ListPreference listPreference) {
        Resources r = getResources();
        GCalHelper.CalendarResult calendars = calendarHelper.getCalendars();

        // Fetch the current setting. Invalid calendar id will
        // be changed to default value.
        String currentSetting = preferences.getStringValue(R.string.gcal_p_default);

        int currentSettingIndex = -1;

        ArrayList<CharSequence> entries = new ArrayList<>();
        entries.addAll(Arrays.asList(r.getStringArray(R.array.EPr_default_addtocalendar)));
        entries.addAll(Arrays.asList(calendars.calendars));

        ArrayList<CharSequence> entryValues = new ArrayList<>();
        entryValues.addAll(Arrays.asList(r.getStringArray(R.array.EPr_default_addtocalendar_values)));
        entryValues.addAll(Arrays.asList(calendars.calendarIds));

        listPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
        listPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

        listPreference.setValueIndex(0);
        listPreference.setEnabled(true);

        if (calendars.calendarIds.length == 0 || calendars.calendars.length == 0) {
            // Something went wrong when querying calendars
            // Leave the preference at disabled.
            return;
        }

        // Iterate calendars one by one, and fill up the list preference
        if (currentSetting != null) {
            for (int i=0; i<calendars.calendarIds.length; i++) {
                // We found currently selected calendar
                if (currentSetting.equals(calendars.calendarIds[i])) {
                    currentSettingIndex = i+1; // +1 correction for disabled-entry
                    break;
                }
            }
        }

        if(currentSettingIndex == -1 || currentSettingIndex > calendars.calendarIds.length+1) {
            // Should not happen!
            // Leave the preference at disabled.
            Log.d("astrid", "initCalendarsPreference: Unknown calendar.");
            currentSettingIndex = 0;
        }

        listPreference.setValueIndex(currentSettingIndex);
        listPreference.setEnabled(true);
    }
}
