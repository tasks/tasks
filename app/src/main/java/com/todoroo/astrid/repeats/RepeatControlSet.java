/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.repeats.CustomRecurrenceDialog;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;
import org.tasks.ui.SingleCheckedArrayAdapter;
import org.tasks.ui.TaskEditControlFragment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.repeats.CustomRecurrenceDialog.newCustomRecurrenceDialog;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatControlSet extends TaskEditControlFragment
        implements CustomRecurrenceDialog.CustomRecurrenceCallback {

    public static final int TAG = R.string.TEA_ctrl_repeat_pref;
    private static final String FRAG_TAG_CUSTOM_RECURRENCE = "frag_tag_custom_recurrence";

    @Override
    public void onSelected(int frequency, int interval, long repeatUntilValue,
                           boolean repeatAfterCompletion, boolean[] isChecked) {
        doRepeat = true;
        this.interval = interval;
        this.frequency = frequency;
        this.repeatUntilValue = repeatUntilValue;
        this.isChecked = isChecked;
        this.repeatAfterCompletion = repeatAfterCompletion;
        refreshDisplayView();
    }

    public interface RepeatChangedListener {
        void repeatChanged(boolean repeat);
    }

    private static final String EXTRA_RECURRENCE = "extra_recurrence";
    private static final String EXTRA_REPEAT_UNTIL = "extra_repeat_until";
    private static final String EXTRA_REPEAT_AFTER_COMPLETION = "extra_repeat_after_completion";

    // --- spinner constants

    public static final int FREQUENCY_MINUTES = 0;
    public static final int FREQUENCY_HOURS = 1;
    public static final int FREQUENCY_DAYS = 2;
    public static final int FREQUENCY_WEEKS = 3;
    public static final int FREQUENCY_MONTHS = 4;
    public static final int FREQUENCY_YEARS = 5;

    public static final int TYPE_DUE_DATE = 0;
    public static final int TYPE_COMPLETION_DATE = 1;

    //private final CheckBox enabled;
    private boolean doRepeat = false;

    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;
    @Inject @ForActivity Context context;
    @Inject Theme theme;

    @BindView(R.id.display_row_edit) TextView displayView;

    private String recurrence;
    private int interval;
    private int frequency;
    private long repeatUntilValue;
    private boolean[] isChecked;
    private final Weekday[] weekdays = new Weekday[7];

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

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        for(int i = 0; i < 7; i++) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            weekdays[i] = Weekday.values()[dayOfWeek - 1];
            calendar.add(Calendar.DATE, 1);
        }

        doRepeat = !Strings.isNullOrEmpty(recurrence);
        if (doRepeat) {
            try {
                RRule rrule = new RRule(recurrence);
                interval = rrule.getInterval();
                isChecked = new boolean[7];
                for (WeekdayNum day : rrule.getByDay()) {
                    for (int i = 0 ; i < 7 ; i++) {
                        if (weekdays[i].equals(day.wday)) {
                            isChecked[i] = true;
                        }
                    }
                }
                switch (rrule.getFreq()) {
                    case DAILY:
                        frequency = FREQUENCY_DAYS;
                        break;
                    case WEEKLY:
                        frequency = FREQUENCY_WEEKS;
                        break;
                    case MONTHLY:
                        frequency = FREQUENCY_MONTHS;
                        break;
                    case HOURLY:
                        frequency = FREQUENCY_HOURS;
                        break;
                    case MINUTELY:
                        frequency = FREQUENCY_MINUTES;
                        break;
                    case YEARLY:
                        frequency = FREQUENCY_YEARS;
                        break;

                }
            } catch (ParseException e) {
                recurrence = "";
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

    private boolean isCustomValue() {
        if (!doRepeat) {
            return false;
        }
        if (frequency == FREQUENCY_WEEKS) {
            for (boolean checked : isChecked) {
                if (checked) {
                    return true;
                }
            }
        }
        return frequency == FREQUENCY_HOURS ||
                frequency == FREQUENCY_MINUTES ||
                !(repeatUntilValue == 0 && interval == 1 && !repeatAfterCompletion);
    }

    @OnClick(R.id.display_row_edit)
    void openPopup(View view) {
        boolean customPicked = isCustomValue();
        List<String> repeatOptions = newArrayList(context.getResources().getStringArray(R.array.repeat_options));
        SingleCheckedArrayAdapter<String> adapter = new SingleCheckedArrayAdapter<>(context, repeatOptions);
        if (customPicked) {
            adapter.insert(getRepeatString(), 0);
            adapter.setChecked(0);
        } else if (!doRepeat) {
            adapter.setChecked(0);
        } else {
            int selected;
            switch(frequency) {
                case FREQUENCY_DAYS:
                    selected = 1;
                    break;
                case FREQUENCY_WEEKS:
                    selected = 2;
                    break;
                case FREQUENCY_MONTHS:
                    selected = 3;
                    break;
                case FREQUENCY_YEARS:
                    selected = 4;
                    break;
                default:
                    selected = 0;
                    break;
            }
            adapter.setChecked(selected);
        }
        dialogBuilder.newDialog()
                .setAdapter(adapter, (dialogInterface, i) -> {
                    if (customPicked) {
                        if (i == 0) {
                            return;
                        }
                        i--;
                    }
                    if (i == 0) {
                        doRepeat = false;
                    } else if (i == 5) {
                        newCustomRecurrenceDialog(this)
                                .show(getFragmentManager(), FRAG_TAG_CUSTOM_RECURRENCE);
                        return;
                    } else {
                        doRepeat = true;
                        repeatAfterCompletion = false;
                        interval = 1;
                        repeatUntilValue = 0;

                        switch (i) {
                            case 1:
                                frequency = FREQUENCY_DAYS;
                                break;
                            case 2:
                                frequency = FREQUENCY_WEEKS;
                                isChecked = new boolean[7];
                                break;
                            case 3:
                                frequency = FREQUENCY_MONTHS;
                                break;
                            case 4:
                                frequency = FREQUENCY_YEARS;
                                break;
                        }
                    }

                    callback.repeatChanged(doRepeat);

                    refreshDisplayView();
                })
                .setOnCancelListener(d -> refreshDisplayView())
                .show();
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
            rrule.setInterval(interval);
            switch(frequency) {
                case FREQUENCY_DAYS:
                    rrule.setFreq(Frequency.DAILY);
                    break;
                case FREQUENCY_WEEKS: {
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
                case FREQUENCY_MONTHS:
                    rrule.setFreq(Frequency.MONTHLY);
                    break;
                case FREQUENCY_HOURS:
                    rrule.setFreq(Frequency.HOURLY);
                    break;
                case FREQUENCY_MINUTES:
                    rrule.setFreq(Frequency.MINUTELY);
                    break;
                case FREQUENCY_YEARS:
                    rrule.setFreq(Frequency.YEARLY);
                    break;
            }

            result = rrule.toIcal();
        }

        return result;
    }

    private void refreshDisplayView() {
        if (doRepeat) {
            displayView.setText(getRepeatString());
            displayView.setTextColor(getColor(context, R.color.text_primary));
        } else {
            displayView.setText(R.string.repeat_option_does_not_repeat);
            displayView.setTextColor(getColor(context, R.color.text_tertiary));
        }
    }

    private String getRepeatString() {
        if (!isCustomValue()) {
            switch (frequency) {
                case FREQUENCY_DAYS:
                    return getString(R.string.repeat_option_every_day);
                case FREQUENCY_WEEKS:
                    return getString(R.string.repeat_option_every_week);
                case FREQUENCY_MONTHS:
                    return getString(R.string.repeat_option_every_month);
                case FREQUENCY_YEARS:
                    return getString(R.string.repeat_option_every_year);
            }
        }

        int arrayResource = R.array.repeat_interval;

        String[] dates = getResources().getStringArray(
                arrayResource);
        String date = String.format("%s %s", interval, dates[frequency]); //$NON-NLS-1$
        if (repeatUntilValue > 0) {
            return getString(R.string.repeat_detail_duedate_until, date, getDisplayString());
        } else {
            return getString(R.string.repeat_detail_duedate, date); // Every freq int
        }
    }

    private String getDisplayString() {
        return getDisplayString(context, repeatUntilValue);
    }

    public static String getDisplayString(Context context, long repeatUntilValue) {
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
