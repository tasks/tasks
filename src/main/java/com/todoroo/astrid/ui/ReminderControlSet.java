/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.alarms.AlarmFields;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.DateAndTimePickerDialog;
import org.tasks.dialogs.LocationPickerDialog;
import org.tasks.location.Geofence;
import org.tasks.location.GeofenceService;
import org.tasks.location.LocationApi;
import org.tasks.location.OnLocationPickedHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderControlSet extends TaskEditControlSetBase implements AdapterView.OnItemSelectedListener {

    private static final Logger log = LoggerFactory.getLogger(ReminderControlSet.class);

    private Spinner mode;
    private Spinner addSpinner;
    private TextView modeDisplay;

    private RandomReminderControlSet randomControlSet;
    private LinearLayout alertContainer;
    private boolean whenDue;
    private boolean whenOverdue;
    private LocationApi locationApi;
    private AlarmService alarmService;
    private GeofenceService geofenceService;
    private TaskEditFragment taskEditFragment;
    private List<String> spinnerOptions = new ArrayList<>();
    private ArrayAdapter<String> remindAdapter;


    public ReminderControlSet(LocationApi locationApi, AlarmService alarmService, GeofenceService geofenceService, TaskEditFragment taskEditFragment) {
        super(taskEditFragment.getActivity(), R.layout.control_set_reminders);
        this.locationApi = locationApi;
        this.alarmService = alarmService;
        this.geofenceService = geofenceService;
        this.taskEditFragment = taskEditFragment;
    }

    public int getValue() {
        int value = 0;
        if(whenDue) {
            value |= Task.NOTIFY_AT_DEADLINE;
        }
        if(whenOverdue) {
            value |= Task.NOTIFY_AFTER_DEADLINE;
        }

        value &= ~(Task.NOTIFY_MODE_FIVE | Task.NOTIFY_MODE_NONSTOP);
        if(mode.getSelectedItemPosition() == 2) {
            value |= Task.NOTIFY_MODE_NONSTOP;
        } else if(mode.getSelectedItemPosition() == 1) {
            value |= Task.NOTIFY_MODE_FIVE;
        }

        return value;
    }

    private void addNewAlarm() {
        pickNewAlarm(newDateTime().withMillisOfDay(0), new DateAndTimePickerDialog.OnDateTimePicked() {
            @Override
            public void onDateTimePicked(DateTime dateTime) {
                addAlarmRow(dateTime.getMillis());
            }
        });
    }

    private void pickNewAlarm(DateTime initial, DateAndTimePickerDialog.OnDateTimePicked onDateTimePicked) {
        DateAndTimePickerDialog.dateAndTimePickerDialog(taskEditFragment.getFragmentManager(),
                taskEditFragment.getActivity(), initial, onDateTimePicked, null);
    }

    private void addAlarmRow(final Long timestamp) {
        final View alertItem = addAlarmRow(getDisplayString(timestamp), timestamp, null);
        TextView display = (TextView) alertItem.findViewById(R.id.alarm_string);
        display.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickNewAlarm(newDateTime(timestamp), new DateAndTimePickerDialog.OnDateTimePicked() {
                    @Override
                    public void onDateTimePicked(DateTime dateTime) {
                        long millis = dateTime.getMillis();
                        addAlarmRow(alertItem, getDisplayString(millis), millis, null);
                    }
                });
            }
        });
    }

    public void addGeolocationReminder(final Geofence geofence) {
        View alertItem = addAlarmRow(geofence.getName(), null, new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        alertItem.setTag(geofence);
    }

    private View addAlarmRow(String text, Long timestamp, final OnClickListener onRemove) {
        final View alertItem = LayoutInflater.from(activity).inflate(R.layout.alarm_edit_row, null);
        alertContainer.addView(alertItem);
        addAlarmRow(alertItem, text, timestamp, onRemove);
        return alertItem;
    }

    private View addAlarmRow(final View alertItem, String text, Long timestamp, final View.OnClickListener onRemove) {
        alertItem.setTag(timestamp);
        TextView display = (TextView) alertItem.findViewById(R.id.alarm_string);
        display.setText(text);
        alertItem.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                alertContainer.removeView(alertItem);
                if (onRemove != null) {
                    onRemove.onClick(v);
                }
                updateSpinner();
            }
        });
        updateSpinner();
        return alertItem;
    }

    private void updateSpinner() {
        addSpinner.setSelection(0);
        spinnerOptions.clear();
        spinnerOptions.add("");
        if (!whenDue) {
            spinnerOptions.add(taskEditFragment.getString(R.string.when_due));
        }
        if (!whenOverdue) {
            spinnerOptions.add(taskEditFragment.getString(R.string.when_overdue));
        }
        if (randomControlSet == null) {
            spinnerOptions.add(taskEditFragment.getString(R.string.randomly));
        }
        if (taskEditFragment.getResources().getBoolean(R.bool.location_enabled)) {
            spinnerOptions.add(taskEditFragment.getString(R.string.pick_a_location));
        }
        spinnerOptions.add(taskEditFragment.getString(R.string.pick_a_date_and_time));
        remindAdapter.notifyDataSetChanged();
    }

    @Override
    protected void afterInflate() {
        alertContainer = (LinearLayout) getView().findViewById(R.id.alert_container);
        getView().findViewById(R.id.alarms_add).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (spinnerOptions.size() == 2) {
                    addNewAlarm();
                } else {
                    addSpinner.performClick();
                }
            }
        });
        addSpinner = (Spinner) getView().findViewById(R.id.alarms_add_spinner);
        addSpinner.setOnItemSelectedListener(ReminderControlSet.this);
        remindAdapter = new ArrayAdapter<String>(activity, R.layout.simple_spinner_item, spinnerOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v;

                // If this is the initial dummy entry, make it hidden
                if (position == 0) {
                    TextView tv = new TextView(getContext());
                    tv.setHeight(0);
                    tv.setVisibility(View.GONE);
                    v = tv;
                }
                else {
                    // Pass convertView as null to prevent reuse of special case views
                    v = super.getDropDownView(position, null, parent);
                }

                // Hide scroll bar because it appears sometimes unnecessarily, this does not prevent scrolling
                parent.setVerticalScrollBarEnabled(false);
                return v;
            }
        };
        addSpinner.setAdapter(remindAdapter);
        remindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeDisplay = (TextView) getView().findViewById(R.id.reminder_alarm_display);
        mode = (Spinner) getView().findViewById(R.id.reminder_alarm);
        modeDisplay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mode.performClick();
            }
        });

        String[] list = new String[] {
                activity.getString(R.string.ring_once),
                activity.getString(R.string.ring_five_times),
                activity.getString(R.string.ring_nonstop),
        };
        final ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, list);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                modeDisplay.setText(modeAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
//                TODO Auto-generated method stub

            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mode.setAdapter(modeAdapter);
            }
        });
    }

    @Override
    protected void readFromTaskOnInitialize() {
        setValue(model.getReminderFlags());

        alertContainer.removeAllViews();
        if (whenDue) {
            addDue();
        }
        if (whenOverdue) {
            addOverdue();
        }
        if (model.hasRandomReminder()) {
            addRandomReminder();
        }
        alarmService.getAlarms(model.getId(), new Callback<Metadata>() {
            @Override
            public void apply(Metadata entry) {
                addAlarmRow(entry.getValue(AlarmFields.TIME));
            }
        });
        for (Geofence geofence : geofenceService.getGeofences(model.getId())) {
            addGeolocationReminder(geofence);
        }
        updateSpinner();
    }

    private void addDue() {
        whenDue = true;
        addAlarmRow(taskEditFragment.getString(R.string.when_due), null, new OnClickListener() {
            @Override
            public void onClick(View v) {
                whenDue = false;
            }
        });
    }

    private void addOverdue() {
        whenOverdue = true;
        addAlarmRow(taskEditFragment.getString(R.string.when_overdue), null, new OnClickListener() {
            @Override
            public void onClick(View v) {
                whenOverdue = false;
            }
        });
    }

    private void addRandomReminder() {
        View alarmRow = addAlarmRow(taskEditFragment.getString(R.string.randomly_once), null, new OnClickListener() {
            @Override
            public void onClick(View v) {
                randomControlSet = null;
            }
        });
        randomControlSet = new RandomReminderControlSet(activity, alarmRow);
        randomControlSet.readFromTaskOnInitialize(model);
    }

    private void setValue(int flags) {
        whenDue = (flags & Task.NOTIFY_AT_DEADLINE) > 0;
        whenOverdue = (flags & Task.NOTIFY_AFTER_DEADLINE) > 0;

        if((flags & Task.NOTIFY_MODE_NONSTOP) > 0) {
            mode.setSelection(2);
        } else if((flags & Task.NOTIFY_MODE_FIVE) > 0) {
            mode.setSelection(1);
        } else {
            mode.setSelection(0);
        }
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        task.setReminderFlags(getValue());

        task.setReminderPeriod(randomControlSet == null ? 0L : randomControlSet.getReminderPeriod());

        Set<Long> alarms = new LinkedHashSet<>();
        Set<Geofence> geofences = new LinkedHashSet<>();

        for(int i = 0; i < alertContainer.getChildCount(); i++) {
            Object tag = alertContainer.getChildAt(i).getTag();
            //noinspection StatementWithEmptyBody
            if (tag == null) {
            } else if (tag instanceof Long) {
                alarms.add((Long) tag);
            } else if (tag instanceof Geofence) {
                geofences.add((Geofence) tag);
            } else {
                log.error("Unexpected tag: {}", tag);
            }
        }

        if(alarmService.synchronizeAlarms(task.getId(), alarms)) {
            task.setModificationDate(DateUtilities.now());
        }
        if (geofenceService.synchronizeGeofences(task.getId(), geofences)) {
            task.setModificationDate(DateUtilities.now());
        }
    }

    private String getDisplayString(long forDate) {
        DateTime dateTime = newDateTime(forDate);
        Date d = dateTime.toDate();
        return (dateTime.getYear() == newDateTime().getYear()
                ? DateUtilities.getLongDateStringHideYear(d)
                : DateUtilities.getLongDateString(d)) +
                ", " + DateUtilities.getTimeString(activity, d);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        log.info("onItemSelected({}, {}, {}, {})", parent, view, position, id);
        String selected = spinnerOptions.get(position);
        if (selected.equals(taskEditFragment.getString(R.string.when_due))) {
            addDue();
        } else if(selected.equals(taskEditFragment.getString(R.string.when_overdue))) {
            addOverdue();
        } else if (selected.equals(taskEditFragment.getString(R.string.randomly))) {
            addRandomReminder();
        } else if (selected.equals(taskEditFragment.getString(R.string.pick_a_date_and_time))) {
            addNewAlarm();
        } else if (selected.equals(taskEditFragment.getString(R.string.pick_a_location))) {
            LocationPickerDialog.pickLocation(locationApi, taskEditFragment, new OnLocationPickedHandler() {
                @Override
                public void onLocationPicked(Geofence geofence) {
                    addGeolocationReminder(geofence);
                }
            });
        }
        if (position != 0) {
            updateSpinner();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
