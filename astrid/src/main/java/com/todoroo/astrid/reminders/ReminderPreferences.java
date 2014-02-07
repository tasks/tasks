/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.res.Resources;
import android.preference.Preference;

import com.todoroo.andlib.utility.TodorooPreferenceActivity;

import org.joda.time.DateTime;
import org.tasks.R;

import java.text.DateFormat;

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

    @Override
    public void updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        if(r.getString(R.string.p_rmd_enable_quiet).equals(preference.getKey())) {
            if( !(Boolean) value) {
                preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_desc_none));
            } else {
                preference.setSummary("");
            }
        } else if(r.getString(R.string.p_rmd_quietStart).equals(preference.getKey())) {
            int millisOfDay = (int) value;
            String setting = DateFormat.getTimeInstance(DateFormat.SHORT).format(new DateTime().withMillisOfDay(millisOfDay).toDate());
            preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_start_desc, setting));
        } else if(r.getString(R.string.p_rmd_quietEnd).equals(preference.getKey())) {
            int millisOfDay = (int) value;
            String setting = DateFormat.getTimeInstance(DateFormat.SHORT).format(new DateTime().withMillisOfDay(millisOfDay).toDate());
            preference.setSummary(r.getString(R.string.rmd_EPr_quiet_hours_end_desc, setting));
        } else if(r.getString(R.string.p_rmd_time).equals(preference.getKey())) {
            int millisOfDay = (int) value;
            String setting = DateFormat.getTimeInstance(DateFormat.SHORT).format(new DateTime().withMillisOfDay(millisOfDay).toDate());
            preference.setSummary(r.getString(R.string.rmd_EPr_rmd_time_desc, setting));
        } else if(r.getString(R.string.p_rmd_ringtone).equals(preference.getKey())) {
            if(value == null || "content://settings/system/notification_sound".equals(value)) //$NON-NLS-1$
            {
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_default));
            } else if("".equals(value)) //$NON-NLS-1$
            {
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_silent));
            } else {
                preference.setSummary(r.getString(R.string.rmd_EPr_ringtone_desc_custom));
            }
        } else if(r.getString(R.string.p_rmd_persistent).equals(preference.getKey())) {
            if((Boolean)value) {
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_true));
            } else {
                preference.setSummary(r.getString(R.string.rmd_EPr_persistent_desc_false));
            }
        } else if(r.getString(R.string.p_rmd_maxvolume).equals(preference.getKey())) {
            if((Boolean)value) {
                preference.setSummary(r.getString(R.string.rmd_EPr_multiple_maxvolume_desc_true));
            } else {
                preference.setSummary(r.getString(R.string.rmd_EPr_multiple_maxvolume_desc_false));
            }
        } else if(r.getString(R.string.p_rmd_vibrate).equals(preference.getKey())) {
            if((Boolean)value) {
                preference.setSummary(r.getString(R.string.rmd_EPr_vibrate_desc_true));
            } else {
                preference.setSummary(r.getString(R.string.rmd_EPr_vibrate_desc_false));
            }
        } else if(r.getString(R.string.p_rmd_snooze_dialog).equals(preference.getKey())) {
            if(value == null || ((Boolean) value)) {
                preference.setSummary(r.getString(R.string.rmd_EPr_snooze_dialog_desc_true));
            } else {
                preference.setSummary(r.getString(R.string.rmd_EPr_snooze_dialog_desc_false));
            }
        } else if (r.getString(R.string.p_rmd_enabled).equals(preference.getKey())) {
            if( (Boolean)value ) {
                preference.setSummary(R.string.rmd_EPr_enabled_desc_true);
            } else {
                preference.setSummary(R.string.rmd_EPr_enabled_desc_false);
            }
        }
    }
}
