/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.DateAndTimeDialog;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;
import com.todoroo.astrid.ui.DateAndTimePicker;
import com.todoroo.astrid.ui.NumberPickerDialog;
import com.todoroo.astrid.ui.NumberPickerDialog.OnNumberPickedListener;
import com.todoroo.astrid.ui.PopupControlSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet extends PopupControlSet {

    private static final Logger log = LoggerFactory.getLogger(RepeatControlSet.class);

    // --- spinner constants

    public static final int INTERVAL_DAYS = 0;
    public static final int INTERVAL_WEEKS = 1;
    public static final int INTERVAL_MONTHS = 2;
    public static final int INTERVAL_HOURS = 3;
    public static final int INTERVAL_MINUTES = 4;
    public static final int INTERVAL_YEARS = 5;

    private static final int TYPE_DUE_DATE = 0;
    private static final int TYPE_COMPLETION_DATE = 1;

    //private final CheckBox enabled;
    private boolean doRepeat = false;
    private Button value;
    private Spinner interval;
    private Spinner type;
    private Button repeatUntil;
    private LinearLayout daysOfWeekContainer;
    private final CompoundButton[] daysOfWeek = new CompoundButton[7];

    private String recurrence;
    private int repeatValue;
    private int intervalValue;
    private long repeatUntilValue;


    private final List<RepeatChangedListener> listeners = new LinkedList<>();

    public interface RepeatChangedListener {
        void repeatChanged(boolean repeat);
    }

    // --- implementation

    public RepeatControlSet(ActivityPreferences preferences, Activity activity) {
        super(preferences, activity, R.layout.control_set_repeat, R.layout.control_set_repeat_display, R.string.repeat_enabled);
    }

    /** Set up the repeat value button */
    private void setRepeatValue(int newValue) {
        repeatValue = newValue;
        value.setText(activity.getString(R.string.repeat_every, newValue));
    }

    private void setRepeatUntilValue(long newValue) {
        repeatUntilValue = newValue;

        if (newValue == 0) {
            repeatUntil.setText(activity.getString(R.string.repeat_forever));
        } else {
            repeatUntil.setText(activity.getString(R.string.repeat_until, DateAndTimePicker.getDisplayString(activity, newValue, false, false)));
        }
    }

    protected void repeatValueClick() {
        int dialogValue = repeatValue;
        if(dialogValue == 0) {
            dialogValue = 1;
        }

        new NumberPickerDialog(activity, preferences.getDialogTheme(), new OnNumberPickedListener() {
            @Override
            public void onNumberPicked(int number) {
                setRepeatValue(number);
            }
        }, activity.getResources().getString(R.string.repeat_interval_prompt),
        dialogValue, 1, 1, 365).show();
    }

    private void repeatUntilClick() {
        DateAndTimeDialog d = new DateAndTimeDialog(preferences, activity, repeatUntilValue,
                R.layout.repeat_until_dialog, R.string.repeat_until_title);
        d.setDateAndTimeDialogListener(new DateAndTimeDialogListener() {
            @Override
            public void onDateAndTimeSelected(long date) {
                setRepeatUntilValue(date);
            }
        });
        d.show();
    }

    public void addListener(RepeatChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        recurrence = model.sanitizedRecurrence();
        if(recurrence == null) {
            recurrence = "";
        }

        repeatUntilValue = model.getRepeatUntil();

        if(recurrence.length() > 0) {
            try {
                RRule rrule = new RRule(recurrence);
                repeatValue = rrule.getInterval();
                switch(rrule.getFreq()) {
                case DAILY:
                    intervalValue = INTERVAL_DAYS;
                    break;
                case WEEKLY: {
                    intervalValue = INTERVAL_WEEKS;
                    break;
                }
                case MONTHLY:
                    intervalValue = INTERVAL_MONTHS;
                    break;
                case HOURLY:
                    intervalValue = INTERVAL_HOURS;
                    break;
                case MINUTELY:
                    intervalValue = INTERVAL_MINUTES;
                    break;
                case YEARLY:
                    intervalValue = INTERVAL_YEARS;
                    break;
                default:
                    log.error("repeat-unhandled-rule", new Exception("Unhandled rrule frequency: " + recurrence));
                }
            } catch (Exception e) {
                // invalid RRULE
                recurrence = ""; //$NON-NLS-1$
                log.error(e.getMessage(), e);
            }
        }
        doRepeat = recurrence.length() > 0;
        refreshDisplayView();
    }

    @Override
    public int getIcon() {
        return R.attr.ic_action_reload;
    }

    @Override
    protected void readFromTaskOnInitialize() {
        Date date;
        if(model.getDueDate() != 0) {
            date = newDate(model.getDueDate());

            int dayOfWeek = date.getDay();
            for(int i = 0; i < 7; i++) {
                daysOfWeek[i].setChecked(i == dayOfWeek);
            }
        }

        // read recurrence rule
        if(recurrence.length() > 0) {
            try {
                RRule rrule = new RRule(recurrence);

                setRepeatValue(rrule.getInterval());
                setRepeatUntilValue(model.getRepeatUntil());
                interval.setSelection(intervalValue);

                // clear all day of week checks, then update them
                for(int i = 0; i < 7; i++) {
                    daysOfWeek[i].setChecked(false);
                }

                for(WeekdayNum day : rrule.getByDay()) {
                    for(int i = 0; i < 7; i++) {
                        if (daysOfWeek[i].getTag().equals(day.wday)) {
                            daysOfWeek[i].setChecked(true);
                        }
                    }
                }
            } catch (Exception e) {
                // invalid RRULE
                recurrence = ""; //$NON-NLS-1$
                log.error(e.getMessage(), e);
            }
        }
        doRepeat = recurrence.length() > 0;

        // read flag
        if(model.repeatAfterCompletion()) {
            type.setSelection(TYPE_COMPLETION_DATE);
        } else {
            type.setSelection(TYPE_DUE_DATE);
        }

        refreshDisplayView();
    }

    @Override
    protected void afterInflate() {
        value = (Button) getDialogView().findViewById(R.id.repeatValue);
        interval = (Spinner) getDialogView().findViewById(R.id.repeatInterval);
        interval.setAdapter(new ArrayAdapter<String>(activity, R.layout.simple_spinner_item, activity.getResources().getStringArray(R.array.repeat_interval)) {{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }});
        type = (Spinner) getDialogView().findViewById(R.id.repeatType);
        type.setAdapter(new ArrayAdapter<String>(activity, R.layout.simple_spinner_item, activity.getResources().getStringArray(R.array.repeat_type)) {{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }});
        daysOfWeekContainer = (LinearLayout) getDialogView().findViewById(R.id.repeatDayOfWeekContainer);
        repeatUntil = (Button) getDialogView().findViewById(R.id.repeatUntil);
        setRepeatValue(1);
        setRepeatUntilValue(0);

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
            label.setTextAppearance(activity, R.style.TextAppearance);
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
            @Override
            public void onClick(View v) {
                repeatValueClick();
            }
        });

        interval.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                daysOfWeekContainer.setVisibility(position == INTERVAL_WEEKS ? View.VISIBLE : View.GONE);
                intervalValue = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //
            }
        });

        repeatUntil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeatUntilClick();
            }
        });
        daysOfWeekContainer.setVisibility(View.GONE);
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        String result;
        if(!doRepeat) {
            result = ""; //$NON-NLS-1$
        } else {
            RRule rrule = new RRule();
            rrule.setInterval(repeatValue);
            switch(interval.getSelectedItemPosition()) {
            case INTERVAL_DAYS:
                rrule.setFreq(Frequency.DAILY);
                break;
            case INTERVAL_WEEKS: {
                rrule.setFreq(Frequency.WEEKLY);

                ArrayList<WeekdayNum> days = new ArrayList<>();
                for (CompoundButton dayOfWeek : daysOfWeek) {
                    if (dayOfWeek.isChecked()) {
                        days.add(new WeekdayNum(0, (Weekday) dayOfWeek.getTag()));
                    }
                }
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

        if (type.getSelectedItemPosition() == TYPE_COMPLETION_DATE && !TextUtils.isEmpty(result)) {
            result = result + ";FROM=COMPLETION"; //$NON-NLS-1$
        }

        task.setRecurrence(result);
        task.setRepeatUntil(repeatUntilValue);

        if(task.repeatAfterCompletion()) {
            type.setSelection(1);
        }
    }

    @Override
    protected void refreshDisplayView() {
        TextView repeatDisplay = (TextView) getView().findViewById(R.id.display_row_edit);
        if (doRepeat) {
            repeatDisplay.setText(getRepeatString());
            repeatDisplay.setTextColor(themeColor);
        } else {
            repeatDisplay.setTextColor(unsetColor);
            repeatDisplay.setText(R.string.repeat_never);
        }
    }

    private String getRepeatString() {
        int arrayResource = R.array.repeat_interval;

        String[] dates = activity.getResources().getStringArray(
                    arrayResource);
        String date = String.format("%s %s", repeatValue, dates[intervalValue]); //$NON-NLS-1$
        if (repeatUntilValue > 0) {
            return activity.getString(R.string.repeat_detail_duedate_until, date, DateAndTimePicker.getDisplayString(activity, repeatUntilValue, false, false));
        } else {
            return activity.getString(R.string.repeat_detail_duedate, date); // Every freq int
        }
    }

    @Override
    protected Dialog buildDialog(String title, final PopupDialogClickListener okListener, final DialogInterface.OnCancelListener cancelListener) {

        PopupDialogClickListener doRepeatButton = new PopupDialogClickListener() {
            @Override
            public boolean onClick(DialogInterface d, int which) {
                doRepeat = true;

                for (RepeatChangedListener l : listeners) {
                    l.repeatChanged(doRepeat);
                }
                return okListener.onClick(d, which);
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
                    l.repeatChanged(doRepeat);
                }
            }
        };
        getDialogView().findViewById(R.id.edit_dont_repeat).setOnClickListener(dontRepeatButton);

        return d;
    }
}
