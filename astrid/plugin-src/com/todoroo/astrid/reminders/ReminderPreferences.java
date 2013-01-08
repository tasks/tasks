/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderPreferences extends TodorooPreferenceActivity {

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
            Preference endPreference = findPreference(getString(R.string.p_rmd_quietEnd));
            if(index <= 0) {
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
                endPreference.setEnabled(false);
            } else {
                String setting = r.getStringArray(R.array.EPr_quiet_hours_start)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_start_desc, setting));
                endPreference.setEnabled(true);
            }
        } else if(r.getString(R.string.p_rmd_quietEnd).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_quiet_hours_end_values), (String)value);
            int quietHoursStart = Preferences.getIntegerFromString(R.string.p_rmd_quietStart, -1);
            if(index == -1 || quietHoursStart == -1)
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
            else {
                String setting = r.getStringArray(R.array.EPr_quiet_hours_end)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_end_desc, setting));
            }
        } else if(r.getString(R.string.p_rmd_time).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_rmd_time_values), (String)value);
            if (index != -1 && index < r.getStringArray(R.array.EPr_rmd_time).length) {
                // FIXME this does not fix the underlying cause of the ArrayIndexOutofBoundsException
                // https://www.crittercism.com/developers/crash-details/e0886dbfcf9e78a21d9f2e2a385c4c13e2f6ad2132ac24a3fa811144
                String setting = r.getStringArray(R.array.EPr_rmd_time)[index];
                preference.setSummary(r.getString(R.string.rmd_EPr_rmd_time_desc, setting));
            }
        } else if(r.getString(R.string.p_rmd_ringtone).equals(preference.getKey())) {
            if(value == null || "content://settings/system/notification_sound".equals(value)) //$NON-NLS-1$
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_default));
            else if("".equals(value)) //$NON-NLS-1$
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_silent));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_custom));
        } else if(r.getString(R.string.p_rmd_persistent).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_false));
        } else if(r.getString(R.string.p_rmd_maxvolume).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(r.getString(R.string.rmd_EPr_multiple_maxvolume_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_multiple_maxvolume_desc_false));
        } else if(r.getString(R.string.p_rmd_vibrate).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(r.getString(R.string.rmd_EPr_vibrate_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_vibrate_desc_false));
        } else if(r.getString(R.string.p_rmd_nagging).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(r.getString(R.string.rmd_EPr_nagging_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_nagging_desc_false));
        } else if(r.getString(R.string.p_rmd_snooze_dialog).equals(preference.getKey())) {
            if(value == null || ((Boolean)value) == true)
                preference.setSummary(r.getString(R.string.rmd_EPr_snooze_dialog_desc_true));
            else
                preference.setSummary(r.getString(R.string.rmd_EPr_snooze_dialog_desc_false));
        } else if (r.getString(R.string.p_rmd_enabled).equals(preference.getKey())) {
            if((Boolean)value)
                preference.setSummary(R.string.rmd_EPr_enabled_desc_true);
            else
                preference.setSummary(R.string.rmd_EPr_enabled_desc_false);
        } else if (r.getString(R.string.p_rmd_social).equals(preference.getKey())) {
            if ((Boolean)value)
                preference.setSummary(R.string.rmd_EPr_social_summary_enabled);
            else
                preference.setSummary(R.string.rmd_EPr_social_summary_disabled);
        }

    }

}
