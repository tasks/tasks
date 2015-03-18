/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.ui.TimePreference;

import java.text.DateFormat;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class ReminderPreferences extends InjectingPreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_reminders);

        initializeRingtonePreference();
        initializeTimePreference(R.string.p_rmd_time, R.string.rmd_EPr_rmd_time_desc);

        if (atLeastLollipop()) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            preferenceScreen.removePreference(findPreference(getString(R.string.p_rmd_enable_quiet)));
            preferenceScreen.removePreference(findPreference(getString(R.string.p_rmd_quietStart)));
            preferenceScreen.removePreference(findPreference(getString(R.string.p_rmd_quietEnd)));
        } else {
            initializeTimePreference(R.string.p_rmd_quietStart, R.string.rmd_EPr_quiet_hours_start_desc);
            initializeTimePreference(R.string.p_rmd_quietEnd, R.string.rmd_EPr_quiet_hours_end_desc);
        }
    }

    private void initializeTimePreference(int key, final int summaryRes) {
        Preference preference = findPreference(getString(key));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPreference(preference, summaryRes, (int) newValue);
                return true;
            }
        });
        setPreference(preference, summaryRes, ((TimePreference) preference).getMillisOfDay());
    }

    private void setPreference(Preference preference, final int summaryRes, int millisOfDay) {
        String setting = DateFormat.getTimeInstance(DateFormat.SHORT).format(new DateTime().withMillisOfDay(millisOfDay).toDate());
        preference.setSummary(getString(summaryRes, setting));
    }

    private void initializeRingtonePreference() {
        Preference.OnPreferenceChangeListener ringtoneChangedListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if ("".equals(value)) {
                    preference.setSummary(R.string.silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(ReminderPreferences.this, Uri.parse((String) (value == null ? "" : value)));
                    String ringtoneTitle = ringtone.getTitle(ReminderPreferences.this);
                    preference.setSummary(ringtoneTitle);
                }
                return true;
            }
        };

        String ringtoneKey = getString(R.string.p_rmd_ringtone);
        Preference ringtonePreference = findPreference(ringtoneKey);
        ringtonePreference.setOnPreferenceChangeListener(ringtoneChangedListener);
        ringtoneChangedListener.onPreferenceChange(ringtonePreference, PreferenceManager.getDefaultSharedPreferences(this)
                .getString(ringtoneKey, null));
    }
}
