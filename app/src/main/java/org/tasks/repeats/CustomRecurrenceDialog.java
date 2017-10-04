package org.tasks.repeats;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.drawables.CheckableDrawable;
import com.appeaser.sublimepickerlibrary.recurrencepicker.WeekButton;
import com.google.common.base.Strings;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.activities.DatePickerActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;
import org.tasks.preferences.ResourceResolver;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeAccent;
import org.tasks.time.DateTime;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
import static com.todoroo.astrid.repeats.RepeatControlSet.WEEKDAYS;
import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class CustomRecurrenceDialog extends InjectingDialogFragment {

    public static CustomRecurrenceDialog newCustomRecurrenceDialog(Fragment target, RRule rrule) {
        CustomRecurrenceDialog dialog = new CustomRecurrenceDialog();
        dialog.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        if (rrule != null) {
            arguments.putString(EXTRA_RRULE, rrule.toIcal());
        }
        dialog.setArguments(arguments);
        return dialog;
    }

    public interface CustomRecurrenceCallback {
        void onSelected(RRule rrule);
    }

    private static final List<Frequency> FREQUENCIES = asList(MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY);

    private static final String EXTRA_RRULE = "extra_rrule";
    private static final int REQUEST_PICK_DATE = 505;

    @Inject @ForActivity Context context;
    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;
    @Inject Locale locale;

    @BindView(R.id.weekGroup) LinearLayout weekGroup1;
    @BindView(R.id.weekGroup2) @Nullable LinearLayout weekGroup2;
    @BindView(R.id.week_day_1) WeekButton day1;
    @BindView(R.id.week_day_2) WeekButton day2;
    @BindView(R.id.week_day_3) WeekButton day3;
    @BindView(R.id.week_day_4) WeekButton day4;
    @BindView(R.id.week_day_5) WeekButton day5;
    @BindView(R.id.week_day_6) WeekButton day6;
    @BindView(R.id.week_day_7) WeekButton day7;

    @BindView(R.id.repeat_until) Spinner repeatUntilSpinner;
    @BindView(R.id.frequency) Spinner frequencySpinner;
    @BindView(R.id.intervalValue) EditText intervalEditText;
    @BindView(R.id.intervalText) TextView intervalTextView;
    @BindView(R.id.repeatTimesValue) EditText repeatTimes;
    @BindView(R.id.repeatTimesText) TextView repeatTimesText;

    private ArrayAdapter<String> repeatUntilAdapter;
    private final List<String> repeatUntilOptions = new ArrayList<>();

    private RRule rrule;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.control_set_repeat, null);

        Bundle arguments = getArguments();
        String rule = arguments.getString(EXTRA_RRULE);
        if (!Strings.isNullOrEmpty(rule)) {
            try {
                rrule = new RRule(rule);
            } catch (Exception ignored) {
            }
        }
        if (rrule == null) {
            rrule = new RRule();
            rrule.setInterval(1);
            rrule.setFreq(WEEKLY);
        }

        ButterKnife.bind(this, dialogView);

        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(context, R.array.repeat_frequency, R.layout.frequency_item);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(frequencyAdapter);
        frequencySpinner.setSelection(FREQUENCIES.indexOf(rrule.getFreq()));

        intervalEditText.setText(locale.formatNumber(rrule.getInterval()));

        repeatUntilAdapter = new ArrayAdapter<String>(context, R.layout.simple_spinner_item, repeatUntilOptions) {
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.simple_spinner_dropdown_item, parent, false);
                ((TextView) vg.findViewById(R.id.text1)).setText(getItem(position));
                return vg;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setPadding(0, 0, 0, 0);
                tv.setText(repeatUntilOptions.get(selectedItemPosition));
                return tv;
            }
        };
        repeatUntilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatUntilSpinner.setAdapter(repeatUntilAdapter);
        updateRepeatUntilOptions();

        WeekButton[] weekButtons = new WeekButton[] { day1, day2, day3, day4, day5, day6, day7 };
        int expandedWidthHeight = getResources()
                .getDimensionPixelSize(R.dimen.week_button_state_on_circle_size);

        int weekButtonUnselectedTextColor = getColor(context, R.color.text_primary);
        int weekButtonSelectedTextColor = ResourceResolver.getData(context, R.attr.fab_text);
        WeekButton.setStateColors(weekButtonUnselectedTextColor, weekButtonSelectedTextColor);

        // set up days of week
        ThemeAccent accent = theme.getThemeAccent();
        DateFormatSymbols dfs = new DateFormatSymbols(locale.getLocale());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        String[] shortWeekdays = dfs.getShortWeekdays();

        for(int i = 0; i < 7; i++) {
            String text = shortWeekdays[calendar.get(Calendar.DAY_OF_WEEK)];
            WeekdayNum weekdayNum = new WeekdayNum(0, WEEKDAYS.get(i));
            WeekButton weekButton = weekButtons[i];
            weekButton.setBackgroundDrawable(new CheckableDrawable(accent.getAccentColor(), false, expandedWidthHeight));
            weekButton.setTextColor(weekButtonUnselectedTextColor);
            weekButton.setTextOff(text);
            weekButton.setTextOn(text);
            weekButton.setText(text);
            if (rrule.getByDay().contains(weekdayNum)) {
                weekButton.setChecked(true);
            }
            weekButton.setOnCheckedChangeListener((compoundButton, b) -> {
                List<WeekdayNum> days = rrule.getByDay();
                if (b) {
                    days.add(weekdayNum);
                } else {
                    days.remove(weekdayNum);
                }
            });
            calendar.add(Calendar.DATE, 1);
        }

        return dialogBuilder.newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog12, which) -> {
                    if (rrule.getFreq() != WEEKLY) {
                        rrule.setByDay(Collections.emptyList());
                    }
                    ((CustomRecurrenceCallback) getTargetFragment()).onSelected(rrule);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnCancelListener(DialogInterface::dismiss)
                .show();
    }

    private void setFrequency(Frequency frequency) {
        rrule.setFreq(frequency);
        int weekVisibility = frequency == WEEKLY ? View.VISIBLE : View.GONE;
        weekGroup1.setVisibility(weekVisibility);
        if (weekGroup2 != null) {
            weekGroup2.setVisibility(weekVisibility);
        }
        updateIntervalTextView();
    }

    private void setInterval(int interval, boolean updateEditText) {
        rrule.setInterval(interval);
        if (updateEditText) {
            intervalEditText.setText(locale.formatNumber(interval));
        }
        updateIntervalTextView();
    }

    private void updateIntervalTextView() {
        int resource = getFrequencyPlural();
        String quantityString = getResources().getQuantityString(resource, rrule.getInterval());
        intervalTextView.setText(quantityString);
    }

    private void setCount(int count, boolean updateEditText) {
        rrule.setCount(count);
        if (updateEditText) {
            intervalEditText.setText(locale.formatNumber(count));
        }
        updateCountText();
    }

    private void updateCountText() {
        repeatTimesText.setText(getResources().getQuantityString(R.plurals.repeat_times, rrule.getCount()));
    }

    private int getFrequencyPlural() {
        switch (rrule.getFreq()) {
            case MINUTELY:
                return R.plurals.repeat_minutes;
            case HOURLY:
                return R.plurals.repeat_hours;
            case DAILY:
                return R.plurals.repeat_days;
            case WEEKLY:
                return R.plurals.repeat_weeks;
            case MONTHLY:
                return R.plurals.repeat_months;
            case YEARLY:
                return R.plurals.repeat_years;
            default:
                throw new RuntimeException("Invalid frequency: " + rrule.getFreq());
        }
    }

    @OnItemSelected(R.id.repeat_until)
    public void onRepeatUntilChanged(int position) {
        if (repeatUntilOptions.size() == 4) {
            position--;
        }
        if (position == 0) {
            rrule.setUntil(null);
            rrule.setCount(0);
            updateRepeatUntilOptions();
        } else if (position == 1) {
            repeatUntilClick();
        } else if (position == 2) {
            rrule.setUntil(null);
            rrule.setCount(Math.max(rrule.getCount(), 1));
            updateRepeatUntilOptions();
        }
    }

    @OnItemSelected(R.id.frequency)
    public void onFrequencyChanged(int position) {
        setFrequency(FREQUENCIES.get(position));
    }

    @OnTextChanged(R.id.intervalValue)
    public void onRepeatValueChanged(CharSequence text) {
        Integer value = locale.parseInteger(text.toString());
        if (value == null) {
            return;
        }
        if (value < 1) {
            setInterval(1, true);
        } else {
            setInterval(value, false);
        }
    }

    @OnTextChanged(R.id.repeatTimesValue)
    public void onRepeatTimesValueChanged(CharSequence text) {
        Integer value = locale.parseInteger(text.toString());
        if (value == null) {
            return;
        }
        if (value < 1) {
            setCount(1, true);
        } else {
            setCount(value, false);
        }
    }

    private void repeatUntilClick() {
        Intent intent = new Intent(context, DatePickerActivity.class);
        long repeatUntil = DateTime.from(rrule.getUntil()).getMillis();
        intent.putExtra(DatePickerActivity.EXTRA_TIMESTAMP, repeatUntil > 0 ? repeatUntil : 0L);
        startActivityForResult(intent, REQUEST_PICK_DATE);
    }

    private void updateRepeatUntilOptions() {
        repeatUntilOptions.clear();
        long repeatUntil = DateTime.from(rrule.getUntil()).getMillis();
        int count = rrule.getCount();
        if (repeatUntil > 0) {
            repeatUntilOptions.add(getString(R.string.repeat_until, getDisplayString(context, repeatUntil)));
            repeatTimes.setVisibility(View.GONE);
            repeatTimesText.setVisibility(View.GONE);
        } else if (count > 0) {
            repeatUntilOptions.add(getString(R.string.repeat_occurs));
            repeatTimes.setText(locale.formatNumber(count));
            repeatTimes.setVisibility(View.VISIBLE);
            updateCountText();
            repeatTimesText.setVisibility(View.VISIBLE);
        } else {
            repeatTimes.setVisibility(View.GONE);
            repeatTimesText.setVisibility(View.GONE);
        }
        repeatUntilOptions.add(getString(R.string.repeat_forever));
        repeatUntilOptions.add(getString(R.string.repeat_until, "").trim());
        repeatUntilOptions.add(getString(R.string.repeat_number_of_times));
        repeatUntilAdapter.notifyDataSetChanged();
        repeatUntilSpinner.setSelection(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                rrule.setUntil(new DateTime(data.getLongExtra(DatePickerActivity.EXTRA_TIMESTAMP, 0L)).toDateValue());
                rrule.setCount(0);
            }
            updateRepeatUntilOptions();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    private static String getDisplayString(Context context, long repeatUntilValue) {
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
