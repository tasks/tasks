package org.tasks.repeats;

import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;

import android.content.Context;
import com.google.common.base.Joiner;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.utility.DateUtilities;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.locale.Locale;
import org.tasks.time.DateTime;

public class RepeatRuleToString {

  private final Context context;
  private final Locale locale;
  private final List<Weekday> weekdays = Arrays.asList(Weekday.values());

  @Inject
  public RepeatRuleToString(@ForApplication Context context, Locale locale) {
    this.context = context;
    this.locale = locale;
  }

  public String toString(RRule rrule) {
    int interval = rrule.getInterval();
    Frequency frequency = rrule.getFreq();
    DateTime repeatUntil = rrule.getUntil() == null ? null : DateTime.from(rrule.getUntil());
    int count = rrule.getCount();
    String countString =
        count > 0 ? context.getResources().getQuantityString(R.plurals.repeat_times, count) : "";
    if (interval == 1) {
      String frequencyString = context.getString(getSingleFrequencyResource(frequency));
      if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.getByDay().isEmpty()) {
        String dayString = getDayString(rrule);
        if (count > 0) {
          return context.getString(
              R.string.repeats_single_on_number_of_times,
              frequencyString,
              dayString,
              count,
              countString);
        } else if (repeatUntil == null) {
          return context.getString(R.string.repeats_single_on, frequencyString, dayString);
        } else {
          return context.getString(
              R.string.repeats_single_on_until,
              frequencyString,
              dayString,
              DateUtilities.getLongDateString(repeatUntil));
        }
      } else if (count > 0) {
        return context.getString(
            R.string.repeats_single_number_of_times, frequencyString, count, countString);
      } else if (repeatUntil == null) {
        return context.getString(R.string.repeats_single, frequencyString);
      } else {
        return context.getString(
            R.string.repeats_single_until,
            frequencyString,
            DateUtilities.getLongDateString(repeatUntil));
      }
    } else {
      int plural = getFrequencyPlural(frequency);
      String frequencyPlural = context.getResources().getQuantityString(plural, interval, interval);
      if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.getByDay().isEmpty()) {
        String dayString = getDayString(rrule);
        if (count > 0) {
          return context.getString(
              R.string.repeats_plural_on_number_of_times,
              frequencyPlural,
              dayString,
              count,
              countString);
        } else if (repeatUntil == null) {
          return context.getString(R.string.repeats_plural_on, frequencyPlural, dayString);
        } else {
          return context.getString(
              R.string.repeats_plural_on_until,
              frequencyPlural,
              dayString,
              DateUtilities.getLongDateString(repeatUntil));
        }
      } else if (count > 0) {
        return context.getString(
            R.string.repeats_plural_number_of_times, frequencyPlural, count, countString);
      } else if (repeatUntil == null) {
        return context.getString(R.string.repeats_plural, frequencyPlural);
      } else {
        return context.getString(
            R.string.repeats_plural_until,
            frequencyPlural,
            DateUtilities.getLongDateString(repeatUntil));
      }
    }
  }

  private String getDayString(RRule rrule) {
    DateFormatSymbols dfs = new DateFormatSymbols(locale.getLocale());
    if (rrule.getFreq() == WEEKLY) {
      String[] shortWeekdays = dfs.getShortWeekdays();
      List<String> days = new ArrayList<>();
      for (WeekdayNum weekday : rrule.getByDay()) {
        days.add(shortWeekdays[weekdays.indexOf(weekday.wday) + 1]);
      }
      return Joiner.on(context.getString(R.string.list_separator_with_space)).join(days);
    } else if (rrule.getFreq() == MONTHLY) {
      String[] longWeekdays = dfs.getWeekdays();
      WeekdayNum weekdayNum = rrule.getByDay().get(0);
      String weekday;
      Calendar dayOfWeekCalendar = Calendar.getInstance(locale.getLocale());
      dayOfWeekCalendar.set(Calendar.DAY_OF_WEEK, weekdayToCalendarDay(weekdayNum.wday));
      weekday = longWeekdays[dayOfWeekCalendar.get(Calendar.DAY_OF_WEEK)];
      if (weekdayNum.num == -1) {
        return context.getString(
            R.string.repeat_monthly_every_day_of_nth_week,
            context.getString(R.string.repeat_monthly_last_week),
            weekday);
      } else {
        int[] resources =
            new int[] {
              R.string.repeat_monthly_first_week,
              R.string.repeat_monthly_second_week,
              R.string.repeat_monthly_third_week,
              R.string.repeat_monthly_fourth_week
            };
        return context.getString(
            R.string.repeat_monthly_every_day_of_nth_week,
            context.getString(resources[weekdayNum.num - 1]),
            weekday);
      }
    } else {
      throw new RuntimeException();
    }
  }

  private int weekdayToCalendarDay(Weekday weekday) {
    switch (weekday) {
      case SU:
        return Calendar.SUNDAY;
      case MO:
        return Calendar.MONDAY;
      case TU:
        return Calendar.TUESDAY;
      case WE:
        return Calendar.WEDNESDAY;
      case TH:
        return Calendar.THURSDAY;
      case FR:
        return Calendar.FRIDAY;
      case SA:
        return Calendar.SATURDAY;
      default:
        throw new RuntimeException("Invalid weekday: " + weekday);
    }
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
