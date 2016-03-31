/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.NumberPickerDialog;
import com.todoroo.astrid.ui.NumberPickerDialog.OnNumberPickedListener;

import org.tasks.R;
import org.tasks.activities.DatePickerActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.ThemeManager;
import org.tasks.time.DateTime;
import org.tasks.ui.TaskEditControlFragment;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_repeat_pref;

    public interface RepeatChangedListener {
        void repeatChanged(boolean repeat);
    }

    private static final int REQUEST_PICK_DATE = 505;
    private static final String EXTRA_RECURRENCE = "extra_recurrence";
    private static final String EXTRA_REPEAT_UNTIL = "extra_repeat_until";
    private static final String EXTRA_REPEAT_AFTER_COMPLETION = "extra_repeat_after_completion";

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
    private Spinner repeatUntil;

    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;
    @Inject @ForActivity Context context;
    @Inject ThemeManager themeManager;

    @Bind(R.id.clear) ImageView clear;
    @Bind(R.id.display_row_edit) TextView displayView;

    private ArrayAdapter<String> repeatUntilAdapter;
    private final List<String> repeatUntilOptions = new ArrayList<>();
    private LinearLayout daysOfWeekContainer;
    private final Weekday[] weekdays = new Weekday[7];
    private final boolean[] isChecked = new boolean[7];

    private String recurrence;
    private int repeatValue;
    private int intervalValue;
    private long repeatUntilValue;
    private View dialogView;
    private AlertDialog dialog;

    private RepeatChangedListener callback;

    private boolean repeatAfterCompletion;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            recurrence = savedInstanceState.getString(EXTRA_RECURRENCE);
            repeatUntilValue = savedInstanceState.getLong(EXTRA_REPEAT_UNTIL);
            repeatAfterCompletion = savedInstanceState.getBoolean(EXTRA_REPEAT_AFTER_COMPLETION);
        }

        dialogView = inflater.inflate(R.layout.control_set_repeat, null);
        value = (Button) dialogView.findViewById(R.id.repeatValue);
        Spinner interval = (Spinner) dialogView.findViewById(R.id.repeatInterval);
        interval.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.repeat_interval)) {{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }});
        Spinner type = (Spinner) dialogView.findViewById(R.id.repeatType);
        type.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.repeat_type)) {{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }});
        type.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                repeatAfterCompletion = position == TYPE_COMPLETION_DATE;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        daysOfWeekContainer = (LinearLayout) dialogView.findViewById(R.id.repeatDayOfWeekContainer);
        repeatUntil = (Spinner) dialogView.findViewById(R.id.repeat_until);
        repeatUntilAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, repeatUntilOptions);
        repeatUntilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatUntil.setAdapter(repeatUntilAdapter);
        // set up days of week
        DateFormatSymbols dfs = new DateFormatSymbols();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        CompoundButton[] daysOfWeek = new CompoundButton[7];
        for(int i = 0; i < 7; i++) {
            final int index = i;
            CheckBox checkBox = (CheckBox) daysOfWeekContainer.getChildAt(i);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    RepeatControlSet.this.isChecked[index] = isChecked;
                }
            });
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            checkBox.setText(dfs.getShortWeekdays()[dayOfWeek].substring(0, 1));
            daysOfWeek[i] = checkBox;
            weekdays[i] = Weekday.values()[dayOfWeek - 1];
            calendar.add(Calendar.DATE, 1);
        }

        // set up listeners
        value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeatValueClick();
            }
        });

        setRepeatValue(1);
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

        setRepeatUntilValue(repeatUntilValue);
        repeatUntil.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (repeatUntilOptions.size() == 2) {
                    if (i == 0) {
                        setRepeatUntilValue(0);
                    } else {
                        repeatUntilClick();
                    }
                } else {
                    if (i == 1) {
                        setRepeatUntilValue(0);
                    } else if (i == 2) {
                        repeatUntilClick();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //
            }
        });

        daysOfWeekContainer.setVisibility(View.GONE);
        type.setSelection(repeatAfterCompletion ? TYPE_COMPLETION_DATE : TYPE_DUE_DATE);
        doRepeat = !Strings.isNullOrEmpty(recurrence);
        if (doRepeat) {
            // read recurrence rule
            try {
                RRule rrule = new RRule(recurrence);

                setRepeatValue(rrule.getInterval());

                for(WeekdayNum day : rrule.getByDay()) {
                    for(int i = 0; i < 7; i++) {
                        if (weekdays[i].equals(day.wday)) {
                            daysOfWeek[i].setChecked(true);
                        }
                    }
                }

                switch(rrule.getFreq()) {
                    case DAILY:
                        intervalValue = INTERVAL_DAYS;
                        break;
                    case WEEKLY:
                        intervalValue = INTERVAL_WEEKS;
                        break;
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
                        Timber.e(new Exception("Unhandled rrule frequency: " + recurrence), "repeat-unhandled-rule");
                }
                interval.setSelection(intervalValue);

            } catch (Exception e) {
                // invalid RRULE
                recurrence = ""; //$NON-NLS-1$
                Timber.e(e, e.getMessage());
            }
        }
        refreshDisplayView();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_RECURRENCE, getRecurrence());
        outState.putLong(EXTRA_REPEAT_UNTIL, repeatUntilValue);
        outState.putBoolean(EXTRA_REPEAT_AFTER_COMPLETION, repeatAfterCompletion);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (RepeatChangedListener) activity;
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    @OnClick(R.id.clear)
    void clearRepeat(View view) {
        doRepeat = false;
        refreshDisplayView();
        callback.repeatChanged(doRepeat);
    }

    @OnClick(R.id.display_row_edit)
    void openPopup(View view) {
        if (dialog == null) {
            buildDialog();
        }
        dialog.show();
    }

    protected Dialog buildDialog() {
        AlertDialog.Builder builder = dialogBuilder.newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doRepeat = true;

                        callback.repeatChanged(doRepeat);

                        refreshDisplayView();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        refreshDisplayView();
                    }
                });
        dialog = builder.show();
        return dialog;
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_repeat_display;
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_repeat_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        repeatAfterCompletion = task.repeatAfterCompletion();
        recurrence = task.sanitizedRecurrence();
        repeatUntilValue = task.getRepeatUntil();
    }

    @Override
    public boolean hasChanges(Task original) {
        return !getRecurrenceValue().equals(original.getRecurrence()) || repeatUntilValue != original.getRepeatUntil();
    }

    @Override
    public void apply(Task task) {
        task.setRecurrence(getRecurrenceValue());
        task.setRepeatUntil(repeatUntilValue);
    }

    private String getRecurrenceValue() {
        String result = getRecurrence();

        if (repeatAfterCompletion && !TextUtils.isEmpty(result)) {
            result += ";FROM=COMPLETION"; //$NON-NLS-1$
        }

        return result;
    }

    private String getRecurrence() {
        String result;
        if(!doRepeat) {
            result = ""; //$NON-NLS-1$
        } else {
            RRule rrule = new RRule();
            rrule.setInterval(repeatValue);
            switch(intervalValue) {
                case INTERVAL_DAYS:
                    rrule.setFreq(Frequency.DAILY);
                    break;
                case INTERVAL_WEEKS: {
                    rrule.setFreq(Frequency.WEEKLY);

                    ArrayList<WeekdayNum> days = new ArrayList<>();
                    for (int i = 0 ; i < isChecked.length ; i++) {
                        if (isChecked[i]) {
                            days.add(new WeekdayNum(0, weekdays[i]));
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

        return result;
    }

    /** Set up the repeat value button */
    private void setRepeatValue(int newValue) {
        repeatValue = newValue;
        value.setText(getString(R.string.repeat_every, newValue));
    }

    private void setRepeatUntilValue(long newValue) {
        repeatUntilValue = newValue;
        updateRepeatUntilOptions();
    }

    protected void repeatValueClick() {
        int dialogValue = repeatValue;
        if(dialogValue == 0) {
            dialogValue = 1;
        }

        new NumberPickerDialog(context, themeManager.getDialogThemeResId(), new OnNumberPickedListener() {
            @Override
            public void onNumberPicked(int number) {
                setRepeatValue(number);
            }
        }, getResources().getString(R.string.repeat_interval_prompt),
        dialogValue, 1, 1, 365).show();
    }

    private void repeatUntilClick() {
        startActivityForResult(new Intent(context, DatePickerActivity.class) {{
            putExtra(DatePickerActivity.EXTRA_TIMESTAMP, repeatUntilValue > 0 ? repeatUntilValue : 0L);
        }}, REQUEST_PICK_DATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                setRepeatUntilValue(data.getLongExtra(DatePickerActivity.EXTRA_TIMESTAMP, 0L));
            } else {
                setRepeatUntilValue(repeatUntilValue);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void refreshDisplayView() {
        if (doRepeat) {
            displayView.setText(getRepeatString());
            displayView.setAlpha(1.0f);
            clear.setVisibility(View.VISIBLE);
        } else {
            displayView.setAlpha(0.5f);
            displayView.setText(R.string.repeat_never);
            clear.setVisibility(View.GONE);
        }
    }

    private String getRepeatString() {
        int arrayResource = R.array.repeat_interval;

        String[] dates = getResources().getStringArray(
                    arrayResource);
        String date = String.format("%s %s", repeatValue, dates[intervalValue]); //$NON-NLS-1$
        if (repeatUntilValue > 0) {
            return getString(R.string.repeat_detail_duedate_until, date, getDisplayString());
        } else {
            return getString(R.string.repeat_detail_duedate, date); // Every freq int
        }
    }

    private void updateRepeatUntilOptions() {
        repeatUntilOptions.clear();
        if (repeatUntilValue > 0) {
            repeatUntilOptions.add(getString(R.string.repeat_until, getDisplayString()));
        }
        repeatUntilOptions.add(getString(R.string.repeat_forever));
        repeatUntilOptions.add(getString(R.string.repeat_until, "").trim());
        repeatUntilAdapter.notifyDataSetChanged();
        repeatUntil.setSelection(0);
    }

    private String getDisplayString() {
        StringBuilder displayString = new StringBuilder();
        DateTime d = newDateTime(repeatUntilValue);
        if (d.getMillis() > 0) {
            displayString.append(DateUtilities.getDateString(d));
            if (Task.hasDueTime(repeatUntilValue)) {
                displayString.append(", "); //$NON-NLS-1$ //$NON-NLS-2$
                displayString.append(DateUtilities.getTimeString(context, repeatUntilValue));
            }
        }
        return displayString.toString();
    }
}
