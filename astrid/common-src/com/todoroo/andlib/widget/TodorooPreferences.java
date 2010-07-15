/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.widget;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.todoroo.andlib.service.DependencyInjectionService;

/**
 * Displays a preference screen for users to edit their preferences. Override
 * updatePreferences to update the summary with preference values.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class TodorooPreferences extends PreferenceActivity {

    // --- abstract methods

    public abstract int getPreferenceResource();

    public abstract void updatePreferences(Preference preference, Object value);

    // --- implementation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(getPreferenceResource());
        DependencyInjectionService.getInstance().inject(this);
    }

    protected void initializePreference(Preference preference) {
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
            else if(preference instanceof RingtonePreference)
                value = getPreferenceManager().getSharedPreferences().getString(preference.getKey(), null);

            if(value != null || Preference.class.equals(preference.getClass()))
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            initializePreference(getPreferenceScreen());
        }
    }

}