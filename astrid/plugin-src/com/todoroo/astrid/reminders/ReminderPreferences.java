/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

import com.timsu.astrid.R;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_reminders);

        PreferenceScreen screen = getPreferenceScreen();
        initializePreference(screen);

    }

    private void initializePreference(Preference preference) {
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup)preference;
            for(int i = 0; i < group.getPreferenceCount(); i++) {
                initializePreference(group.getPreference(i));
            }
        } else {
            Object value = null;
            if(preference instanceof ListPreference)
                value = ((ListPreference)preference).getValue();
            else if(preference instanceof CheckBoxPreference)
                value = ((CheckBoxPreference)preference).isChecked();
            else if(preference instanceof EditTextPreference)
                value = ((EditTextPreference)preference).getText();

            if(value != null)
                updatePreferences(preference, value);

            preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference myPreference, Object newValue) {
                    updatePreferences(myPreference, newValue);
                    return true;
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected int valueToIndex(String value, String[] array) {
        for(int i = 0; i < array.length; i++)
            if(array[i].equals(value))
                return i;
        return -1;
    }

    /**
     *
     * @param resource if null, updates all resources
     */
    protected void updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        if(r.getString(R.string.p_rmd_quietStart).equals(preference.getKey())) {
            int index = valueToIndex((String)value, r.getStringArray(R.array.EPr_quiet_hours_start_values));
            if(index == -1)
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
            else {
                String duration = r.getStringArray(R.array.EPr_quiet_hours_start)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_start_desc, duration));
            }
        } else if(r.getString(R.string.p_rmd_quietEnd).equals(preference.getKey())) {
            int index = valueToIndex((String)value, r.getStringArray(R.array.EPr_quiet_hours_end_values));
            if(index == -1)
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
            else {
                String duration = r.getStringArray(R.array.EPr_quiet_hours_end)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_end_desc, duration));
            }
        } else if(r.getString(R.string.p_rmd_ringtone).equals(preference.getKey())) {
            if(value == null)
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_default));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_custom));
        } else if(r.getString(R.string.p_rmd_persistent).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_false));
        }

    }

}