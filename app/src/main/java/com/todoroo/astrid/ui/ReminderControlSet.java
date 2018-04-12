/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.todoroo.andlib.utility.DateUtilities.getLongDateStringWithTime;
import static com.todoroo.astrid.data.Task.NO_ID;
import static java.util.Collections.emptyList;
import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import com.google.common.primitives.Longs;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.data.Alarm;
import org.tasks.data.Location;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.location.GeofenceService;
import org.tasks.location.PlacePicker;
import org.tasks.preferences.Device;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import org.tasks.ui.HiddenTopArrayAdapter;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class ReminderControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_reminders_pref;

  private static final int REQUEST_NEW_ALARM = 12152;
  private static final int REQUEST_LOCATION_REMINDER = 12153;

  private static final String EXTRA_FLAGS = "extra_flags";
  private static final String EXTRA_RANDOM_REMINDER = "extra_random_reminder";
  private static final String EXTRA_ALARMS = "extra_alarms";
  private static final String EXTRA_GEOFENCES = "extra_geofences";
  private final List<String> spinnerOptions = new ArrayList<>();
  private final Set<Long> alarms = new LinkedHashSet<>();
  private final Set<Location> locations = new LinkedHashSet<>();
  @Inject AlarmService alarmService;
  @Inject GeofenceService geofenceService;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject Device device;
  @Inject Preferences preferences;
  @Inject @ForActivity Context context;

  @BindView(R.id.alert_container)
  LinearLayout alertContainer;

  @BindView(R.id.reminder_alarm)
  Spinner mode;

  @BindView(R.id.alarms_add_spinner)
  Spinner addSpinner;

  private long taskId;
  private int flags;
  private long randomReminder;
  private int ringMode;
  private RandomReminderControlSet randomControlSet;
  private boolean whenDue;
  private boolean whenOverdue;
  private ArrayAdapter<String> remindAdapter;

  @OnItemSelected(R.id.reminder_alarm)
  void ringModeSelected(int position) {
    ringMode = position;
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    remindAdapter =
        new HiddenTopArrayAdapter(context, android.R.layout.simple_spinner_item, spinnerOptions);
    String[] modes = getResources().getStringArray(R.array.reminder_ring_modes);
    ArrayAdapter<String> modeAdapter =
        new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, modes);
    modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mode.setAdapter(modeAdapter);

    taskId = task.getId();
    if (savedInstanceState == null) {
      flags = task.getReminderFlags();
      randomReminder = task.getReminderPeriod();
      setup(currentAlarms(), geofenceService.getGeofences(taskId));
    } else {
      flags = savedInstanceState.getInt(EXTRA_FLAGS);
      randomReminder = savedInstanceState.getLong(EXTRA_RANDOM_REMINDER);
      List<Location> locations = new ArrayList<>();
      List<Parcelable> geofenceArray = savedInstanceState.getParcelableArrayList(EXTRA_GEOFENCES);
      for (Parcelable geofence : geofenceArray) {
        locations.add((Location) geofence);
      }
      setup(Longs.asList(savedInstanceState.getLongArray(EXTRA_ALARMS)), locations);
    }

    addSpinner.setAdapter(remindAdapter);

    return view;
  }

  private List<Long> currentAlarms() {
    return taskId == NO_ID
        ? emptyList()
        : transform(alarmService.getAlarms(taskId), Alarm::getTime);
  }

  @OnItemSelected(R.id.alarms_add_spinner)
  void addAlarm(int position) {
    String selected = spinnerOptions.get(position);
    if (selected.equals(getString(R.string.when_due))) {
      addDue();
    } else if (selected.equals(getString(R.string.when_overdue))) {
      addOverdue();
    } else if (selected.equals(getString(R.string.randomly))) {
      addRandomReminder(TimeUnit.DAYS.toMillis(14));
    } else if (selected.equals(getString(R.string.pick_a_date_and_time))) {
      addNewAlarm();
    } else if (selected.equals(getString(R.string.pick_a_location))) {
      if (permissionRequestor.requestFineLocation()) {
        pickLocation();
      }
    }
    if (position != 0) {
      updateSpinner();
    }
  }

  @OnClick(R.id.alarms_add)
  void addAlarm(View view) {
    if (spinnerOptions.size() == 2) {
      addNewAlarm();
    } else {
      addSpinner.performClick();
    }
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_reminders;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_notifications_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  private void setup(List<Long> alarms, List<Location> locations) {
    setValue(flags);

    alertContainer.removeAllViews();
    if (whenDue) {
      addDue();
    }
    if (whenOverdue) {
      addOverdue();
    }
    if (randomReminder > 0) {
      addRandomReminder(randomReminder);
    }
    for (long timestamp : alarms) {
      addAlarmRow(timestamp);
    }
    for (Location location : locations) {
      addGeolocationReminder(location);
    }
    updateSpinner();
  }

  @Override
  public boolean hasChanges(Task original) {
    return getFlags() != original.getReminderFlags()
        || getRandomReminderPeriod() != original.getReminderPeriod()
        || !newHashSet(currentAlarms()).equals(alarms)
        || !newHashSet(geofenceService.getGeofences(taskId)).equals(locations);
  }

  @Override
  public void apply(Task task) {
    task.setReminderFlags(getFlags());

    task.setReminderPeriod(getRandomReminderPeriod());

    if (alarmService.synchronizeAlarms(task.getId(), alarms)) {
      task.setModificationDate(DateUtilities.now());
    }
    if (geofenceService.synchronizeGeofences(task.getId(), locations)) {
      task.setModificationDate(DateUtilities.now());
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_FLAGS, getFlags());
    outState.putLong(EXTRA_RANDOM_REMINDER, getRandomReminderPeriod());
    outState.putLongArray(EXTRA_ALARMS, Longs.toArray(alarms));
    outState.putParcelableArrayList(EXTRA_GEOFENCES, newArrayList(locations));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_NEW_ALARM) {
      if (resultCode == Activity.RESULT_OK) {
        addAlarmRow(data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L));
      }
    } else if (requestCode == REQUEST_LOCATION_REMINDER) {
      if (resultCode == Activity.RESULT_OK) {
        addGeolocationReminder(PlacePicker.getPlace(context, data, preferences));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_LOCATION) {
      if (verifyPermissions(grantResults)) {
        pickLocation();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void addAlarmRow(final Long timestamp) {
    addAlarmRow(getLongDateStringWithTime(context, timestamp), v -> alarms.remove(timestamp));
    alarms.add(timestamp);
  }

  private void pickLocation() {
    Intent intent = PlacePicker.getIntent(getActivity());
    if (intent != null) {
      startActivityForResult(intent, REQUEST_LOCATION_REMINDER);
    }
  }

  private void addGeolocationReminder(final Location location) {
    addAlarmRow(location.getName(), v -> locations.remove(location));
    locations.add(location);
  }

  private int getFlags() {
    int value = 0;
    if (whenDue) {
      value |= Task.NOTIFY_AT_DEADLINE;
    }
    if (whenOverdue) {
      value |= Task.NOTIFY_AFTER_DEADLINE;
    }

    value &= ~(Task.NOTIFY_MODE_FIVE | Task.NOTIFY_MODE_NONSTOP);
    if (ringMode == 2) {
      value |= Task.NOTIFY_MODE_NONSTOP;
    } else if (ringMode == 1) {
      value |= Task.NOTIFY_MODE_FIVE;
    }

    return value;
  }

  private long getRandomReminderPeriod() {
    return randomControlSet == null ? 0L : randomControlSet.getReminderPeriod();
  }

  private void addNewAlarm() {
    Intent intent = new Intent(getActivity(), DateAndTimePickerActivity.class);
    intent.putExtra(
        DateAndTimePickerActivity.EXTRA_TIMESTAMP, newDateTime().startOfDay().getMillis());
    startActivityForResult(intent, REQUEST_NEW_ALARM);
  }

  private View addAlarmRow(String text, final OnClickListener onRemove) {
    final View alertItem = getActivity().getLayoutInflater().inflate(R.layout.alarm_edit_row, null);
    alertContainer.addView(alertItem);
    addAlarmRow(alertItem, text, onRemove);
    return alertItem;
  }

  private void addAlarmRow(final View alertItem, String text, final View.OnClickListener onRemove) {
    TextView display = alertItem.findViewById(R.id.alarm_string);
    display.setText(text);
    alertItem
        .findViewById(R.id.clear)
        .setOnClickListener(
            v -> {
              alertContainer.removeView(alertItem);
              if (onRemove != null) {
                onRemove.onClick(v);
              }
              updateSpinner();
            });
    updateSpinner();
  }

  private void updateSpinner() {
    addSpinner.setSelection(0);
    spinnerOptions.clear();
    spinnerOptions.add("");
    if (!whenDue) {
      spinnerOptions.add(getString(R.string.when_due));
    }
    if (!whenOverdue) {
      spinnerOptions.add(getString(R.string.when_overdue));
    }
    if (randomControlSet == null) {
      spinnerOptions.add(getString(R.string.randomly));
    }
    if (device.supportsLocationServices()) {
      spinnerOptions.add(getString(R.string.pick_a_location));
    }
    spinnerOptions.add(getString(R.string.pick_a_date_and_time));
    remindAdapter.notifyDataSetChanged();
  }

  private void addDue() {
    whenDue = true;
    addAlarmRow(getString(R.string.when_due), v -> whenDue = false);
  }

  private void addOverdue() {
    whenOverdue = true;
    addAlarmRow(getString(R.string.when_overdue), v -> whenOverdue = false);
  }

  private void addRandomReminder(long reminderPeriod) {
    View alarmRow =
        addAlarmRow(getString(R.string.randomly_once) + " ", v -> randomControlSet = null);
    randomControlSet = new RandomReminderControlSet(context, alarmRow, reminderPeriod);
  }

  private void setValue(int flags) {
    whenDue = (flags & Task.NOTIFY_AT_DEADLINE) > 0;
    whenOverdue = (flags & Task.NOTIFY_AFTER_DEADLINE) > 0;

    if ((flags & Task.NOTIFY_MODE_NONSTOP) > 0) {
      mode.setSelection(2);
    } else if ((flags & Task.NOTIFY_MODE_FIVE) > 0) {
      mode.setSelection(1);
    } else {
      mode.setSelection(0);
    }
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
