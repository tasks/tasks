package com.todoroo.astrid.ui;

import java.util.Date;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.DeadlineTimePickerDialog.OnDeadlineTimeSetListener;

/**
 * Control set for specifying when a task should be hidden
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HideUntilControlSet implements TaskEditControlSet,
        OnItemSelectedListener, OnCancelListener,
        OnDeadlineTimeSetListener {

    private static final int SPECIFIC_DATE = -1;
    private static final int EXISTING_TIME_UNSET = -2;

    private final Spinner spinner;
    private int previousSetting = Task.HIDE_UNTIL_NONE;

    private long existingDate = EXISTING_TIME_UNSET;
    private int existingDateHour = EXISTING_TIME_UNSET;
    private int existingDateMinutes = EXISTING_TIME_UNSET;

    private final Activity activity;
    private boolean cancelled = false;

    public HideUntilControlSet(Activity activity, int hideUntil) {
        this.activity = activity;
        this.spinner = (Spinner) activity.findViewById(hideUntil);
        this.spinner.setOnItemSelectedListener(this);
    }

    private ArrayAdapter<HideUntilValue> adapter;

    /**
     * Container class for urgencies
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class HideUntilValue {
        public String label;
        public int setting;
        public long date;

        public HideUntilValue(String label, int setting) {
            this(label, setting, 0);
        }

        public HideUntilValue(String label, int setting, long date) {
            this.label = label;
            this.setting = setting;
            this.date = date;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private HideUntilValue[] createHideUntilList(long specificDate) {
        // set up base values
        String[] labels = activity.getResources().getStringArray(R.array.TEA_hideUntil);
        HideUntilValue[] values = new HideUntilValue[labels.length];
        values[0] = new HideUntilValue(labels[0], Task.HIDE_UNTIL_NONE);
        values[1] = new HideUntilValue(labels[1], Task.HIDE_UNTIL_DUE);
        values[2] = new HideUntilValue(labels[2], Task.HIDE_UNTIL_DAY_BEFORE);
        values[3] = new HideUntilValue(labels[3], Task.HIDE_UNTIL_WEEK_BEFORE);
        values[4] = new HideUntilValue(labels[4], Task.HIDE_UNTIL_SPECIFIC_DAY, -1);

        if(specificDate > 0) {
            HideUntilValue[] updated = new HideUntilValue[values.length + 1];
            for(int i = 0; i < values.length; i++)
                updated[i+1] = values[i];
            Date hideUntilAsDate = new Date(specificDate);
            if(hideUntilAsDate.getHours() == 0 && hideUntilAsDate.getMinutes() == 0 && hideUntilAsDate.getSeconds() == 0) {
                updated[0] = new HideUntilValue(DateUtilities.getDateString(activity, new Date(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY, specificDate);
                existingDate = specificDate;
                existingDateHour = SPECIFIC_DATE;
            } else {
                updated[0] = new HideUntilValue(DateUtilities.getDateStringWithTime(activity, new Date(specificDate)),
                        Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, specificDate);
                existingDate = specificDate;
                existingDateHour = hideUntilAsDate.getHours();
                existingDateMinutes = hideUntilAsDate.getMinutes();
            }
            values = updated;
        }

        return values;
    }

    // --- listening for events

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // if specific date selected, show dialog
        // ... at conclusion of dialog, update our list
        HideUntilValue item = adapter.getItem(position);
        if(item.date == SPECIFIC_DATE) {
            customDate = new Date(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate);
            customDate.setSeconds(0);

            final CalendarDialog calendarDialog = new CalendarDialog(activity, customDate);
            calendarDialog.show();
            calendarDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface arg0) {
                    if (!cancelled) {
                        setDate(calendarDialog);
                    }
                    cancelled = false;
                }
            });

            calendarDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    cancelled = true;
                }
            });

            spinner.setSelection(previousSetting);
        } else {
            previousSetting = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

    Date customDate;

    private void setDate(CalendarDialog calendarDialog) {
        customDate = calendarDialog.getCalendarDate();

        if(existingDateHour < 0) {
            existingDateHour = customDate.getHours();
            existingDateMinutes= customDate.getMinutes();
        }

        DeadlineTimePickerDialog timePicker = new DeadlineTimePickerDialog(activity, this,
                existingDateHour, existingDateMinutes,
                DateUtilities.is24HourFormat(activity), true);

        timePicker.setOnCancelListener(this);
        timePicker.show();
    }

    public void onTimeSet(TimePicker view, boolean hasTime, int hourOfDay, int minute) {
        if(!hasTime) {
            customDate.setHours(0);
            customDate.setMinutes(0);
            customDate.setSeconds(0);
        } else {
            customDate.setHours(hourOfDay);
            customDate.setMinutes(minute);
            existingDateHour = hourOfDay;
            existingDateMinutes = minute;
        }
        customDateFinished();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // user canceled, restore previous choice
        spinner.setSelection(previousSetting);
    }

    private void customDateFinished() {
        HideUntilValue[] list = createHideUntilList(customDate.getTime());
        adapter = new ArrayAdapter<HideUntilValue>(
                activity, android.R.layout.simple_spinner_item,
                list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }

    // --- setting up values

    public void setDefaults() {
        int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                Task.HIDE_UNTIL_NONE);
        spinner.setSelection(setting);
    }

    @Override
    public void readFromTask(Task task) {
        long date = task.getValue(Task.HIDE_UNTIL);

        Date dueDay = new Date(task.getValue(Task.DUE_DATE)/1000L*1000L);

        dueDay.setHours(0);
        dueDay.setMinutes(0);
        dueDay.setSeconds(0);

        // For the hide until due case, we need the time component
        long dueTime = task.hasDueTime() ? task.getValue(Task.DUE_DATE)/1000L*1000L : dueDay.getTime();

        int selection = 0;
        if(date == 0) {
            selection = 0;
            date = 0;
        } else if(date == dueTime) {
            selection = 1;
            date = 0;
        } else if(date + DateUtilities.ONE_DAY == dueDay.getTime()) {
            selection = 2;
            date = 0;
        } else if(date + DateUtilities.ONE_WEEK == dueDay.getTime()) {
            selection = 3;
            date = 0;
        }

        HideUntilValue[] list = createHideUntilList(date);
        adapter = new ArrayAdapter<HideUntilValue>(
                activity, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(selection);
    }

    @Override
    public String writeToModel(Task task) {
        if(adapter == null || spinner == null)
            return null;
        HideUntilValue item = adapter.getItem(spinner.getSelectedItemPosition());
        if(item == null)
            return null;
        long value = task.createHideUntil(item.setting, item.date);
        task.setValue(Task.HIDE_UNTIL, value);
        return null;
    }

}