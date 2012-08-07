/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.res.Resources;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.utility.Constants;

public class LabsPreferences extends TodorooPreferenceActivity {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_labs;
    }

    public static final int PERFORMANCE_SETTING_CHANGED = 3;

    private final OnPreferenceChangeListener settingChangedListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference p, Object newValue) {
            setResult(PERFORMANCE_SETTING_CHANGED);
            updatePreferences(p, newValue);
            return true;
        }
    };

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        String key = preference.getKey();
        if (r.getString(R.string.p_swipe_lists_performance_key).equals(key)) {
            preference.setOnPreferenceChangeListener(settingChangedListener);

            int index = 0;
            if(value instanceof String && !TextUtils.isEmpty((String)value))
                index = AndroidUtilities.indexOf(r.getStringArray(R.array.EPr_swipe_lists_performance_mode_values), (String)value);
            if (index < 0)
                index = 0;

            String name = r.getStringArray(R.array.EPr_swipe_lists_performance_mode)[index];
            String desc = r.getStringArray(R.array.EPr_swipe_lists_performance_desc)[index];
            preference.setSummary(r.getString(R.string.EPr_swipe_lists_display, name, desc));
        } else if (r.getString(R.string.p_field_missed_calls).equals(key)) {
            setEnabledSummary(preference, value,
                    R.string.MCA_missed_calls_pref_desc_enabled, R.string.MCA_missed_calls_pref_desc_disabled);
        } else if (r.getString(R.string.p_use_contact_picker).equals(key)) {
            setEnabledSummary(preference, value,
                    R.string.EPr_use_contact_picker_desc_enabled, R.string.EPr_use_contact_picker_desc_disabled);
        } else if (r.getString(R.string.p_third_party_addons).equals(key)) {
            setEnabledSummary(preference, value,
                    R.string.EPr_third_party_addons_desc_enabled, R.string.EPr_third_party_addons_desc_disabled);
        } else if (r.getString(R.string.p_end_at_deadline).equals(key)){
            setEnabledSummary(preference, value, R.string.EPr_cal_start_at_due_time, R.string.EPr_cal_end_at_due_time);
        } else if (r.getString(R.string.p_ideas_tab_enabled).equals(key)) {
            if (!Constants.MARKET_STRATEGY.allowIdeasTab()) {
                PreferenceScreen screen = getPreferenceScreen();
                screen.removePreference(preference);
            }
        }
    }

    private void setEnabledSummary(Preference preference, Object value, int enabledStr, int disabledStr) {
        if (value != null && (Boolean) value) {
            preference.setSummary(enabledStr);
        } else {
            preference.setSummary(disabledStr);
        }
    }

}
