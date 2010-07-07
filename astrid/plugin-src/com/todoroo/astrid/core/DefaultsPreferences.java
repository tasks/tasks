/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.widget.TodorooPreferences;
import com.todoroo.astrid.utility.Preferences;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DefaultsPreferences extends TodorooPreferences {

    public int getPreferenceResource() {
        return R.xml.preferences_defaults;
    }

    /**
     *
     * @param resource if null, updates all resources
     */
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
        }
    }

    private void updateTaskListPreference(Preference preference, Object value,
            Resources r, int keyArray, int valueArray, int summaryResource) {
        int index = AndroidUtilities.indexOf(r.getStringArray(valueArray), (String)value);
        String setting = r.getStringArray(keyArray)[index];
        preference.setSummary(r.getString(summaryResource,
                setting));

        // if user changed the value, refresh task defaults
        if(!value.equals(Preferences.getStringValue(preference.getKey()))) {
            Editor editor = Preferences.getPrefs(this).edit();
            editor.putString(preference.getKey(), (String)value);
            editor.commit();
        }
    }

}