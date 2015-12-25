/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.time.DateTime;
import org.tasks.R;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.DeviceInfo;
import org.tasks.ui.TimePreference;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;
import static com.todoroo.andlib.utility.AndroidUtilities.preJellybean;

public class ReminderPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_QUIET_START = 10001;
    private static final int REQUEST_QUIET_END = 10002;
    private static final int REQUEST_DEFAULT_REMIND = 10003;
    private static final String EXTRA_RESULT = "extra_result";

    public static String RESET_GEOFENCES = "reset_geofences";
    public static String TOGGLE_GEOFENCES = "toggle_geofences";
    public static String RESCHEDULE_ALARMS = "reschedule_alarms";
    private Bundle result;

    @Inject DeviceInfo deviceInfo;
    @Inject PermissionRequestor permissionRequestor;
    @Inject PermissionChecker permissionChecker;

    private CheckBoxPreference fieldMissedCalls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        addPreferencesFromResource(R.xml.preferences_reminders);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preJellybean()) {
            preferenceScreen.removePreference(findPreference(getString(R.string.p_rmd_notif_actions_enabled)));
            preferenceScreen.removePreference(findPreference(getString(R.string.p_notification_priority)));
        }
        if (atLeastMarshmallow()) {
            setExtraOnChange(R.string.p_doze_notifications, RESCHEDULE_ALARMS);
        } else {
            preferenceScreen.removePreference(findPreference(getString(R.string.p_doze_notifications)));
        }

        if (deviceInfo.supportsLocationServices()) {
            setExtraOnChange(R.string.p_geofence_radius, RESET_GEOFENCES);
            setExtraOnChange(R.string.p_geofence_responsiveness, RESET_GEOFENCES);
            setExtraOnChange(R.string.p_geofence_reminders_enabled, TOGGLE_GEOFENCES);
        } else {
            preferenceScreen.removePreference(findPreference(getString(R.string.geolocation_reminders)));
        }

        fieldMissedCalls = (CheckBoxPreference) findPreference(getString(R.string.p_field_missed_calls));
        fieldMissedCalls.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return newValue != null && (!(boolean) newValue || permissionRequestor.requestMissedCallPermissions());
            }
        });
        fieldMissedCalls.setChecked(fieldMissedCalls.isChecked() && permissionChecker.canAccessMissedCallPermissions());

        initializeRingtonePreference();
        initializeTimePreference(getDefaultRemindTimePreference(), REQUEST_DEFAULT_REMIND);
        initializeTimePreference(getQuietStartPreference(), REQUEST_QUIET_START);
        initializeTimePreference(getQuietEndPreference(), REQUEST_QUIET_END);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CONTACTS) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            fieldMissedCalls.setChecked(true);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(EXTRA_RESULT, result);
    }

    private void initializeTimePreference(final TimePreference preference, final int requestCode) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference ignored) {
                final DateTime current = new DateTime().withMillisOfDay(preference.getMillisOfDay());
                startActivityForResult(new Intent(ReminderPreferences.this, TimePickerActivity.class) {{
                    putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.getMillis());
                }}, requestCode);
                return true;
            }
        });
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
                    preference.setSummary(ringtone == null ? "" : ringtone.getTitle(ReminderPreferences.this));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_QUIET_START:
                    getQuietStartPreference().handleTimePickerActivityIntent(data);
                    return;
                case REQUEST_QUIET_END:
                    getQuietEndPreference().handleTimePickerActivityIntent(data);
                    return;
                case REQUEST_DEFAULT_REMIND:
                    getDefaultRemindTimePreference().handleTimePickerActivityIntent(data);
                    return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private TimePreference getQuietStartPreference() {
        return getTimePreference(R.string.p_rmd_quietStart);
    }

    private TimePreference getQuietEndPreference() {
        return getTimePreference(R.string.p_rmd_quietEnd);
    }

    private TimePreference getDefaultRemindTimePreference() {
        return getTimePreference(R.string.p_rmd_time);
    }

    private TimePreference getTimePreference(int resId) {
        return (TimePreference) findPreference(getString(resId));
    }

    private void setExtraOnChange(int resId, final String extra) {
        findPreference(getString(resId)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                result.putBoolean(extra, true);
                setResult(RESULT_OK, new Intent().putExtras(result));
                return true;
            }
        });
    }
}
