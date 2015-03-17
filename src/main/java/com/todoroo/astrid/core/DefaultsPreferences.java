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

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.gcal.GCalHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
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
public class DefaultsPreferences extends InjectingPreferenceActivity {

    private static final Logger log = LoggerFactory.getLogger(DefaultsPreferences.class);

    @Inject Preferences preferences;
    @Inject GCalHelper calendarHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_defaults);

        initCalendarsPreference((ListPreference) findPreference(getString(R.string.gcal_p_default)));

        initListPreference(R.string.p_default_urgency_key, R.array.EPr_default_urgency,
                R.array.EPr_default_urgency_values);
        initListPreference(R.string.p_default_importance_key, R.array.EPr_default_importance,
                R.array.EPr_default_importance_values);
        initListPreference(R.string.p_default_hideUntil_key, R.array.EPr_default_hideUntil,
                R.array.EPr_default_hideUntil_values);
        initListPreference(R.string.p_default_reminders_key, R.array.EPr_default_reminders,
                R.array.EPr_default_reminders_values);
        initListPreference(R.string.p_default_reminders_mode_key, R.array.EPr_default_reminders_mode,
                R.array.EPr_default_reminders_mode_values);
        initListPreference(R.string.p_rmd_default_random_hours, R.array.EPr_reminder_random,
                R.array.EPr_reminder_random_hours);
    }

    private void setCalendarSummary(Object value) {
        ListPreference listPreference = (ListPreference) findPreference(getString(R.string.gcal_p_default));
        int index = AndroidUtilities.indexOf(listPreference.getEntryValues(), value);
        String setting = listPreference.getEntries()[index].toString();
        listPreference.setSummary(setting);
    }

    private void initListPreference(int key, final int keyArray, final int valueArray) {
        Preference preference = findPreference(getString(key));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateTaskListPreference(preference, newValue, keyArray, valueArray);
                return true;
            }
        });
        updateTaskListPreference(preference, ((ListPreference) preference).getValue(), keyArray, valueArray);
    }

    private void updateTaskListPreference(Preference preference, Object value, int keyArray, int valueArray) {
        Resources r = getResources();
        int index = AndroidUtilities.indexOf(r.getStringArray(valueArray), value);
        if(index == -1) {
            // force the zeroth index
            index = 0;
            Editor editor = preference.getEditor();
            editor.putString(preference.getKey(), r.getStringArray(valueArray)[0]);
            editor.commit();
        }
        String setting = r.getStringArray(keyArray)[index];
        preference.setSummary(setting);

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
            log.debug("initCalendarsPreference: Unknown calendar.");
            currentSettingIndex = 0;
        }

        listPreference.setValueIndex(currentSettingIndex);
        listPreference.setEnabled(true);

        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setCalendarSummary(newValue);
                return true;
            }
        });
        setCalendarSummary(listPreference.getValue());
    }
}
