/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.AndroidUtilities.preOreo;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Device;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.receivers.Badger;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;
import org.tasks.time.DateTime;
import org.tasks.ui.TimePreference;

public class ReminderPreferences extends InjectingPreferenceActivity {

  private static final int REQUEST_QUIET_START = 10001;
  private static final int REQUEST_QUIET_END = 10002;
  private static final int REQUEST_DEFAULT_REMIND = 10003;
  private static final int REQUEST_BADGE_LIST = 10004;

  @Inject Device device;
  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;
  @Inject Badger badger;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject LocalBroadcastManager localBroadcastManager;

  private CheckBoxPreference fieldMissedCalls;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_reminders);

    rescheduleNotificationsOnChange(
        R.string.p_rmd_time,
        R.string.p_rmd_enable_quiet,
        R.string.p_rmd_quietStart,
        R.string.p_rmd_quietEnd,
        R.string.p_rmd_persistent);
    resetGeofencesOnChange(R.string.p_geofence_radius, R.string.p_geofence_responsiveness);

    fieldMissedCalls =
        (CheckBoxPreference) findPreference(getString(R.string.p_field_missed_calls));
    fieldMissedCalls.setOnPreferenceChangeListener(
        (preference, newValue) ->
            newValue != null
                && (!(boolean) newValue || permissionRequestor.requestMissedCallPermissions()));
    fieldMissedCalls.setChecked(
        fieldMissedCalls.isChecked() && permissionChecker.canAccessMissedCallPermissions());

    initializeRingtonePreference();
    initializeTimePreference(getDefaultRemindTimePreference(), REQUEST_DEFAULT_REMIND);
    initializeTimePreference(getQuietStartPreference(), REQUEST_QUIET_START);
    initializeTimePreference(getQuietEndPreference(), REQUEST_QUIET_END);

    findPreference(R.string.notification_channel_settings)
        .setOnPreferenceClickListener(this::openNotificationChannelSettings);
    findPreference(R.string.battery_optimization_settings)
        .setOnPreferenceClickListener(this::openBatteryOptimizationSettings);

    findPreference(R.string.p_bundle_notifications)
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              NotificationSchedulerIntentService.enqueueWork(this, true);
              return true;
            });

    findPreference(R.string.p_badges_enabled)
        .setOnPreferenceChangeListener(
            (preference, newValue) -> {
              if (newValue != null) {
                boolean enabled = (boolean) newValue;
                badger.setEnabled(enabled);
                if (enabled) {
                  showRestartDialog();
                }
                return true;
              }
              return false;
            });

    Preference badgePreference = findPreference(getString(R.string.p_badge_list));
    Filter filter = defaultFilterProvider.getBadgeFilter();
    badgePreference.setSummary(filter.listingTitle);
    badgePreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(ReminderPreferences.this, FilterSelectionActivity.class);
          intent.putExtra(
              FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.getBadgeFilter());
          intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
          startActivityForResult(intent, REQUEST_BADGE_LIST);
          return true;
        });

    requires(device.supportsLocationServices(), R.string.geolocation_reminders);
    requires(atLeastOreo(), R.string.notification_channel_settings);
    requires(atLeastMarshmallow(), R.string.battery_optimization_settings);
    requires(
        preOreo(), R.string.p_rmd_ringtone, R.string.p_rmd_vibrate, R.string.p_led_notification);
    requires(atLeastJellybean(), R.string.p_bundle_notifications);
  }

  @TargetApi(Build.VERSION_CODES.O)
  private boolean openNotificationChannelSettings(Preference ignored) {
    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, ReminderPreferences.this.getPackageName());
    startActivity(intent);
    return true;
  }

  @TargetApi(Build.VERSION_CODES.M)
  private boolean openBatteryOptimizationSettings(Preference ignored) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    startActivity(intent);
    return true;
  }

  private void rescheduleNotificationsOnChange(int... resIds) {
    for (int resId : resIds) {
      findPreference(getString(resId))
          .setOnPreferenceChangeListener(
              (preference, newValue) -> {
                NotificationSchedulerIntentService.enqueueWork(this, false);
                return true;
              });
    }
  }

  private void resetGeofencesOnChange(int... resIds) {
    for (int resId : resIds) {
      findPreference(getString(resId))
          .setOnPreferenceChangeListener(
              (preference, newValue) -> {
                GeofenceSchedulingIntentService.enqueueWork(this);
                return true;
              });
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CONTACTS) {
      if (verifyPermissions(grantResults)) {
        fieldMissedCalls.setChecked(true);
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void initializeTimePreference(final TimePreference preference, final int requestCode) {
    preference.setOnPreferenceClickListener(
        ignored -> {
          final DateTime current = new DateTime().withMillisOfDay(preference.getMillisOfDay());
          Intent intent = new Intent(ReminderPreferences.this, TimePickerActivity.class);
          intent.putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.getMillis());
          startActivityForResult(intent, requestCode);
          return true;
        });
  }

  private void initializeRingtonePreference() {
    Preference.OnPreferenceChangeListener ringtoneChangedListener =
        (preference, value) -> {
          if ("".equals(value)) {
            preference.setSummary(R.string.silent);
          } else {
            Ringtone ringtone =
                RingtoneManager.getRingtone(
                    ReminderPreferences.this,
                    value == null
                        ? Settings.System.DEFAULT_NOTIFICATION_URI
                        : Uri.parse((String) value));
            preference.setSummary(
                ringtone == null ? "" : ringtone.getTitle(ReminderPreferences.this));
          }
          return true;
        };

    String ringtoneKey = getString(R.string.p_rmd_ringtone);
    Preference ringtonePreference = findPreference(ringtoneKey);
    ringtonePreference.setOnPreferenceChangeListener(ringtoneChangedListener);
    ringtoneChangedListener.onPreferenceChange(
        ringtonePreference,
        PreferenceManager.getDefaultSharedPreferences(this).getString(ringtoneKey, null));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_QUIET_START) {
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
    } else if (requestCode == REQUEST_BADGE_LIST) {
      if (resultCode == RESULT_OK) {
        Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        defaultFilterProvider.setBadgeFilter(filter);
        findPreference(getString(R.string.p_badge_list)).setSummary(filter.listingTitle);
        localBroadcastManager.broadcastRefresh();
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
}
