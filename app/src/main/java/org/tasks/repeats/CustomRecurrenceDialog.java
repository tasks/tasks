package org.tasks.repeats;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;
import com.google.common.base.Strings;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.DatePickerActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;
import org.tasks.preferences.ResourceResolver;
import org.tasks.time.DateTime;
import timber.log.Timber;

public class CustomRecurrenceDialog extends InjectingDialogFragment {

  private static final List<Frequency> FREQUENCIES =
      asList(MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY);
  private static final String EXTRA_RRULE = "extra_rrule";
  private static final String EXTRA_DATE = "extra_date";
  private static final int REQUEST_PICK_DATE = 505;
  private final List<String> repeatUntilOptions = new ArrayList<>();
  @Inject @ForActivity Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject Locale locale;
  @Inject Tracker tracker;

  @BindView(R.id.weekGroup)
  LinearLayout weekGroup1;

  @BindView(R.id.weekGroup2)
  @Nullable
  LinearLayout weekGroup2;

  @BindView(R.id.week_day_1)
  ToggleButton day1;

  @BindView(R.id.week_day_2)
  ToggleButton day2;

  @BindView(R.id.week_day_3)
  ToggleButton day3;

  @BindView(R.id.week_day_4)
  ToggleButton day4;

  @BindView(R.id.week_day_5)
  ToggleButton day5;

  @BindView(R.id.week_day_6)
  ToggleButton day6;

  @BindView(R.id.week_day_7)
  ToggleButton day7;

  @BindView(R.id.month_group)
  RadioGroup monthGroup;

  @BindView(R.id.repeat_monthly_same_day)
  RadioButton repeatMonthlySameDay;

  @BindView(R.id.repeat_monthly_day_of_nth_week)
  RadioButton repeatMonthlyDayOfNthWeek;

  @BindView(R.id.repeat_monthly_day_of_last_week)
  RadioButton repeatMonthlyDayOfLastWeek;

  @BindView(R.id.repeat_until)
  Spinner repeatUntilSpinner;

  @BindView(R.id.frequency)
  Spinner frequencySpinner;

  @BindView(R.id.intervalValue)
  EditText intervalEditText;

  @BindView(R.id.intervalText)
  TextView intervalTextView;

  @BindView(R.id.repeatTimesValue)
  EditText repeatTimes;

  @BindView(R.id.repeatTimesText)
  TextView repeatTimesText;

  private ArrayAdapter<String> repeatUntilAdapter;
  private ToggleButton[] weekButtons;
  private RRule rrule;

  public static CustomRecurrenceDialog newCustomRecurrenceDialog(
      RepeatControlSet target, RRule rrule, long dueDate) {
    CustomRecurrenceDialog dialog = new CustomRecurrenceDialog();
    dialog.setTargetFragment(target, 0);
    Bundle arguments = new Bundle();
    if (rrule != null) {
      arguments.putString(EXTRA_RRULE, rrule.toIcal());
    }
    arguments.putLong(EXTRA_DATE, dueDate);
    dialog.setArguments(arguments);
    return dialog;
  }

  private static String getDisplayString(Context context, long repeatUntilValue) {
    StringBuilder displayString = new StringBuilder();
    DateTime d = newDateTime(repeatUntilValue);
    if (d.getMillis() > 0) {
      displayString.append(DateUtilities.getDateString(d));
      if (Task.hasDueTime(repeatUntilValue)) {
        displayString.append(", "); // $NON-NLS-1$ //$NON-NLS-2$
        displayString.append(DateUtilities.getTimeString(context, repeatUntilValue));
      }
    }
    return displayString.toString();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View dialogView = inflater.inflate(R.layout.control_set_repeat, null);

    Bundle arguments = getArguments();
    long dueDate = arguments.getLong(EXTRA_DATE, currentTimeMillis());
    String rule =
        savedInstanceState == null
            ? arguments.getString(EXTRA_RRULE)
            : savedInstanceState.getString(EXTRA_RRULE);
    try {
      if (!Strings.isNullOrEmpty(rule)) {
        rrule = new RRule(rule);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    if (rrule == null) {
      rrule = new RRule();
      rrule.setInterval(1);
      rrule.setFreq(WEEKLY);
    }

    DateFormatSymbols dfs = new DateFormatSymbols(locale.getLocale());
    String[] shortWeekdays = dfs.getShortWeekdays();

    ButterKnife.bind(this, dialogView);

    Calendar dayOfMonthCalendar = Calendar.getInstance(locale.getLocale());
    dayOfMonthCalendar.setTimeInMillis(dueDate);
    int dayOfWeekInMonth = dayOfMonthCalendar.get(Calendar.DAY_OF_WEEK_IN_MONTH);
    int maxDayOfWeekInMonth = dayOfMonthCalendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

    int dueDayOfWeek = dayOfMonthCalendar.get(Calendar.DAY_OF_WEEK);
    String today = dfs.getWeekdays()[dueDayOfWeek];
    if (dayOfWeekInMonth == maxDayOfWeekInMonth) {
      repeatMonthlyDayOfLastWeek.setVisibility(View.VISIBLE);
      String last = getString(R.string.repeat_monthly_last_week);
      String text = getString(R.string.repeat_monthly_on_every_day_of_nth_week, last, today);
      repeatMonthlyDayOfLastWeek.setTag(new WeekdayNum(-1, calendarDayToWeekday(dueDayOfWeek)));
      repeatMonthlyDayOfLastWeek.setText(text);
    } else {
      repeatMonthlyDayOfLastWeek.setVisibility(View.GONE);
    }

    if (dayOfWeekInMonth < 5) {
      int[] resources =
          new int[] {
            R.string.repeat_monthly_first_week,
            R.string.repeat_monthly_second_week,
            R.string.repeat_monthly_third_week,
            R.string.repeat_monthly_fourth_week
          };
      repeatMonthlyDayOfNthWeek.setVisibility(View.VISIBLE);
      String nth = getString(resources[dayOfWeekInMonth - 1]);
      String text = getString(R.string.repeat_monthly_on_every_day_of_nth_week, nth, today);
      repeatMonthlyDayOfNthWeek.setTag(
          new WeekdayNum(dayOfWeekInMonth, calendarDayToWeekday(dueDayOfWeek)));
      repeatMonthlyDayOfNthWeek.setText(text);
    } else {
      repeatMonthlyDayOfNthWeek.setVisibility(View.GONE);
    }

    if (rrule.getFreq() == MONTHLY) {
      if (rrule.getByDay().size() == 1) {
        WeekdayNum weekdayNum = rrule.getByDay().get(0);
        if (weekdayNum.num == -1) {
          repeatMonthlyDayOfLastWeek.setChecked(true);
        } else if (weekdayNum.num == dayOfWeekInMonth) {
          repeatMonthlyDayOfNthWeek.setChecked(true);
        }
      }
    }
    if (monthGroup.getCheckedRadioButtonId() != R.id.repeat_monthly_day_of_last_week
        && monthGroup.getCheckedRadioButtonId() != R.id.repeat_monthly_day_of_nth_week) {
      repeatMonthlySameDay.setChecked(true);
    }

    ArrayAdapter<CharSequence> frequencyAdapter =
        ArrayAdapter.createFromResource(context, R.array.repeat_frequency, R.layout.frequency_item);
    frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    frequencySpinner.setAdapter(frequencyAdapter);
    frequencySpinner.setSelection(FREQUENCIES.indexOf(rrule.getFreq()));

    intervalEditText.setText(locale.formatNumber(rrule.getInterval()));

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

    weekButtons = new ToggleButton[] {day1, day2, day3, day4, day5, day6, day7};

    // set up days of week
    Calendar dayOfWeekCalendar = Calendar.getInstance(locale.getLocale());
    dayOfWeekCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeekCalendar.getFirstDayOfWeek());

    WeekdayNum todayWeekday = new WeekdayNum(0, new DateTime(dueDate).getWeekday());

    ColorStateList colorStateList =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked}, new int[] {-android.R.attr.state_checked}
            },
            new int[] {
              ResourceResolver.getData(context, R.attr.fab_text),
              getColor(context, R.color.text_primary)
            });
    int inset = (int) context.getResources().getDimension(R.dimen.week_button_inset);
    int accentColor = ResourceResolver.getData(context, R.attr.colorAccent);
    int animationDuration =
        context.getResources().getInteger(android.R.integer.config_shortAnimTime);

    for (int i = 0; i < 7; i++) {
      ToggleButton weekButton = weekButtons[i];

      GradientDrawable ovalDrawable =
          (GradientDrawable)
              context.getResources().getDrawable(R.drawable.week_day_button_oval).mutate();
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
      ViewCompat.setBackground(weekButton, stateListDrawable);
      weekButton.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

      int dayOfWeek = dayOfWeekCalendar.get(Calendar.DAY_OF_WEEK);
      String text = shortWeekdays[dayOfWeek];
      weekButton.setTextColor(colorStateList);
      weekButton.setTextOn(text);
      weekButton.setTextOff(text);
      weekButton.setTag(new WeekdayNum(0, calendarDayToWeekday(dayOfWeek)));
      if (savedInstanceState == null) {
        weekButton.setChecked(
            rrule.getFreq() != WEEKLY || rrule.getByDay().isEmpty()
                ? todayWeekday.equals(weekButton.getTag())
                : rrule.getByDay().contains(weekButton.getTag()));
      }
      dayOfWeekCalendar.add(Calendar.DATE, 1);
    }

    setCancelable(false);

    return dialogBuilder
        .newDialog()
        .setView(dialogView)
        .setPositiveButton(android.R.string.ok, this::onRuleSelected)
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void onRuleSelected(DialogInterface dialogInterface, int which) {
    if (rrule.getFreq() == WEEKLY) {
      List<WeekdayNum> checked = new ArrayList<>();
      for (ToggleButton weekButton : weekButtons) {
        if (weekButton.isChecked()) {
          checked.add((WeekdayNum) weekButton.getTag());
        }
      }
      rrule.setByDay(checked);
    } else if (rrule.getFreq() == MONTHLY) {
      switch (monthGroup.getCheckedRadioButtonId()) {
        case R.id.repeat_monthly_same_day:
          rrule.setByDay(Collections.emptyList());
          break;
        case R.id.repeat_monthly_day_of_nth_week:
          rrule.setByDay(newArrayList((WeekdayNum) repeatMonthlyDayOfNthWeek.getTag()));
          break;
        case R.id.repeat_monthly_day_of_last_week:
          rrule.setByDay(newArrayList((WeekdayNum) repeatMonthlyDayOfLastWeek.getTag()));
          break;
      }
    } else {
      rrule.setByDay(Collections.emptyList());
    }
    tracker.reportEvent(Tracking.Events.RECURRENCE_CUSTOM, rrule.toIcal());
    RepeatControlSet target = (RepeatControlSet) getTargetFragment();
    if (target != null) {
      target.onSelected(rrule);
    }
    dismiss();
  }

  private Weekday calendarDayToWeekday(int calendarDay) {
    switch (calendarDay) {
      case Calendar.SUNDAY:
        return Weekday.SU;
      case Calendar.MONDAY:
        return Weekday.MO;
      case Calendar.TUESDAY:
        return Weekday.TU;
      case Calendar.WEDNESDAY:
        return Weekday.WE;
      case Calendar.THURSDAY:
        return Weekday.TH;
      case Calendar.FRIDAY:
        return Weekday.FR;
      case Calendar.SATURDAY:
        return Weekday.SA;
    }
    throw new RuntimeException("Invalid calendar day: " + calendarDay);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_RRULE, rrule.toIcal());
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
    repeatTimesText.setText(
        getResources().getQuantityString(R.plurals.repeat_times, rrule.getCount()));
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
    Frequency frequency = FREQUENCIES.get(position);
    rrule.setFreq(frequency);
    int weekVisibility = frequency == WEEKLY ? View.VISIBLE : View.GONE;
    weekGroup1.setVisibility(weekVisibility);
    if (weekGroup2 != null) {
      weekGroup2.setVisibility(weekVisibility);
    }
    monthGroup.setVisibility(frequency == MONTHLY ? View.VISIBLE : View.GONE);
    updateIntervalTextView();
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
      repeatUntilOptions.add(
          getString(R.string.repeat_until, getDisplayString(context, repeatUntil)));
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
        rrule.setUntil(
            new DateTime(data.getLongExtra(DatePickerActivity.EXTRA_TIMESTAMP, 0L)).toDateValue());
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
}
