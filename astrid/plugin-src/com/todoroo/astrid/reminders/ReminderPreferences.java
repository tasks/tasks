/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.widget.TodorooPreferences;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderPreferences extends TodorooPreferences {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_reminders;
    }

    /**
     *
     * @param resource if null, updates all resources
     */
    @Override
    public void updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        if(r.getString(R.string.p_rmd_quietStart).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_quiet_hours_start_values), (String)value);
            if(index == -1)
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
            else {
                String duration = r.getStringArray(R.array.EPr_quiet_hours_start)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_start_desc, duration));
            }
        } else if(r.getString(R.string.p_rmd_quietEnd).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_quiet_hours_end_values), (String)value);
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