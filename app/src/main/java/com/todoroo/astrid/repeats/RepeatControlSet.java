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
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.locale.Locale;
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
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
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
    public static final List<Weekday> WEEKDAYS = Arrays.asList(Weekday.values());
    private static final String FRAG_TAG_CUSTOM_RECURRENCE = "frag_tag_custom_recurrence";

    @Override
    public void onSelected(RRule rrule) {
        this.rrule = rrule;
        tracker.reportEvent(Tracking.Events.RECURRENCE_CUSTOM, rrule.toIcal());
        refreshDisplayView();
    }

    public interface RepeatChangedListener {
        void repeatChanged(boolean repeat);
    }

    private static final String EXTRA_RECURRENCE = "extra_recurrence";
    private static final String EXTRA_REPEAT_AFTER_COMPLETION = "extra_repeat_after_completion";

    public static final int TYPE_DUE_DATE = 1;
    public static final int TYPE_COMPLETION_DATE = 2;

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject Theme theme;
    @Inject Locale locale;
    @Inject Tracker tracker;

    @BindView(R.id.display_row_edit) TextView displayView;
    @BindView(R.id.repeatType) Spinner typeSpinner;
    @BindView(R.id.repeatTypeContainer) LinearLayout repeatTypeContainer;

    private RRule rrule;
    private final List<String> repeatTypes = new ArrayList<>();
    private HiddenTopArrayAdapter<String> typeAdapter;

    private RepeatChangedListener callback;

    private boolean repeatAfterCompletion;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            String recurrence = savedInstanceState.getString(EXTRA_RECURRENCE);
            if (Strings.isNullOrEmpty(recurrence)) {
                rrule = null;
            } else {
                try {
                    rrule = new RRule(recurrence);
                } catch (ParseException e) {
                    rrule = null;
                }
            }
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

        refreshDisplayView();
        return view;
    }

    @OnItemSelected(R.id.repeatType)
    public void onRepeatTypeChanged(int position) {
        repeatAfterCompletion = position == TYPE_COMPLETION_DATE;
        repeatTypes.set(0, repeatAfterCompletion ? repeatTypes.get(2) : repeatTypes.get(1));
        typeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_RECURRENCE, rrule == null ? "" : rrule.toIcal());
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
        if (rrule == null) {
            return false;
        }
        Frequency frequency = rrule.getFreq();
        return frequency == WEEKLY && !rrule.getByDay().isEmpty() ||
                frequency == HOURLY ||
                frequency == MINUTELY ||
                rrule.getUntil() != null ||
                rrule.getInterval() != 1 ||
                rrule.getCount() != 0;
    }

    @OnClick(R.id.display_row_edit)
    void openPopup(View view) {
        boolean customPicked = isCustomValue();
        List<String> repeatOptions = newArrayList(context.getResources().getStringArray(R.array.repeat_options));
        SingleCheckedArrayAdapter adapter = new SingleCheckedArrayAdapter(context, repeatOptions, theme.getThemeAccent());
        if (customPicked) {
            adapter.insert(getRepeatString(), 0);
            adapter.setChecked(0);
        } else if (rrule == null) {
            adapter.setChecked(0);
        } else {
            int selected;
            switch (rrule.getFreq()) {
                case DAILY:
                    selected = 1;
                    break;
                case WEEKLY:
                    selected = 2;
                    break;
                case MONTHLY:
                    selected = 3;
                    break;
                case YEARLY:
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
                        rrule = null;
                    } else if (i == 5) {
                        newCustomRecurrenceDialog(this, rrule)
                                .show(getFragmentManager(), FRAG_TAG_CUSTOM_RECURRENCE);
                        return;
                    } else {
                        rrule = new RRule();
                        rrule.setInterval(1);
                        repeatAfterCompletion = false;

                        switch (i) {
                            case 1:
                                rrule.setFreq(DAILY);
                                break;
                            case 2:
                                rrule.setFreq(WEEKLY);
                                break;
                            case 3:
                                rrule.setFreq(MONTHLY);
                                break;
                            case 4:
                                rrule.setFreq(YEARLY);
                                break;
                        }

                        tracker.reportEvent(Tracking.Events.RECURRENCE_PRESET, rrule.toIcal());
                    }

                    callback.repeatChanged(rrule != null);

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
        try {
            rrule = new RRule(task.getRecurrenceWithoutFrom());
            rrule.setUntil(new DateTime(task.getRepeatUntil()).toDateValue());
        } catch (ParseException e) {
            rrule = null;
        }
    }

    @Override
    public boolean hasChanges(Task original) {
        return !getRecurrenceValue().equals(original.getRecurrence()) ||
                original.getRepeatUntil() != (rrule == null ? 0 : DateTime.from(rrule.getUntil()).getMillis());
    }

    @Override
    public void apply(Task task) {
        task.setRepeatUntil(rrule == null ? 0 : DateTime.from(rrule.getUntil()).getMillis());
        task.setRecurrence(getRecurrenceValue());
    }

    private String getRecurrenceValue() {
        if (rrule == null) {
            return "";
        }
        RRule copy;
        try {
            copy = new RRule(rrule.toIcal());
        } catch (ParseException e) {
            return "";
        }
        copy.setUntil(null);
        String result = copy.toIcal();
        if (repeatAfterCompletion && !TextUtils.isEmpty(result)) {
            result += ";FROM=COMPLETION"; //$NON-NLS-1$
        }

        return result;
    }

    private void refreshDisplayView() {
        if (rrule == null) {
            displayView.setText(R.string.repeat_option_does_not_repeat);
            displayView.setTextColor(getColor(context, R.color.text_tertiary));
            repeatTypeContainer.setVisibility(View.GONE);
        } else {
            displayView.setText(getRepeatString());
            displayView.setTextColor(getColor(context, R.color.text_primary));
            repeatTypeContainer.setVisibility(View.VISIBLE);
        }
    }

    private String getRepeatString() {
        int interval = rrule.getInterval();
        Frequency frequency = rrule.getFreq();
        DateTime repeatUntil = rrule.getUntil() == null ? null : DateTime.from(rrule.getUntil());
        int count = rrule.getCount();
        String countString = count > 0 ? getContext().getResources().getQuantityString(R.plurals.repeat_times, count) : "";
        if (interval == 1) {
            String frequencyString = getString(getSingleFrequencyResource(frequency));
            if (frequency == WEEKLY && !rrule.getByDay().isEmpty()) {
                String dayString = getDayString();
                if (count > 0) {
                    return getString(R.string.repeats_single_on_number_of_times, frequencyString, dayString, count, countString);
                } else if (repeatUntil == null) {
                    return getString(R.string.repeats_single_on, frequencyString, dayString);
                } else {
                    return getString(R.string.repeats_single_on_until, frequencyString, dayString, DateUtilities.getLongDateString(repeatUntil));
                }
            } else if (count > 0) {
                return getString(R.string.repeats_single_number_of_times, frequencyString, count, countString);
            } else if (repeatUntil == null) {
                return getString(R.string.repeats_single, frequencyString);
            } else {
                return getString(R.string.repeats_single_until, frequencyString, DateUtilities.getLongDateString(repeatUntil));
            }
        } else {
            int plural = getFrequencyPlural(frequency);
            String frequencyPlural = getResources().getQuantityString(plural, interval, interval);
            if (frequency == WEEKLY && !rrule.getByDay().isEmpty()) {
                String dayString = getDayString();
                if (count > 0) {
                    return getString(R.string.repeats_plural_on_number_of_times, frequencyPlural, dayString, count, countString);
                } else if (repeatUntil == null) {
                    return getString(R.string.repeats_plural_on, frequencyPlural, dayString);
                } else {
                    return getString(R.string.repeats_plural_on_until, frequencyPlural, dayString, DateUtilities.getLongDateString(repeatUntil));
                }
            } else if (count > 0) {
                return getString(R.string.repeats_plural_number_of_times, frequencyPlural, count, countString);
            } else if (repeatUntil == null) {
                return getString(R.string.repeats_plural, frequencyPlural);
            } else {
                return getString(R.string.repeats_plural_until, frequencyPlural, DateUtilities.getLongDateString(repeatUntil));
            }
        }
    }

    private String getDayString() {
        DateFormatSymbols dfs = new DateFormatSymbols(locale.getLocale());
        String[] shortWeekdays = dfs.getShortWeekdays();
        List<String> days = new ArrayList<>();
        for (WeekdayNum weekday : rrule.getByDay()) {
            days.add(shortWeekdays[WEEKDAYS.indexOf(weekday.wday) + 1]);
        }
        return Joiner.on(getString(R.string.list_separator_with_space)).join(days);
    }

    private int getSingleFrequencyResource(Frequency frequency) {
        switch (frequency) {
            case MINUTELY:
                return R.string.repeats_minutely;
            case HOURLY:
                return R.string.repeats_hourly;
            case DAILY:
                return R.string.repeats_daily;
            case WEEKLY:
                return R.string.repeats_weekly;
            case MONTHLY:
                return R.string.repeats_monthly;
            case YEARLY:
                return R.string.repeats_yearly;
            default:
                throw new RuntimeException("Invalid frequency: " + frequency);
        }
    }

    private int getFrequencyPlural(Frequency frequency) {
        switch (frequency) {
            case MINUTELY:
                return R.plurals.repeat_n_minutes;
            case HOURLY:
                return R.plurals.repeat_n_hours;
            case DAILY:
                return R.plurals.repeat_n_days;
            case WEEKLY:
                return R.plurals.repeat_n_weeks;
            case MONTHLY:
                return R.plurals.repeat_n_months;
            case YEARLY:
                return R.plurals.repeat_n_years;
            default:
                throw new RuntimeException("Invalid frequency: " + frequency);
        }
    }
}
