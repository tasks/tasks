/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.activities.RemoteListNativePicker.newRemoteListNativePicker;
import static org.tasks.dialogs.NativeSeekBarDialog.newSeekBarDialog;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import androidx.annotation.NonNull;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarProvider;
import org.tasks.dialogs.NativeSeekBarDialog;
import org.tasks.gtasks.RemoteListSelectionHandler;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.Locale;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Device;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;

public class DefaultsPreferences extends InjectingPreferenceActivity
    implements RemoteListSelectionHandler, NativeSeekBarDialog.SeekBarCallback {

  private static final String FRAG_TAG_REMOTE_LIST_SELECTION = "frag_tag_remote_list_selection";
  private static final String FRAG_TAG_RADIUS_PICKER = "frag_tag_radius_picker";

  private static final int REQUEST_CALENDAR_SELECTION = 10412;

  @Inject Preferences preferences;
  @Inject CalendarProvider calendarProvider;
  @Inject ActivityPermissionRequestor permissionRequester;
  @Inject Tracker tracker;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject SyncAdapters syncAdapters;
  @Inject Locale locale;
  @Inject Device device;

  private Preference defaultCalendarPref;
  private Preference defaultRadiusPref;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_defaults);

    defaultCalendarPref = findPreference(getString(R.string.gcal_p_default));
    defaultCalendarPref.setOnPreferenceClickListener(
        preference -> {
          if (permissionRequester.requestCalendarPermissions()) {
            startCalendarSelectionActivity();
          }
          return false;
        });
    String defaultCalendarName = getDefaultCalendarName();
    defaultCalendarPref.setSummary(
        defaultCalendarName == null
            ? getString(R.string.dont_add_to_calendar)
            : defaultCalendarName);

    findPreference(R.string.p_default_remote_list)
        .setOnPreferenceClickListener(
            preference -> {
              newRemoteListNativePicker(defaultFilterProvider.getDefaultRemoteList())
                  .show(getFragmentManager(), FRAG_TAG_REMOTE_LIST_SELECTION);
              return false;
            });
    updateRemoteListSummary();
    defaultRadiusPref = findPreference(R.string.p_default_location_radius);
    defaultRadiusPref.setOnPreferenceClickListener(
        preference -> {
          newSeekBarDialog(
                  R.layout.dialog_radius_seekbar,
                  75,
                  1000,
                  preferences.getInt(R.string.p_default_location_radius, 250),
                  0)
              .show(getFragmentManager(), FRAG_TAG_RADIUS_PICKER);
          return false;
        });
    updateRadius();

    requires(syncAdapters.isSyncEnabled(), R.string.p_default_remote_list);
    requires(device.supportsGeofences(), R.string.p_default_location_reminder_key);
  }

  private void startCalendarSelectionActivity() {
    Intent intent = new Intent(DefaultsPreferences.this, CalendarSelectionActivity.class);
    intent.putExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME, getDefaultCalendarName());
    startActivityForResult(intent, REQUEST_CALENDAR_SELECTION);
  }

  private String getDefaultCalendarName() {
    AndroidCalendar calendar =
        calendarProvider.getCalendar(preferences.getStringValue(R.string.gcal_p_default));
    return calendar == null ? null : calendar.getName();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
      if (verifyPermissions(grantResults)) {
        startCalendarSelectionActivity();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CALENDAR_SELECTION && resultCode == RESULT_OK) {
      preferences.setString(
          R.string.gcal_p_default,
          data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_ID));
      defaultCalendarPref.setSummary(
          data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME));
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void selectedList(Filter list) {
    tracker.reportEvent(Tracking.Events.DEFAULT_REMOTE_LIST);
    if (list == null) {
      preferences.setString(R.string.p_default_remote_list, null);
    } else if (list instanceof GtasksFilter || list instanceof CaldavFilter) {
      defaultFilterProvider.setDefaultRemoteList(list);
    } else {
      throw new RuntimeException("Unhandled filter type");
    }
    updateRemoteListSummary();
  }

  private void updateRemoteListSummary() {
    Filter defaultFilter = defaultFilterProvider.getDefaultRemoteList();
    findPreference(R.string.p_default_remote_list)
        .setSummary(
            defaultFilter == null ? getString(R.string.dont_sync) : defaultFilter.listingTitle);
  }

  private void updateRadius() {
    String radius =
        locale.formatNumber(preferences.getInt(R.string.p_default_location_radius, 250));
    defaultRadiusPref.setSummary(getString(R.string.location_radius_meters, radius));
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void valueSelected(int radius, int requestCode) {
    preferences.setInt(R.string.p_default_location_radius, radius);
    updateRadius();
  }
}
