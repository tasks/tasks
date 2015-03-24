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

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.ui.TimePreference;

import java.text.DateFormat;

import static com.todoroo.andlib.utility.AndroidUtilities.preJellybean;

public class ReminderPreferences extends InjectingPreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_reminders);

        if (preJellybean()) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.p_rmd_notif_actions_enabled)));
        }

        initializeRingtonePreference();
        initializeTimePreference(R.string.p_rmd_time, R.string.rmd_EPr_rmd_time_desc);
        initializeTimePreference(R.string.p_rmd_quietStart, null);
        initializeTimePreference(R.string.p_rmd_quietEnd, null);
    }

    private void initializeTimePreference(int key, final Integer summaryRes) {
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

    private void setPreference(Preference preference, final Integer summaryRes, int millisOfDay) {
        String setting = DateFormat.getTimeInstance(DateFormat.SHORT).format(new DateTime().withMillisOfDay(millisOfDay).toDate());
        preference.setSummary(summaryRes == null ? setting : getString(summaryRes, setting));
    }

    private void initializeRingtonePreference() {
        Preference.OnPreferenceChangeListener ringtoneChangedListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if ("".equals(value)) {
                    preference.setSummary(R.string.silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(ReminderPreferences.this, value == null
                            ? RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION)
                            : Uri.parse((String) value));
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
