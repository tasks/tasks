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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.ui.NumberPicker;
import com.todoroo.astrid.ui.NumberPickerDialog;
import com.todoroo.astrid.ui.NumberPickerDialog.OnNumberPickedListener;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet implements TaskEditControlSet {

    // --- spinner constants

    private static final int INTERVAL_DAYS = 0;
    private static final int INTERVAL_WEEKS = 1;
    private static final int INTERVAL_MONTHS = 2;
    private static final int INTERVAL_HOURS = 3;

    private static final int TYPE_DUE_DATE = 0;
    private static final int TYPE_COMPLETION_DATE = 1;

    // --- instance variables

    private final Activity activity;
    private final CheckBox enabled;
    private final Button value;
    private final Spinner interval;
    private final Spinner type;
    private final LinearLayout repeatContainer;
    private final LinearLayout daysOfWeekContainer;
    private final CompoundButton[] daysOfWeek = new CompoundButton[7];

    @Autowired
    ExceptionService exceptionService;

    // --- implementation

    public RepeatControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);

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
                }, activity.getResources().getString(R.string.repeat_interval_prompt),
                dialogValue, 1, 1, 365).show();
            }
        };

        openDialogRunnable.run();
    }


    @Override
    public void readFromTask(Task task) {
        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence == null)
            recurrence = ""; //$NON-NLS-1$

        // read recurrence rule
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
                case HOURLY:
                    interval.setSelection(INTERVAL_HOURS);
                    break;
                default:
                    // an unhandled recurrence
                    exceptionService.reportError("repeat-unhandled-rule",  //$NON-NLS-1$
                            new Exception("Unhandled rrule frequency: " + recurrence)); //$NON-NLS-1$
                }
            } catch (ParseException e) {
                recurrence = ""; //$NON-NLS-1$
                exceptionService.reportError("repeat-parse-exception", e);  //$NON-NLS-1$
            }
        }
        enabled.setChecked(recurrence.length() > 0);
        repeatContainer.setVisibility(enabled.isChecked() ? View.VISIBLE : View.GONE);

        // read flag
        if(task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION))
            type.setSelection(TYPE_COMPLETION_DATE);
        else
            type.setSelection(TYPE_DUE_DATE);
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
            case INTERVAL_HOURS:
                rrule.setFreq(Frequency.HOURLY);
            }
            result = rrule.toIcal();
        }
        task.setValue(Task.RECURRENCE, result);

        switch(type.getSelectedItemPosition()) {
        case TYPE_DUE_DATE:
            task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);
            break;
        case TYPE_COMPLETION_DATE:
            task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);
        }

        if(task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION))
            type.setSelection(1);
    }
}