package com.todoroo.astrid.repeats;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.timsu.astrid.R;
import com.timsu.astrid.widget.NumberPicker;
import com.timsu.astrid.widget.NumberPickerDialog;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.model.Task;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet implements TaskEditControlSet {

    private static final int INTERVAL_DAYS = 0;
    private static final int INTERVAL_WEEKS = 1;
    private static final int INTERVAL_MONTHS = 2;
    private static final int INTERVAL_YEARS = 3;

    private final Activity activity;
    private final CheckBox enabled;
    private final Button value;
    private final Spinner interval;
    private final Spinner type;
    private final LinearLayout repeatContainer;
    private final LinearLayout daysOfWeekContainer;
    private final CompoundButton[] daysOfWeek = new CompoundButton[7];

    public RepeatControlSet(final Activity activity, ViewGroup parent) {
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.repeat_control, parent, true);

        enabled = (CheckBox) activity.findViewById(R.id.repeatEnabled);
        value = (Button) activity.findViewById(R.id.repeatValue);
        interval = (Spinner) activity.findViewById(R.id.repeatInterval);
        type = (Spinner) activity.findViewById(R.id.repeatType);
        repeatContainer = (LinearLayout) activity.findViewById(R.id.repeatContainer);
        daysOfWeekContainer = (LinearLayout) activity.findViewById(R.id.repeatDayOfWeekContainer);
        setRepeatValue(1);

        // set up days of week
        DateFormatSymbols dfs = new DateFormatSymbols();
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f/7);
        for(int i = 0; i < 7; i++) {
            CheckBox checkBox = new CheckBox(activity);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            checkBox.setText(dfs.getShortWeekdays()[dayOfWeek].substring(0, 1));
            checkBox.setLayoutParams(lp);
            checkBox.setTextSize(10);
            checkBox.setTag(Weekday.values()[dayOfWeek - 1]);
            if(dayOfWeek == currentDayOfWeek)
                checkBox.setChecked(true);
            daysOfWeek[i] = checkBox;
            calendar.add(Calendar.DATE, 1);
            daysOfWeekContainer.addView(checkBox);
        }

        // set up listeners
        enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                repeatContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        value.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                repeatValueClick();
            }
        });
        interval.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                daysOfWeekContainer.setVisibility(position == INTERVAL_WEEKS ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });
    }

    /** Set up the repeat value button */
    private void setRepeatValue(int newValue) {
        value.setText(activity.getString(R.string.repeat_every, newValue));
        value.setTag(newValue);
    }

    protected void repeatValueClick() {
        final int tagValue = (Integer)value.getTag();

        final Runnable openDialogRunnable = new Runnable() {
            public void run() {
                int dialogValue = tagValue;
                if(dialogValue == 0)
                    dialogValue = 1;

                new NumberPickerDialog(activity, new OnNumberPickedListener() {
                    @Override
                    public void onNumberPicked(NumberPicker view,
                            int number) {
                        setRepeatValue(number);
                    }
                }, activity.getResources().getString(R.string.repeat_picker_title),
                dialogValue, 1, 1, 365).show();
            }
        };

        openDialogRunnable.run();
    }


    @Override
    public void readFromTask(Task task) {
        String recurrence = task.getValue(Task.RECURRENCE);

        if(recurrence.length() > 0) {
            try {
                RRule rrule = new RRule(recurrence);

                setRepeatValue(rrule.getInterval());
                switch(rrule.getFreq()) {
                case DAILY:
                    interval.setSelection(INTERVAL_DAYS);
                    break;
                case WEEKLY: {
                    interval.setSelection(INTERVAL_WEEKS);

                    for(WeekdayNum day : rrule.getByDay()) {
                        for(int i = 0; i < 7; i++)
                            if(daysOfWeek[i].getTag() == day.wday)
                                daysOfWeek[i].setChecked(true);
                    }

                    break;
                }
                case MONTHLY:
                    interval.setSelection(INTERVAL_MONTHS);
                    break;
                case YEARLY:
                    interval.setSelection(INTERVAL_YEARS);
                    break;
                }
            } catch (ParseException e) {
                recurrence = ""; //$NON-NLS-1$
            }
        }

        enabled.setChecked(recurrence.length() > 0);
        repeatContainer.setVisibility(enabled.isChecked() ? View.VISIBLE : View.GONE);
    }


    @Override
    public void writeToModel(Task task) {
        String result;
        if(!enabled.isChecked())
            result = ""; //$NON-NLS-1$
        else {
            RRule rrule = new RRule();
            rrule.setInterval((Integer)value.getTag());
            switch(interval.getSelectedItemPosition()) {
            case INTERVAL_DAYS:
                rrule.setFreq(Frequency.DAILY);
                break;
            case INTERVAL_WEEKS: {
                rrule.setFreq(Frequency.WEEKLY);
                ArrayList<WeekdayNum> days = new ArrayList<WeekdayNum>();
                for(int i = 0; i < daysOfWeek.length; i++)
                    if(daysOfWeek[i].isChecked())
                        days.add(new WeekdayNum(0, (Weekday)daysOfWeek[i].getTag()));
                rrule.setByDay(days);
                break;
            }
            case INTERVAL_MONTHS:
                rrule.setFreq(Frequency.MONTHLY);
                break;
            case INTERVAL_YEARS:
                rrule.setFreq(Frequency.YEARLY);
            }
            result = rrule.toIcal();
        }
        task.setValue(Task.RECURRENCE, result);
    }
}