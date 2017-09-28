/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.primitives.Booleans;
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
import org.tasks.locale.Locale;
import org.tasks.preferences.Preferences;
import org.tasks.repeats.CustomRecurrenceDialog;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;
import org.tasks.ui.HiddenTopArrayAdapter;
import org.tasks.ui.SingleCheckedArrayAdapter;
import org.tasks.ui.TaskEditControlFragment;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import timber.log.Timber;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Lists.newArrayList;
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
    public void onSelected(int frequency, int interval, long repeatUntilValue, boolean[] isChecked) {
        doRepeat = true;
        this.interval = interval;
        this.frequency = frequency;
        this.repeatUntilValue = repeatUntilValue;
        this.isChecked = isChecked;
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
    @Inject Locale locale;

    @BindView(R.id.display_row_edit) TextView displayView;
    @BindView(R.id.repeatType) Spinner typeSpinner;
    @BindView(R.id.repeatTypeContainer) LinearLayout repeatTypeContainer;

    private String recurrence;
    private int interval;
    private int frequency;
    private long repeatUntilValue;
    private boolean[] isChecked;
    private final Weekday[] weekdays = new Weekday[7];
    private final List<String> repeatTypes = new ArrayList<>();
    private HiddenTopArrayAdapter<String> typeAdapter;

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

        repeatTypes.add("");
        repeatTypes.addAll(Arrays.asList(getResources().getStringArray(R.array.repeat_type)));
        typeAdapter = new HiddenTopArrayAdapter<String>(context, 0, repeatTypes) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setPadding(0, 0, 0, 0);
                tv.setText(repeatTypes.get(selectedItemPosition));
                return tv;
            }
        };
        Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.textfield_underline_black));
        drawable.mutate();
        DrawableCompat.setTint(drawable, getColor(context, R.color.text_primary));
        typeSpinner.setBackgroundDrawable(drawable);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(repeatAfterCompletion ? TYPE_COMPLETION_DATE : TYPE_DUE_DATE);

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

    @OnItemSelected(R.id.repeatType)
    public void onRepeatTypeChanged(Spinner spinner, int position) {
        repeatAfterCompletion = position == TYPE_COMPLETION_DATE;
        repeatTypes.set(0, repeatAfterCompletion ? repeatTypes.get(2) : repeatTypes.get(1));
        typeAdapter.notifyDataSetChanged();
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
        return frequency == FREQUENCY_WEEKS && any(Booleans.asList(isChecked), b -> b) ||
                frequency == FREQUENCY_HOURS ||
                frequency == FREQUENCY_MINUTES ||
                repeatUntilValue != 0 ||
                interval != 1;
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
            repeatTypeContainer.setVisibility(View.VISIBLE);
        } else {
            displayView.setText(R.string.repeat_option_does_not_repeat);
            displayView.setTextColor(getColor(context, R.color.text_tertiary));
            repeatTypeContainer.setVisibility(View.GONE);
        }
    }

    private String getRepeatString() {
        if (interval == 1) {
            String frequencyString = getString(getSingleFrequencyResource(frequency));
            if (frequency == FREQUENCY_WEEKS && any(Booleans.asList(isChecked), b -> b)) {
                String dayString = getDayString();
                if (repeatUntilValue > 0) {
                    return getString(R.string.repeats_single_on_until, frequencyString, dayString, DateUtilities.getLongDateString(new DateTime(repeatUntilValue)));
                } else {
                    return getString(R.string.repeats_single_on, frequencyString, dayString);
                }
            } else if (repeatUntilValue > 0) {
                return getString(R.string.repeats_single_until, frequencyString, DateUtilities.getLongDateString(new DateTime(repeatUntilValue)));
            } else {
                return getString(R.string.repeats_single, frequencyString);
            }
        } else {
            int plural = getFrequencyPlural(frequency);
            String frequencyPlural = getResources().getQuantityString(plural, interval, interval);
            if (frequency == FREQUENCY_WEEKS && any(Booleans.asList(isChecked), b -> b)) {
                String dayString = getDayString();
                if (repeatUntilValue > 0) {
                    return getString(R.string.repeats_plural_on_until, frequencyPlural, dayString, DateUtilities.getLongDateString(new DateTime(repeatUntilValue)));
                } else {
                    return getString(R.string.repeats_plural_on, frequencyPlural, dayString);
                }
            } else if (repeatUntilValue > 0) {
                return getString(R.string.repeats_plural_until, frequencyPlural, DateUtilities.getLongDateString(new DateTime(repeatUntilValue)));
            } else {
                return getString(R.string.repeats_plural, frequencyPlural);
            }
        }
    }

    private String getDayString() {
        DateFormatSymbols dfs = new DateFormatSymbols(locale.getLocale());
        String[] shortWeekdays = dfs.getShortWeekdays();
        List<String> days = new ArrayList<>();
        for (int i = 0 ; i < 7 ; i++) {
            if (isChecked[i]) {
                days.add(shortWeekdays[i + 1]);
            }
        }
        return Joiner.on(getString(R.string.list_separator_with_space)).join(days);
    }

    private int getSingleFrequencyResource(int frequency) {
        switch (frequency) {
            case FREQUENCY_MINUTES:
                return R.string.repeats_minutely;
            case FREQUENCY_HOURS:
                return R.string.repeats_hourly;
            case FREQUENCY_DAYS:
                return R.string.repeats_daily;
            case FREQUENCY_WEEKS:
                return R.string.repeats_weekly;
            case FREQUENCY_MONTHS:
                return R.string.repeats_monthly;
            case FREQUENCY_YEARS:
                return R.string.repeats_yearly;
            default:
                throw new RuntimeException("Invalid frequency: " + frequency);
        }
    }

    private int getFrequencyPlural(int frequency) {
        switch (frequency) {
            case FREQUENCY_MINUTES:
                return R.plurals.repeat_n_minutes;
            case FREQUENCY_HOURS:
                return R.plurals.repeat_n_hours;
            case FREQUENCY_DAYS:
                return R.plurals.repeat_n_days;
            case FREQUENCY_WEEKS:
                return R.plurals.repeat_n_weeks;
            case FREQUENCY_MONTHS:
                return R.plurals.repeat_n_months;
            case FREQUENCY_YEARS:
                return R.plurals.repeat_n_years;
            default:
                throw new RuntimeException("Invalid frequency: " + frequency);
        }
    }
}
