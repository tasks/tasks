package com.todoroo.astrid.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TimePicker;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.DeadlineTimePickerDialog.OnDeadlineTimeSetListener;

public class UrgencyControlSet implements TaskEditControlSet,
        OnDeadlineTimeSetListener {

    private static final int SPECIFIC_DATE = -1;

    private final Button dateButton;
    private final Button timeButton;
    private ArrayAdapter<UrgencyValue> urgencyAdapter;

    private final Activity activity;

    private long dueDateValue = 0;
    private long dueTimeValue = 0;

    /**
     * Container class for urgencies
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    private class UrgencyValue {
        public String label;
        public int setting;
        public long dueDate;

        public UrgencyValue(String label, int setting) {
            this.label = label;
            this.setting = setting;
            dueDate = Task.createDueDate(setting, 0);
        }

        public UrgencyValue(String label, int setting, long dueDate) {
            this.label = label;
            this.setting = setting;
            this.dueDate = dueDate;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public UrgencyControlSet(Activity activity, int date, int time) {
        this.activity = activity;
        this.dateButton = (Button)activity.findViewById(date);
        this.timeButton = (Button)activity.findViewById(time);

        dateButton.setOnClickListener(dateButtonClick);

        timeButton.setOnClickListener(timeButtonClick);
    }

    // --- events

    View.OnClickListener dateButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showUrgencySpinner(dueDateValue);
        }
    };

    View.OnClickListener timeButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean hasTime = Task.hasDueTime(dueTimeValue);
            int hour = 18, minute = 0;
            if(hasTime) {
                Date date = new Date(dueTimeValue);
                hour = date.getHours();
                minute = date.getMinutes();
            }

            DeadlineTimePickerDialog timePicker = new DeadlineTimePickerDialog(activity,
                    UrgencyControlSet.this,
                    hour, minute,
                    DateUtilities.is24HourFormat(activity), true);

            timePicker.show();
        }
    };


    /**
     * set up urgency adapter and picks the right selected item
     * @param dueDate
     */
    private void showUrgencySpinner(long dueDate) {
        // set up base urgency list
        String[] labels = activity.getResources().getStringArray(R.array.TEA_urgency);
        UrgencyValue[] urgencyValues = new UrgencyValue[labels.length];
        urgencyValues[0] = new UrgencyValue(labels[0],
                Task.URGENCY_NONE);
        urgencyValues[1] = new UrgencyValue(labels[1],
                Task.URGENCY_SPECIFIC_DAY_TIME, SPECIFIC_DATE);
        urgencyValues[2] = new UrgencyValue(labels[2],
                Task.URGENCY_TODAY);
        urgencyValues[3] = new UrgencyValue(labels[3],
                Task.URGENCY_TOMORROW);
        String dayAfterTomorrow = DateUtils.getDayOfWeekString(
                new Date(DateUtilities.now() + 2 * DateUtilities.ONE_DAY).getDay() +
                Calendar.SUNDAY, DateUtils.LENGTH_LONG);
        urgencyValues[4] = new UrgencyValue(dayAfterTomorrow,
                Task.URGENCY_DAY_AFTER);
        urgencyValues[5] = new UrgencyValue(labels[5],
                Task.URGENCY_NEXT_WEEK);
        urgencyValues[6] = new UrgencyValue(labels[6],
                Task.URGENCY_IN_TWO_WEEKS);
        urgencyValues[7] = new UrgencyValue(labels[7],
                Task.URGENCY_NEXT_MONTH);

        // search for setting
        int selection = -1;
        for(int i = 0; i < urgencyValues.length; i++)
            if(urgencyValues[i].dueDate == dueDate) {
                selection = i;
                break;
            }

        if(selection == -1) {
            UrgencyValue[] updated = new UrgencyValue[labels.length + 1];
            for(int i = 0; i < labels.length; i++)
                updated[i+1] = urgencyValues[i];
            if(Task.hasDueTime(dueDate)) {
                Date dueDateAsDate = new Date(dueDate);
                updated[0] = new UrgencyValue(DateUtilities.getDateStringWithTime(activity, dueDateAsDate),
                        Task.URGENCY_SPECIFIC_DAY_TIME, dueDate);
            } else {
                updated[0] = new UrgencyValue(DateUtilities.getDateString(activity, new Date(dueDate)),
                        Task.URGENCY_SPECIFIC_DAY, dueDate);
            }
            selection = 0;
            urgencyValues = updated;
        }

        urgencyAdapter = new ArrayAdapter<UrgencyValue>(
                activity, android.R.layout.simple_dropdown_item_1line,
                urgencyValues);

        new AlertDialog.Builder(activity)
            .setTitle(R.string.TEA_urgency_label)
            .setAdapter(urgencyAdapter, spinnerClickListener)
            .setIcon(R.drawable.gl_date_blue)
            .show().setOwnerActivity(activity);
    }

    DialogInterface.OnClickListener spinnerClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int position) {
            UrgencyValue item = urgencyAdapter.getItem(position);
            if(item.dueDate == SPECIFIC_DATE) {
                customSetting = item.setting;
                Date date = new Date(dueDateValue == 0 ? DateUtilities.now() : dueDateValue);
                date.setSeconds(0);

                final CalendarDialog calendarDialog = new CalendarDialog(activity, date);
                final AtomicBoolean cancelled = new AtomicBoolean(false);
                calendarDialog.show();
                calendarDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        if (!cancelled.get())
                            setDateFromCalendar(calendarDialog);
                    }
                });

                calendarDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        cancelled.set(true);
                    }
                });
            } else {
                dueDateValue = item.dueDate;
                if(dueDateValue == 0)
                    dueTimeValue = 0;
                updateButtons();
            }
        }
    };

    // --- date setting logic

    int customSetting;

    private void setDateFromCalendar(CalendarDialog calendarDialog) {
        Date date = calendarDialog.getCalendarDate();
        date.setMinutes(0);
        dueDateValue = Task.createDueDate(customSetting, date.getTime());

        updateButtons();
    }

    public void onTimeSet(TimePicker view, boolean hasTime, int hourOfDay, int minute) {
        if(!hasTime)
            dueTimeValue = 0;
        else {
            Date date = new Date();
            date.setHours(hourOfDay);
            date.setMinutes(minute);
            date.setSeconds(0);
            dueTimeValue = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, date.getTime());

            if(dueDateValue == 0)
                dueDateValue = DateUtilities.now();
        }
        updateButtons();
    }

    private void updateButtons() {
        if(dueDateValue == 0) {
            dateButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.gl_date, 0, 0, 0);
            dateButton.setText(R.string.TEA_urgency_none);
        } else {
            dateButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.gl_date_blue, 0, 0, 0);
            dateButton.setText(DateUtilities.getDateString(activity, new Date(dueDateValue)));
        }

        if(dueTimeValue == 0 || !Task.hasDueTime(dueTimeValue)) {
            timeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.gl_time, 0, 0, 0);
            timeButton.setText(R.string.TEA_urgency_none);
        } else {
            timeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.gl_time_blue, 0, 0, 0);
            timeButton.setText(DateUtilities.getTimeString(activity, new Date(dueTimeValue)));
        }
    }

    // --- setting up values

    @Override
    public void readFromTask(Task task) {
        dueTimeValue = dueDateValue = task.getValue(Task.DUE_DATE);
        updateButtons();
    }

    @Override
    public String writeToModel(Task task) {
        Date date = new Date(dueDateValue);
        if(dueTimeValue > 0) {
            Date time = new Date(dueTimeValue);
            date.setHours(time.getHours());
            date.setMinutes(time.getMinutes());
            date.setSeconds(time.getSeconds());
        } else {
            date.setTime(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDateValue));
        }

        task.setValue(Task.DUE_DATE, date.getTime());
        return null;
    }

}