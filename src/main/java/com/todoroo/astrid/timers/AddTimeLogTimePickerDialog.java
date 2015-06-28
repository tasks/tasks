package com.todoroo.astrid.timers;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.todoroo.astrid.ui.DateAndTimeDialog;
import com.todoroo.astrid.ui.DateAndTimePicker;
import com.todoroo.astrid.ui.NNumberPickerDialog;

import org.tasks.R;
import org.tasks.date.DateTimeUtils;
import org.tasks.preferences.ActivityPreferences;

import java.util.Date;


public class AddTimeLogTimePickerDialog extends NNumberPickerDialog {

    private ActivityPreferences preferences;
    private OnTimeLogPickedListener onTimeLogPickedListener;
    private EditText descriptionView;
    private DateAndTimeDialog pickerDialog;
    private TextView dateTextView;
    private Date timeLogDate;

    /**
     * Instantiate the dialog box.
     *
     * @param preferences
     * @param context
     * @param callBack    callback function to get the numbers you requested
     * @param title       title of the dialog box
     */
    public AddTimeLogTimePickerDialog(ActivityPreferences preferences, Context context, OnTimeLogPickedListener callBack, String title, int timeSpentInSeconds, long date, String description) {
        super(context, null, title, seconds2HoursAndMinutes(timeSpentInSeconds), new int[]{1, 5}, new int[]{0, 0}, new int[]{999, 59}, new String[]{":", null}, R.layout.control_set_time_log_add);
        this.preferences = preferences;
        this.onTimeLogPickedListener = callBack;
        descriptionView = (EditText) view.findViewById(R.id.addTimeLog_description);
        descriptionView.setText(description);
        dateTextView = (TextView) view.findViewById(R.id.addTimeLog_date);
        dateTextView.setText(DateAndTimePicker.getDisplayString(context, date));
        timeLogDate = new Date(date);

        pickerDialog = new DateAndTimeDialog(preferences, context, date);
        pickerDialog.setDateAndTimeDialogListener(new TimeLogDateAndTimeDialogListener());
        dateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickerDialog.setSelectedDateAndTime(timeLogDate.getTime());
                pickerDialog.show();
            }
        });
    }

    private static int[] seconds2HoursAndMinutes(int timeSpentInSeconds) {
        int minutes = timeSpentInSeconds / 60;
        return new int[]{minutes / 60, minutes % 60};
    }

    @Override
    protected void setButtons(Context context) {
        setButton(BUTTON_POSITIVE, context.getText(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int hours = getPicker(0).getCurrent();
                int minutes = getPicker(1).getCurrent();
                int seconds = hours * 3600 + minutes * 60;
                onTimeLogPickedListener.onTimeLogChanged(seconds, timeLogDate, descriptionView.getText().toString());
            }
        });
        setButton(BUTTON_NEGATIVE, context.getText(android.R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onTimeLogPickedListener.onCancel();
            }
        });
        //TODO set string
        setButton(BUTTON_NEUTRAL, context.getText(R.string.TEA_timer_button_deleteTimeLog), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onTimeLogPickedListener.onTimeLogDeleted();
            }
        });
    }

    public interface OnTimeLogPickedListener {

        public void onTimeLogChanged(int timeSpentInSeconds, Date date, String description);

        public void onTimeLogDeleted();

        public void onCancel();
    }

    private class TimeLogDateAndTimeDialogListener implements DateAndTimeDialog.DateAndTimeDialogListener {
        @Override
        public void onDateAndTimeSelected(long date) {
            if (pickerDialog.hasTime()) {
                timeLogDate = new Date(date);
            } else {
                timeLogDate = DateTimeUtils.newDateWithoutHours(date);
            }
            dateTextView.setText(DateAndTimePicker.getDisplayString(getContext(), timeLogDate.getTime()));
        }

    }
}
