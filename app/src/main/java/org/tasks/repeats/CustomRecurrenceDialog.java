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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.drawables.CheckableDrawable;
import com.appeaser.sublimepickerlibrary.recurrencepicker.WeekButton;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;

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
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_DAYS;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_HOURS;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_MINUTES;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_MONTHS;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_WEEKS;
import static com.todoroo.astrid.repeats.RepeatControlSet.FREQUENCY_YEARS;
import static com.todoroo.astrid.repeats.RepeatControlSet.TYPE_COMPLETION_DATE;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class CustomRecurrenceDialog extends InjectingDialogFragment {

    public static CustomRecurrenceDialog newCustomRecurrenceDialog(Fragment target) {
        CustomRecurrenceDialog dialog = new CustomRecurrenceDialog();
        dialog.setTargetFragment(target, 0);
        return dialog;
    }

    public interface CustomRecurrenceCallback {
        void onSelected(int frequency, int interval, long repeatUntilValue, boolean[] isChecked);
    }

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
    @BindView(R.id.repeatValue) EditText intervalEditText;
    @BindView(R.id.intervalText) TextView intervalTextView;

    private ArrayAdapter<String> repeatUntilAdapter;
    private final List<String> repeatUntilOptions = new ArrayList<>();
    private final boolean[] isChecked = new boolean[7];

    private int frequency;
    private int interval;
    private long repeatUntilValue;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.control_set_repeat, null);

        ButterKnife.bind(this, dialogView);

        ArrayAdapter<CharSequence> frequencyAdapter = ArrayAdapter.createFromResource(context, R.array.repeat_frequency, R.layout.frequency_item);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(frequencyAdapter);
        frequencySpinner.setSelection(3);
        intervalEditText.setText(locale.formatNumber(1));
        intervalEditText.setSelectAllOnFocus(true);
        intervalEditText.selectAll();

        repeatUntilAdapter = new ArrayAdapter<>(context, R.layout.simple_spinner_item, repeatUntilOptions);
        repeatUntilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatUntilSpinner.setAdapter(repeatUntilAdapter);

        setInterval(1, true);

        setRepeatUntilValue(repeatUntilValue);

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
            final int index = i;
            WeekButton weekButton = weekButtons[index];
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String text = shortWeekdays[dayOfWeek];
            weekButton.setBackgroundDrawable(new CheckableDrawable(accent.getAccentColor(), false, expandedWidthHeight));
            weekButton.setTextColor(weekButtonUnselectedTextColor);
            weekButton.setTextOff(text);
            weekButton.setTextOn(text);
            weekButton.setText(text);
            weekButton.setOnCheckedChangeListener((compoundButton, b) -> CustomRecurrenceDialog.this.isChecked[index] = b);
            calendar.add(Calendar.DATE, 1);
        }

        return dialogBuilder.newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog12, which) ->
                        ((CustomRecurrenceCallback) getTargetFragment())
                                .onSelected(frequency, interval, repeatUntilValue, isChecked))
                .setNegativeButton(android.R.string.cancel, null)
                .setOnCancelListener(DialogInterface::dismiss)
                .show();
    }

    private void setInterval(int interval, boolean updateEditText) {
        this.interval = interval;
        if (updateEditText) {
            intervalEditText.setText(locale.formatNumber(interval));
        }
        updateIntervalTextView();
    }

    private void updateIntervalTextView() {
        int resource = getFrequencyPlural();
        String quantityString = getResources().getQuantityString(resource, interval);
        intervalTextView.setText(quantityString);
    }

    private int getFrequencyPlural() {
        switch (frequency) {
            case FREQUENCY_MINUTES:
                return R.plurals.repeat_minutes;
            case FREQUENCY_HOURS:
                return R.plurals.repeat_hours;
            case FREQUENCY_DAYS:
                return R.plurals.repeat_days;
            case FREQUENCY_WEEKS:
                return R.plurals.repeat_weeks;
            case FREQUENCY_MONTHS:
                return R.plurals.repeat_months;
            case FREQUENCY_YEARS:
                return R.plurals.repeat_years;
            default:
                throw new RuntimeException("Invalid frequency: " + frequency);
        }
    }

    @OnItemSelected(R.id.repeat_until)
    public void onRepeatUntilChanged(Spinner spinner, int position) {
        if (repeatUntilOptions.size() == 2) {
            if (position == 0) {
                setRepeatUntilValue(0);
            } else {
                repeatUntilClick();
            }
        } else {
            if (position == 1) {
                setRepeatUntilValue(0);
            } else if (position == 2) {
                repeatUntilClick();
            }
        }
    }

    @OnItemSelected(R.id.frequency)
    public void onFrequencyChanged(Spinner spinner, int position) {
        int weekVisibility = position == RepeatControlSet.FREQUENCY_WEEKS ? View.VISIBLE : View.GONE;
        weekGroup1.setVisibility(weekVisibility);
        if (weekGroup2 != null) {
            weekGroup2.setVisibility(weekVisibility);
        }
        frequency = position;
        updateIntervalTextView();
    }

    @OnTextChanged(R.id.repeatValue)
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

    private void setRepeatUntilValue(long newValue) {
        repeatUntilValue = newValue;
        updateRepeatUntilOptions();
    }

    private void repeatUntilClick() {
        Intent intent = new Intent(context, DatePickerActivity.class);
        intent.putExtra(DatePickerActivity.EXTRA_TIMESTAMP, repeatUntilValue > 0 ? repeatUntilValue : 0L);
        startActivityForResult(intent, REQUEST_PICK_DATE);
    }

    private void updateRepeatUntilOptions() {
        repeatUntilOptions.clear();
        if (repeatUntilValue > 0) {
            repeatUntilOptions.add(getString(R.string.repeat_until, getDisplayString(context, repeatUntilValue)));
        }
        repeatUntilOptions.add(getString(R.string.repeat_forever));
        repeatUntilOptions.add(getString(R.string.repeat_until, "").trim());
        repeatUntilAdapter.notifyDataSetChanged();
        repeatUntilSpinner.setSelection(0);
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
