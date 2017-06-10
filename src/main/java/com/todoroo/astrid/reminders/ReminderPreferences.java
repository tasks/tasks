/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.Device;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;
import org.tasks.themes.LEDColor;
import org.tasks.themes.ThemeCache;
import org.tasks.time.DateTime;
import org.tasks.ui.TimePreference;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;
import static org.tasks.PermissionUtil.verifyPermissions;

public class ReminderPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_QUIET_START = 10001;
    private static final int REQUEST_QUIET_END = 10002;
    private static final int REQUEST_DEFAULT_REMIND = 10003;
    private static final int REQUEST_LED_PICKER = 10004;

    @Inject Device device;
    @Inject ActivityPermissionRequestor permissionRequestor;
    @Inject PermissionChecker permissionChecker;
    @Inject Preferences preferences;
    @Inject ThemeCache themeCache;

    private CheckBoxPreference fieldMissedCalls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_reminders);

        rescheduleNotificationsOnChange(
                R.string.p_rmd_time,
                R.string.p_rmd_enable_quiet,
                R.string.p_rmd_quietStart,
                R.string.p_rmd_quietEnd);
        resetGeofencesOnChange(
                R.string.p_geofence_radius,
                R.string.p_geofence_responsiveness);

        fieldMissedCalls = (CheckBoxPreference) findPreference(getString(R.string.p_field_missed_calls));
        fieldMissedCalls.setOnPreferenceChangeListener((preference, newValue) -> newValue != null && (!(boolean) newValue || permissionRequestor.requestMissedCallPermissions()));
        fieldMissedCalls.setChecked(fieldMissedCalls.isChecked() && permissionChecker.canAccessMissedCallPermissions());

        initializeRingtonePreference();
        initializeTimePreference(getDefaultRemindTimePreference(), REQUEST_DEFAULT_REMIND);
        initializeTimePreference(getQuietStartPreference(), REQUEST_QUIET_START);
        initializeTimePreference(getQuietEndPreference(), REQUEST_QUIET_END);

        findPreference(getString(R.string.p_led_color)).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(ReminderPreferences.this, ColorPickerActivity.class);
            intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerDialog.ColorPalette.LED);
            startActivityForResult(intent, REQUEST_LED_PICKER);
            return false;
        });

        requires(R.string.notification_shade, atLeastJellybean(), R.string.p_rmd_notif_actions_enabled, R.string.p_notification_priority, R.string.p_rmd_show_description);
        requires(device.supportsLocationServices(), R.string.geolocation_reminders);

        updateLEDColor();
    }

    private void rescheduleNotificationsOnChange(int... resIds) {
        for (int resId : resIds) {
            findPreference(getString(resId)).setOnPreferenceChangeListener((preference, newValue) -> {
                startService(new Intent(ReminderPreferences.this, NotificationSchedulerIntentService.class));
                return true;
            });
        }
    }

    private void resetGeofencesOnChange(int... resIds) {
        for (int resId : resIds) {
            findPreference(getString(resId)).setOnPreferenceChangeListener((preference, newValue) -> {
                startService(new Intent(ReminderPreferences.this, GeofenceSchedulingIntentService.class));
                return true;
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CONTACTS) {
            if (verifyPermissions(grantResults)) {
                fieldMissedCalls.setChecked(true);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initializeTimePreference(final TimePreference preference, final int requestCode) {
        preference.setOnPreferenceClickListener(ignored -> {
            final DateTime current = new DateTime().withMillisOfDay(preference.getMillisOfDay());
            Intent intent = new Intent(ReminderPreferences.this, TimePickerActivity.class);
            intent.putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.getMillis());
            startActivityForResult(intent, requestCode);
            return true;
        });
    }

    private void initializeRingtonePreference() {
        Preference.OnPreferenceChangeListener ringtoneChangedListener = (preference, value) -> {
            if ("".equals(value)) {
                preference.setSummary(R.string.silent);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(ReminderPreferences.this, value == null
                        ? Settings.System.DEFAULT_NOTIFICATION_URI : Uri.parse((String) value));
                preference.setSummary(ringtone == null ? "" : ringtone.getTitle(ReminderPreferences.this));
            }
            return true;
        };

        String ringtoneKey = getString(R.string.p_rmd_ringtone);
        Preference ringtonePreference = findPreference(ringtoneKey);
        ringtonePreference.setOnPreferenceChangeListener(ringtoneChangedListener);
        ringtoneChangedListener.onPreferenceChange(ringtonePreference, PreferenceManager.getDefaultSharedPreferences(this)
                .getString(ringtoneKey, null));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LED_PICKER) {
            if (resultCode == RESULT_OK) {
                preferences.setInt(R.string.p_led_color, data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 4));
                updateLEDColor();
            }
        } else if (requestCode == REQUEST_QUIET_START) {
            if (resultCode == RESULT_OK) {
                getQuietStartPreference().handleTimePickerActivityIntent(data);
            }
        } else if (requestCode == REQUEST_QUIET_END) {
            if (resultCode == RESULT_OK) {
                getQuietEndPreference().handleTimePickerActivityIntent(data);
            }
        } else if (requestCode == REQUEST_DEFAULT_REMIND) {
            if (resultCode == RESULT_OK) {
                getDefaultRemindTimePreference().handleTimePickerActivityIntent(data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void updateLEDColor() {
        int index = preferences.getInt(R.string.p_led_color, 4);
        LEDColor ledColor = themeCache.getLEDColor(index);
        findPreference(getString(R.string.p_led_color)).setSummary(ledColor.getName());
    }
}
