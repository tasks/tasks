/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;
/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.RingtonePreference;

import com.todoroo.andlib.service.ContextManager;

/**
 * Displays a preference screen for users to edit their preferences. Override
 * updatePreferences to update the summary with preference values.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TodorooPreferenceActivity extends PreferenceActivity {

    // --- abstract methods

    public abstract int getPreferenceResource();

    /**
     * Update preferences for the given preference
     * @param preference
     * @param value setting. may be null.
     */
    public abstract void updatePreferences(Preference preference, Object value);

    // --- implementation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContextManager.setContext(this);
        addPreferencesFromResource(getPreferenceResource());
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return Preferences.getPrefs(this);
    }

    protected void initializePreference(Preference preference) {
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup)preference;
            for(int i = 0; i < group.getPreferenceCount(); i++) {
                initializePreference(group.getPreference(i));
            }
            updatePreferences(group, null);
        } else {
            Object value = null;
            if(preference instanceof ListPreference)
                value = ((ListPreference)preference).getValue();
            else if(preference instanceof CheckBoxPreference)
                value = ((CheckBoxPreference)preference).isChecked();
            else if(preference instanceof EditTextPreference)
                value = ((EditTextPreference)preference).getText();
            else if(preference instanceof RingtonePreference)
                value = getPreferenceManager().getSharedPreferences().getString(preference.getKey(), null);

            updatePreferences(preference, value);

            if (preference.getOnPreferenceChangeListener() == null) {
                preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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