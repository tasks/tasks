package com.todoroo.astrid.repeats;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.ui.NumberPicker;
import com.todoroo.astrid.ui.NumberPickerDialog;
import com.todoroo.astrid.ui.NumberPickerDialog.OnNumberPickedListener;
import com.todoroo.astrid.ui.PopupControlSet;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet extends PopupControlSet {

    // --- spinner constants

    public static final int INTERVAL_DAYS = 0;
    public static final int INTERVAL_WEEKS = 1;
    public static final int INTERVAL_MONTHS = 2;
    public static final int INTERVAL_HOURS = 3;
    public static final int INTERVAL_MINUTES = 4;
    public static final int INTERVAL_YEARS = 5;

    private static final int TYPE_DUE_DATE = 0;
    private static final int TYPE_COMPLETION_DATE = 1;

    // --- instance variables

    private final Activity activity;
    //private final CheckBox enabled;
    private boolean doRepeat = true;
    private final Button value;
    private final Spinner interval;
    private final Spinner type;
    private final LinearLayout repeatContainer;
    private final LinearLayout daysOfWeekContainer;
    private final CompoundButton[] daysOfWeek = new CompoundButton[7];
    private Task model;


    private final List<RepeatChangedListener> listeners = new LinkedList<RepeatChangedListener>();

    public interface RepeatChangedListener {
        public void RepeatChanged(boolean repeat);
    }

    @Autowired
    ExceptionService exceptionService;

    boolean setInterval = false;

    // --- implementation

    public RepeatControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        value = (Button) getView().findViewById(R.id.repeatValue);
        interval = (Spinner) getView().findViewById(R.id.repeatInterval);
        type = (Spinner) getView().findViewById(R.id.repeatType);
        repeatContainer = (LinearLayout) getView().findViewById(R.id.repeatContainer);
        daysOfWeekContainer = (LinearLayout) getView().findViewById(R.id.repeatDayOfWeekContainer);
        setRepeatValue(1);

        // set up days of week
        DateFormatSymbols dfs = new DateFormatSymbols();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f/14);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f/14);
        for(int i = 0; i < 7; i++) {
            CheckBox checkBox = new CheckBox(activity);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            checkBox.setPadding(0, 0, 0, 0);
            checkBox.setLayoutParams(lp);
            checkBox.setTag(Weekday.values()[dayOfWeek - 1]);
            checkBox.setButtonDrawable(R.drawable.btn_check_small);

            TextView label = new TextView(activity);
            label.setTextAppearance(activity, R.style.TextAppearance_GEN_EditLabel);
            label.setLayoutParams(textLp);
            label.setTextSize(14);
            label.setText(dfs.getShortWeekdays()[dayOfWeek].substring(0, 1));

            daysOfWeek[i] = checkBox;
            calendar.add(Calendar.DATE, 1);
            daysOfWeekContainer.addView(checkBox);
            daysOfWeekContainer.addView(label);
        }

        // set up listeners
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
        daysOfWeekContainer.setVisibility(View.GONE);
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


    public void addListener(RepeatChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RepeatChangedListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }


    @SuppressWarnings("nls")
    @Override
    public void readFromTask(Task task) {
        model = task;

        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence == null)
            recurrence = "";

        Date date;
        if(model.getValue(Task.DUE_DATE) == 0)
            date = new Date();
        else
            date = new Date(model.getValue(Task.DUE_DATE));
        int dayOfWeek = date.getDay();
        for(int i = 0; i < 7; i++)
            daysOfWeek[i].setChecked(i == dayOfWeek);

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
                    break;
                }
                case MONTHLY:
                    interval.setSelection(INTERVAL_MONTHS);
                    break;
                case HOURLY:
                    interval.setSelection(INTERVAL_HOURS);
                    break;
                case MINUTELY:
                    interval.setSelection(INTERVAL_MINUTES);
                    break;
                case YEARLY:
                    interval.setSelection(INTERVAL_YEARS);
                    break;
                default:
                    // an unhandled recurrence
                    exceptionService.reportError("repeat-unhandled-rule",  //$NON-NLS-1$
                            new Exception("Unhandled rrule frequency: " + recurrence));
                }

                // clear all day of week checks, then update them
                for(int i = 0; i < 7; i++)
                    daysOfWeek[i].setChecked(false);

                for(WeekdayNum day : rrule.getByDay()) {
                    for(int i = 0; i < 7; i++)
                        if(daysOfWeek[i].getTag().equals(day.wday))
                            daysOfWeek[i].setChecked(true);
                }

                // suppress first call to interval.onItemSelected
                setInterval = true;
            } catch (Exception e) {
                // invalid RRULE
                recurrence = ""; //$NON-NLS-1$
                exceptionService.reportError("repeat-parse-exception", e);
            }
        }
        doRepeat = recurrence.length() > 0;

        // read flag
        if(task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION))
            type.setSelection(TYPE_COMPLETION_DATE);
        else
            type.setSelection(TYPE_DUE_DATE);

        refreshDisplayView();
    }


    @Override
    public String writeToModel(Task task) {
        String result;
        if(!doRepeat)
            result = ""; //$NON-NLS-1$
        else {
            if(TextUtils.isEmpty(task.getValue(Task.RECURRENCE))) {
                StatisticsService.reportEvent(StatisticsConstants.REPEAT_TASK_CREATE);
            }

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
                break;
            case INTERVAL_MINUTES:
                rrule.setFreq(Frequency.MINUTELY);
                break;
            case INTERVAL_YEARS:
                rrule.setFreq(Frequency.YEARLY);
                break;
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
        return null;
    }

    @Override
    protected void refreshDisplayView() {
        TextView repeatDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        if (doRepeat) {
            repeatDisplay.setText(R.string.repeat_enabled);
        } else {
            repeatDisplay.setText(R.string.repeat_never);
        }
    }

    @Override
    protected Dialog buildDialog(int title, final DialogInterface.OnClickListener okListener, final DialogInterface.OnCancelListener cancelListener) {

        DialogInterface.OnClickListener doRepeatButton = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                doRepeat = true;
                okListener.onClick(d, which);

                for (RepeatChangedListener l : listeners) {
                    l.RepeatChanged(doRepeat);
                }
            }
        };
        final Dialog d = super.buildDialog(title, doRepeatButton, cancelListener);

        View.OnClickListener dontRepeatButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doRepeat = false;
                refreshDisplayView();
                DialogUtilities.dismissDialog(activity, d);

                for (RepeatChangedListener l : listeners) {
                    l.RepeatChanged(doRepeat);
                }
            }
        };
        getView().findViewById(R.id.edit_dont_repeat).setOnClickListener(dontRepeatButton);

        return d;
    }
}