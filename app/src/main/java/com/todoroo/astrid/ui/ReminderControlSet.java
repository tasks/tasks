/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.ui;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.todoroo.andlib.utility.DateUtilities.getLongDateStringWithTime;
import static com.todoroo.astrid.data.Task.NO_ID;
import static java.util.Collections.emptyList;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import butterknife.BindView;
import butterknife.OnClick;
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
import org.tasks.data.Alarm;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.locale.Locale;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class ReminderControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_reminders_pref;

  private static final int REQUEST_NEW_ALARM = 12152;

  private static final String EXTRA_FLAGS = "extra_flags";
  private static final String EXTRA_RANDOM_REMINDER = "extra_random_reminder";
  private static final String EXTRA_ALARMS = "extra_alarms";
  private final Set<Long> alarms = new LinkedHashSet<>();
  @Inject AlarmService alarmService;
  @Inject @ForActivity Context context;
  @Inject Locale locale;
  @Inject DialogBuilder dialogBuilder;

  @BindView(R.id.alert_container)
  LinearLayout alertContainer;

  @BindView(R.id.reminder_alarm)
  TextView mode;

  private long taskId;
  private int flags;
  private long randomReminder;
  private int ringMode;
  private RandomReminderControlSet randomControlSet;
  private boolean whenDue;
  private boolean whenOverdue;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    mode.setPaintFlags(mode.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

    taskId = task.getId();
    if (savedInstanceState == null) {
      flags = task.getReminderFlags();
      randomReminder = task.getReminderPeriod();
      setup(currentAlarms());
    } else {
      flags = savedInstanceState.getInt(EXTRA_FLAGS);
      randomReminder = savedInstanceState.getLong(EXTRA_RANDOM_REMINDER);
      setup(Longs.asList(savedInstanceState.getLongArray(EXTRA_ALARMS)));
    }

    return view;
  }

  private List<Long> currentAlarms() {
    return taskId == NO_ID
        ? emptyList()
        : transform(alarmService.getAlarms(taskId), Alarm::getTime);
  }

  @OnClick(R.id.reminder_alarm)
  void onClickRingType() {
    String[] modes = getResources().getStringArray(R.array.reminder_ring_modes);
    dialogBuilder
        .newDialog()
        .setSingleChoiceItems(modes, ringMode, (dialog, which) -> {
          setRingMode(which);
          dialog.dismiss();
        })
        .show();
  }

  private void setRingMode(int ringMode) {
    this.ringMode = ringMode;
    mode.setText(getRingModeString(ringMode));
  }

  private @StringRes int getRingModeString(int ringMode) {
    switch (ringMode) {
      case 2:
        return R.string.ring_nonstop;
      case 1:
        return R.string.ring_five_times;
      default:
        return R.string.ring_once;
    }
  }

  void addAlarm(String selected) {
    if (selected.equals(getString(R.string.when_due))) {
      addDue();
    } else if (selected.equals(getString(R.string.when_overdue))) {
      addOverdue();
    } else if (selected.equals(getString(R.string.randomly))) {
      addRandomReminder(TimeUnit.DAYS.toMillis(14));
    } else if (selected.equals(getString(R.string.pick_a_date_and_time))) {
      addNewAlarm();
    }
  }

  @OnClick(R.id.alarms_add)
  void addAlarm(View view) {
    List<String> options = getOptions();
    if (options.size() == 1) {
      addNewAlarm();
    } else {
      dialogBuilder
          .newDialog()
          .setItems(
              options,
              (dialog, which) -> {
                addAlarm(options.get(which));
                dialog.dismiss();
              })
          .show();
    }
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_reminders;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_outline_notifications_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  private void setup(List<Long> alarms) {
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
  }

  @Override
  public boolean hasChanges(Task original) {
    return getFlags() != original.getReminderFlags()
        || getRandomReminderPeriod() != original.getReminderPeriod()
        || !newHashSet(currentAlarms()).equals(alarms);
  }

  @Override
  public boolean requiresId() {
    return true;
  }

  @Override
  public void apply(Task task) {
    task.setReminderFlags(getFlags());

    task.setReminderPeriod(getRandomReminderPeriod());

    if (alarmService.synchronizeAlarms(task.getId(), alarms)) {
      task.setModificationDate(DateUtilities.now());
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_FLAGS, getFlags());
    outState.putLong(EXTRA_RANDOM_REMINDER, getRandomReminderPeriod());
    outState.putLongArray(EXTRA_ALARMS, Longs.toArray(alarms));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_NEW_ALARM) {
      if (resultCode == Activity.RESULT_OK) {
        addAlarmRow(data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void addAlarmRow(final Long timestamp) {
    if (alarms.add(timestamp)) {
      addAlarmRow(getLongDateStringWithTime(timestamp, locale.getLocale()), v -> alarms.remove(timestamp));
    }
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
        DateAndTimePickerActivity.EXTRA_TIMESTAMP, newDateTime().noon().getMillis());
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
            });
  }

  private List<String> getOptions() {
    List<String> options = new ArrayList<>();
    if (!whenDue) {
      options.add(getString(R.string.when_due));
    }
    if (!whenOverdue) {
      options.add(getString(R.string.when_overdue));
    }
    if (randomControlSet == null) {
      options.add(getString(R.string.randomly));
    }
    options.add(getString(R.string.pick_a_date_and_time));
    return options;
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
      setRingMode(2);
    } else if ((flags & Task.NOTIFY_MODE_FIVE) > 0) {
      setRingMode(1);
    } else {
      setRingMode(0);
    }
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
