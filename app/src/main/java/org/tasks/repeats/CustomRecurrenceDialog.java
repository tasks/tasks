package org.tasks.repeats;

import static android.app.Activity.RESULT_OK;
import static com.google.common.collect.Lists.newArrayList;
import static net.fortuna.ical4j.model.Recur.Frequency.DAILY;
import static net.fortuna.ical4j.model.Recur.Frequency.HOURLY;
import static net.fortuna.ical4j.model.Recur.Frequency.MINUTELY;
import static net.fortuna.ical4j.model.Recur.Frequency.MONTHLY;
import static net.fortuna.ical4j.model.Recur.Frequency.WEEKLY;
import static net.fortuna.ical4j.model.Recur.Frequency.YEARLY;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.dialogs.MyDatePickerDialog.newDatePicker;
import static org.tasks.repeats.BasicRecurrenceDialog.EXTRA_RRULE;
import static org.tasks.repeats.RecurrenceUtils.newRecur;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static java.util.Arrays.asList;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.todoroo.andlib.utility.DateUtilities;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Frequency;
import net.fortuna.ical4j.model.WeekDay;

import org.tasks.R;
import org.tasks.databinding.ControlSetRepeatBinding;
import org.tasks.databinding.WeekButtonsBinding;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.extensions.LocaleKt;
import org.tasks.preferences.ResourceResolver;
import org.tasks.time.DateTime;
import org.tasks.ui.OnItemSelected;
import org.tasks.ui.OnTextChanged;

import java.text.DateFormatSymbols;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class CustomRecurrenceDialog extends DialogFragment {

  private static final List<Frequency> FREQUENCIES =
      asList(MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY);
  private static final String EXTRA_DATE = "extra_date";
  private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
  private static final int REQUEST_PICK_DATE = 505;
  private final List<String> repeatUntilOptions = new ArrayList<>();
  @Inject Activity context;
  @Inject DialogBuilder dialogBuilder;
  @Inject Locale locale;

  private LinearLayout weekGroup1;
  @Nullable private LinearLayout weekGroup2;
  private RadioGroup monthGroup;
  private RadioButton repeatMonthlyDayOfNthWeek;
  private RadioButton repeatMonthlyDayOfLastWeek;
  private Spinner repeatUntilSpinner;
  private EditText intervalEditText;
  private TextView intervalTextView;
  private EditText repeatTimes;
  private TextView repeatTimesText;

  private ArrayAdapter<String> repeatUntilAdapter;
  private ToggleButton[] weekButtons;
  private Recur rrule;
  private long dueDate;

  public static CustomRecurrenceDialog newCustomRecurrenceDialog(
      Fragment target, int rc, String rrule, long dueDate) {
    CustomRecurrenceDialog dialog = new CustomRecurrenceDialog();
    dialog.setTargetFragment(target, rc);
    Bundle arguments = new Bundle();
    if (rrule != null) {
      arguments.putString(EXTRA_RRULE, rrule);
    }
    arguments.putLong(EXTRA_DATE, dueDate);
    dialog.setArguments(arguments);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = LayoutInflater.from(context);
    ControlSetRepeatBinding binding = ControlSetRepeatBinding.inflate(inflater);
    WeekButtonsBinding weekBinding = WeekButtonsBinding.bind(binding.getRoot());

    Bundle arguments = getArguments();
    dueDate = arguments.getLong(EXTRA_DATE, currentTimeMillis());
    String rule =
        savedInstanceState == null
            ? arguments.getString(EXTRA_RRULE)
            : savedInstanceState.getString(EXTRA_RRULE);
    try {
      if (!isNullOrEmpty(rule)) {
        rrule = newRecur(rule);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    if (rrule == null) {
      rrule = newRecur();
      rrule.setInterval(1);
      rrule.setFrequency(WEEKLY.name());
    }

    DateFormatSymbols dfs = new DateFormatSymbols(locale);
    String[] shortWeekdays = dfs.getShortWeekdays();

    weekGroup1 = weekBinding.weekGroup;
    weekGroup2 = weekBinding.weekGroup2;
    monthGroup = binding.monthGroup;
    repeatMonthlyDayOfNthWeek = binding.repeatMonthlyDayOfNthWeek;
    repeatMonthlyDayOfLastWeek = binding.repeatMonthlyDayOfLastWeek;
    repeatUntilSpinner = binding.repeatUntil;
    repeatUntilSpinner.setOnItemSelectedListener(new OnItemSelected() {
      @Override
      public void onItemSelected(int position) {
        onRepeatUntilChanged(position);
      }
    });
    intervalEditText = binding.intervalValue;
    intervalEditText.addTextChangedListener(new OnTextChanged() {
      @Override
      public void onTextChanged(@Nullable CharSequence text) {
        if (text != null) {
          onRepeatValueChanged(text);
        }
      }
    });
    intervalTextView = binding.intervalText;
    repeatTimes = binding.repeatTimesValue;
    repeatTimes.addTextChangedListener(new OnTextChanged() {
      @Override
      public void onTextChanged(@Nullable CharSequence text) {
        if (text != null) {
          onRepeatTimesValueChanged(text);
        }
      }
    });
    repeatTimesText = binding.repeatTimesText;
    AppCompatSpinner frequency = binding.frequency;
    frequency.setOnItemSelectedListener(new OnItemSelected() {
      @Override
      public void onItemSelected(int position) {
        onFrequencyChanged(position);
      }
    });

    Calendar dayOfMonthCalendar = Calendar.getInstance(locale);
    dayOfMonthCalendar.setTimeInMillis(dueDate);
    int dayOfWeekInMonth = dayOfMonthCalendar.get(Calendar.DAY_OF_WEEK_IN_MONTH);
    int maxDayOfWeekInMonth = dayOfMonthCalendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

    int dueDayOfWeek = dayOfMonthCalendar.get(Calendar.DAY_OF_WEEK);
    String today = dfs.getWeekdays()[dueDayOfWeek];
    if (dayOfWeekInMonth == maxDayOfWeekInMonth) {
      repeatMonthlyDayOfLastWeek.setVisibility(View.VISIBLE);
      String last = getString(R.string.repeat_monthly_last_week);
      String text = getString(R.string.repeat_monthly_on_every_day_of_nth_week, last, today);
      repeatMonthlyDayOfLastWeek.setTag(new WeekDay(calendarDayToWeekday(dueDayOfWeek), -1));
      repeatMonthlyDayOfLastWeek.setText(text);
    } else {
      repeatMonthlyDayOfLastWeek.setVisibility(View.GONE);
    }

    if (dayOfWeekInMonth < 6) {
      int[] resources =
          new int[] {
            R.string.repeat_monthly_first_week,
            R.string.repeat_monthly_second_week,
            R.string.repeat_monthly_third_week,
            R.string.repeat_monthly_fourth_week,
            R.string.repeat_monthly_fifth_week,
          };
      repeatMonthlyDayOfNthWeek.setVisibility(View.VISIBLE);
      String nth = getString(resources[dayOfWeekInMonth - 1]);
      String text = getString(R.string.repeat_monthly_on_every_day_of_nth_week, nth, today);
      repeatMonthlyDayOfNthWeek.setTag(
          new WeekDay(calendarDayToWeekday(dueDayOfWeek), dayOfWeekInMonth));
      repeatMonthlyDayOfNthWeek.setText(text);
    } else {
      repeatMonthlyDayOfNthWeek.setVisibility(View.GONE);
    }

    if (rrule.getFrequency() == MONTHLY) {
      if (rrule.getDayList().size() == 1) {
        WeekDay weekday = rrule.getDayList().get(0);
        if (weekday.getOffset() == -1) {
          repeatMonthlyDayOfLastWeek.setChecked(true);
        } else if (weekday.getOffset() == dayOfWeekInMonth) {
          repeatMonthlyDayOfNthWeek.setChecked(true);
        }
      }
    }
    if (monthGroup.getCheckedRadioButtonId() != R.id.repeat_monthly_day_of_last_week
        && monthGroup.getCheckedRadioButtonId() != R.id.repeat_monthly_day_of_nth_week) {
      binding.repeatMonthlySameDay.setChecked(true);
    }

    ArrayAdapter<CharSequence> frequencyAdapter =
        ArrayAdapter.createFromResource(context, R.array.repeat_frequency, R.layout.frequency_item);
    frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    frequency.setAdapter(frequencyAdapter);
    frequency.setSelection(FREQUENCIES.indexOf(rrule.getFrequency()));

    intervalEditText.setText(LocaleKt.formatNumber(locale, rrule.getInterval()));

    repeatUntilAdapter =
        new ArrayAdapter<String>(context, 0, repeatUntilOptions) {
          @Override
          public View getDropDownView(
              int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewGroup vg =
                (ViewGroup) inflater.inflate(R.layout.simple_spinner_dropdown_item, parent, false);
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
            TextView tv =
                (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            tv.setPadding(0, 0, 0, 0);
            tv.setText(repeatUntilOptions.get(selectedItemPosition));
            return tv;
          }
        };
    repeatUntilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    repeatUntilSpinner.setAdapter(repeatUntilAdapter);
    updateRepeatUntilOptions();

    weekButtons = new ToggleButton[]{
            weekBinding.weekDay1.button,
            weekBinding.weekDay2.button,
            weekBinding.weekDay3.button,
            weekBinding.weekDay4.button,
            weekBinding.weekDay5.button,
            weekBinding.weekDay6.button,
            weekBinding.weekDay7.button
    };

    // set up days of week
    Calendar dayOfWeekCalendar = Calendar.getInstance(locale);
    dayOfWeekCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeekCalendar.getFirstDayOfWeek());

    WeekDay todayWeekday = new WeekDay(new DateTime(dueDate).getWeekDay(), 0);

    ColorStateList colorStateList =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked}, new int[] {-android.R.attr.state_checked}
            },
            new int[] {
              ResourceResolver.getData(context, R.attr.colorOnSecondary),
              context.getColor(R.color.text_primary)
            });
    int inset = (int) context.getResources().getDimension(R.dimen.week_button_inset);
    int accentColor = ResourceResolver.getData(context, R.attr.colorAccent);
    int animationDuration =
        context.getResources().getInteger(android.R.integer.config_shortAnimTime);

    for (int i = 0; i < 7; i++) {
      ToggleButton weekButton = weekButtons[i];

      GradientDrawable ovalDrawable =
          (GradientDrawable)
              context.getDrawable(R.drawable.week_day_button_oval).mutate();
      ovalDrawable.setColor(accentColor);
      LayerDrawable layerDrawable = new LayerDrawable(new Drawable[] {ovalDrawable});
      layerDrawable.setLayerInset(0, inset, inset, inset, inset);
      StateListDrawable stateListDrawable = new StateListDrawable();
      stateListDrawable.setEnterFadeDuration(animationDuration);
      stateListDrawable.setExitFadeDuration(animationDuration);
      stateListDrawable.addState(
          new int[] {-android.R.attr.state_checked}, new ColorDrawable(Color.TRANSPARENT));
      stateListDrawable.addState(new int[] {android.R.attr.state_checked}, layerDrawable);
      int paddingBottom = weekButton.getPaddingBottom();
      int paddingTop = weekButton.getPaddingTop();
      int paddingLeft = weekButton.getPaddingLeft();
      int paddingRight = weekButton.getPaddingRight();
      weekButton.setBackground(stateListDrawable);
      weekButton.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

      int dayOfWeek = dayOfWeekCalendar.get(Calendar.DAY_OF_WEEK);
      String text = shortWeekdays[dayOfWeek];
      weekButton.setTextColor(colorStateList);
      weekButton.setTextOn(text);
      weekButton.setTextOff(text);
      weekButton.setTag(new WeekDay(calendarDayToWeekday(dayOfWeek), 0));
      if (savedInstanceState == null) {
        weekButton.setChecked(
            rrule.getFrequency() != WEEKLY || rrule.getDayList().isEmpty()
                ? todayWeekday.equals(weekButton.getTag())
                : rrule.getDayList().contains(weekButton.getTag()));
      }
      dayOfWeekCalendar.add(Calendar.DATE, 1);
    }

    setCancelable(false);

    return dialogBuilder
        .newDialog()
        .setView(binding.getRoot())
        .setPositiveButton(R.string.ok, this::onRuleSelected)
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  private void onRuleSelected(DialogInterface dialogInterface, int which) {
    if (rrule.getFrequency() == WEEKLY) {
      List<WeekDay> checked = new ArrayList<>();
      for (ToggleButton weekButton : weekButtons) {
        if (weekButton.isChecked()) {
          checked.add((WeekDay) weekButton.getTag());
        }
      }
      rrule.getDayList().clear();
      rrule.getDayList().addAll(checked);
    } else if (rrule.getFrequency() == MONTHLY) {
      switch (monthGroup.getCheckedRadioButtonId()) {
        case R.id.repeat_monthly_same_day:
          rrule.getDayList().clear();
          break;
        case R.id.repeat_monthly_day_of_nth_week:
          rrule.getDayList().clear();
          rrule.getDayList().addAll(newArrayList((WeekDay) repeatMonthlyDayOfNthWeek.getTag()));
          break;
        case R.id.repeat_monthly_day_of_last_week:
          rrule.getDayList().clear();
          rrule.getDayList().addAll(newArrayList((WeekDay) repeatMonthlyDayOfLastWeek.getTag()));
          break;
      }
    } else {
      rrule.getDayList().clear();
    }
    Intent intent = new Intent();
    intent.putExtra(EXTRA_RRULE, rrule.toString());
    getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, intent);
    dismiss();
  }

  private WeekDay calendarDayToWeekday(int calendarDay) {
    switch (calendarDay) {
      case Calendar.SUNDAY:
        return WeekDay.SU;
      case Calendar.MONDAY:
        return WeekDay.MO;
      case Calendar.TUESDAY:
        return WeekDay.TU;
      case Calendar.WEDNESDAY:
        return WeekDay.WE;
      case Calendar.THURSDAY:
        return WeekDay.TH;
      case Calendar.FRIDAY:
        return WeekDay.FR;
      case Calendar.SATURDAY:
        return WeekDay.SA;
    }
    throw new RuntimeException("Invalid calendar day: " + calendarDay);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_RRULE, rrule.toString());
  }

  private void setInterval(int interval, boolean updateEditText) {
    rrule.setInterval(interval);
    if (updateEditText) {
      intervalEditText.setText(LocaleKt.formatNumber(locale, interval));
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
      intervalEditText.setText(LocaleKt.formatNumber(locale, count));
    }
    updateCountText();
  }

  private void updateCountText() {
    repeatTimesText.setText(
        getResources().getQuantityString(R.plurals.repeat_times, rrule.getCount()));
  }

  private int getFrequencyPlural() {
    switch (rrule.getFrequency()) {
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
        throw new RuntimeException("Invalid frequency: " + rrule.getFrequency());
    }
  }

  private void onRepeatUntilChanged(int position) {
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

  private void onFrequencyChanged(int position) {
    Frequency frequency = FREQUENCIES.get(position);
    rrule.setFrequency(frequency.name());
    int weekVisibility = frequency == WEEKLY ? View.VISIBLE : View.GONE;
    weekGroup1.setVisibility(weekVisibility);
    if (weekGroup2 != null) {
      weekGroup2.setVisibility(weekVisibility);
    }
    monthGroup.setVisibility(frequency == MONTHLY && dueDate >= 0 ? View.VISIBLE : View.GONE);
    updateIntervalTextView();
  }

  private void onRepeatValueChanged(CharSequence text) {
    Integer value = LocaleKt.parseInteger(locale, text.toString());
    if (value == null) {
      return;
    }
    if (value < 1) {
      setInterval(1, true);
    } else {
      setInterval(value, false);
    }
  }

  private void onRepeatTimesValueChanged(CharSequence text) {
    Integer value = LocaleKt.parseInteger(locale, text.toString());
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
    if (getParentFragmentManager().findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
      long repeatUntil = DateTime.from(rrule.getUntil()).getMillis();
      newDatePicker(this, REQUEST_PICK_DATE, repeatUntil > 0 ? repeatUntil : 0L)
          .show(getParentFragmentManager(), FRAG_TAG_DATE_PICKER);
    }
  }

  private void updateRepeatUntilOptions() {
    repeatUntilOptions.clear();
    long repeatUntil = DateTime.from(rrule.getUntil()).getMillis();
    int count = rrule.getCount();
    if (repeatUntil > 0) {
      repeatUntilOptions.add(
          getString(
              R.string.repeat_until,
              DateUtilities.getRelativeDateTime(
                  context, repeatUntil, locale, FormatStyle.MEDIUM, true)));
      repeatTimes.setVisibility(View.GONE);
      repeatTimesText.setVisibility(View.GONE);
    } else if (count > 0) {
      repeatUntilOptions.add(getString(R.string.repeat_occurs));
      repeatTimes.setText(LocaleKt.formatNumber(locale, count));
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
      if (resultCode == RESULT_OK) {
        rrule.setUntil(
            new DateTime(data.getLongExtra(MyDatePickerDialog.EXTRA_TIMESTAMP, 0L)).toDate());
      }
      updateRepeatUntilOptions();
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
