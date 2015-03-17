/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;
/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */


import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;

import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

/**
 * Displays a preference screen for users to edit their preferences. Override
 * updatePreferences to update the summary with preference values.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TodorooPreferenceActivity extends InjectingPreferenceActivity {

    /**
     * Update preferences for the given preference
     * @param value setting. may be null.
     */
    public abstract void updatePreferences(Preference preference, Object value);

    // --- implementation

    @Inject Preferences preferences;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return preferences.getPrefs();
    }

    private void initializePreference(Preference preference) {
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup)preference;
            for(int i = 0; i < group.getPreferenceCount(); i++) {
                initializePreference(group.getPreference(i));
            }
            updatePreferences(group, null);
        } else {
            Object value = null;
            if(preference instanceof ListPreference) {
                value = ((ListPreference) preference).getValue();
            } else if(preference instanceof CheckBoxPreference) {
                value = ((CheckBoxPreference) preference).isChecked();
            }

            updatePreferences(preference, value);

            if (preference.getOnPreferenceChangeListener() == null) {
                preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference myPreference, Object newValue) {
                        updatePreferences(myPreference, newValue);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            initializePreference(getPreferenceScreen());
        }
    }
}
