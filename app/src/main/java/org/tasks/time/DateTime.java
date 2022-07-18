package org.tasks.time;

import static com.todoroo.astrid.core.SortHelper.APPLE_EPOCH;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

import net.fortuna.ical4j.model.WeekDay;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTime {

  public static final int MAX_MILLIS_PER_DAY = (int) TimeUnit.DAYS.toMillis(1) - 1;
  public static final TimeZone UTC = TimeZone.getTimeZone("GMT");
  private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);
  private static final int MILLIS_PER_MINUTE = (int) TimeUnit.MINUTES.toMillis(1);
  private static final int MILLIS_PER_SECOND = (int) TimeUnit.SECONDS.toMillis(1);

  private final TimeZone timeZone;
  private final long timestamp;

  public DateTime(int year, int month, int day) {
    this(year, month, day, 0, 0, 0, 0);
  }

  public DateTime(int year, int month, int day, int hour, int minute) {
    this(year, month, day, hour, minute, 0, 0);
  }

  public DateTime(int year, int month, int day, int hour, int minute, int second) {
    this(year, month, day, hour, minute, second, 0);
  }

  public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond) {
    this(year, month, day, hour, minute, second, millisecond, TimeZone.getDefault());
  }

  public DateTime(
      int year,
      int month,
      int day,
      int hour,
      int minute,
      int second,
      int millisecond,
      TimeZone timeZone) {
    GregorianCalendar gregorianCalendar = new GregorianCalendar(timeZone);
    gregorianCalendar.set(year, month - 1, day, hour, minute, second);
    gregorianCalendar.set(Calendar.MILLISECOND, millisecond);
    timestamp = gregorianCalendar.getTimeInMillis();
    this.timeZone = timeZone;
  }

  public DateTime() {
    this(currentTimeMillis());
  }

  public DateTime(long timestamp) {
    this(timestamp, TimeZone.getDefault());
  }

  public DateTime(long timestamp, TimeZone timeZone) {
    this.timestamp = timestamp;
    this.timeZone = timeZone;
  }

  private DateTime(Calendar calendar) {
    this(calendar.getTimeInMillis(), calendar.getTimeZone());
  }

  public static DateTime from(Date date) {
    if (date == null) {
      return new DateTime(0);
    }
    DateTime dateTime = new DateTime(date.getTime());
    return dateTime.minusMillis(dateTime.getOffset());
  }

  public static DateTime from(net.fortuna.ical4j.model.Date date) {
    if (date instanceof net.fortuna.ical4j.model.DateTime) {
      net.fortuna.ical4j.model.DateTime dt = (net.fortuna.ical4j.model.DateTime) date;
      TimeZone tz = dt.getTimeZone();
      return new DateTime(
          dt.getTime(),
          tz != null ? tz : dt.isUtc() ? UTC : TimeZone.getDefault()
      );
    } else {
      return from((java.util.Date) date);
    }
  }

  private DateTime setTime(int hours, int minutes, int seconds, int milliseconds) {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, hours);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, seconds);
    calendar.set(Calendar.MILLISECOND, milliseconds);
    return new DateTime(calendar);
  }

  public DateTime startOfDay() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return new DateTime(calendar);
  }

  public DateTime startOfMinute() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return new DateTime(calendar);
  }

  public DateTime startOfSecond() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.MILLISECOND, 0);
    return new DateTime(calendar);
  }

  public DateTime endOfMinute() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return new DateTime(calendar);
  }

  public DateTime noon() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return new DateTime(calendar);
  }

  public DateTime endOfDay() {
    Calendar calendar = getCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    return new DateTime(calendar);
  }

  public DateTime withMillisOfDay(int millisOfDay) {
    if (millisOfDay > MAX_MILLIS_PER_DAY || millisOfDay < 0) {
      throw new RuntimeException("Illegal millis of day: " + millisOfDay);
    }
    int hours = millisOfDay / MILLIS_PER_HOUR;
    millisOfDay %= MILLIS_PER_HOUR;
    int minutes = millisOfDay / MILLIS_PER_MINUTE;
    millisOfDay %= MILLIS_PER_MINUTE;
    int seconds = millisOfDay / MILLIS_PER_SECOND;
    millisOfDay %= MILLIS_PER_SECOND;
    return startOfDay().setTime(hours, minutes, seconds, millisOfDay);
  }

  public long getOffset() {
    return timeZone.getOffset(timestamp);
  }

  public long getMillis() {
    return timestamp;
  }

  public int getMillisOfDay() {
    Calendar calendar = getCalendar();
    long millisOfDay =
        calendar.get(Calendar.MILLISECOND)
            + TimeUnit.SECONDS.toMillis(calendar.get(Calendar.SECOND))
            + TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE))
            + TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY));
    return (int) millisOfDay;
  }

  public int getYear() {
    return getCalendar().get(Calendar.YEAR);
  }

  public int getMonthOfYear() {
    return getCalendar().get(Calendar.MONTH) + 1;
  }

  public int getDayOfMonth() {
    return getCalendar().get(Calendar.DATE);
  }

  public int getDayOfWeek() {
    return getCalendar().get(Calendar.DAY_OF_WEEK);
  }

  public int getHourOfDay() {
    return getCalendar().get(Calendar.HOUR_OF_DAY);
  }

  public int getMinuteOfHour() {
    return getCalendar().get(Calendar.MINUTE);
  }

  public int getSecondOfMinute() {
    return getCalendar().get(Calendar.SECOND);
  }

  public DateTime withYear(int year) {
    return with(Calendar.YEAR, year);
  }

  public DateTime withMonthOfYear(int monthOfYear) {
    return with(Calendar.MONTH, monthOfYear - 1);
  }

  public DateTime withDayOfMonth(int dayOfMonth) {
    return with(Calendar.DAY_OF_MONTH, dayOfMonth);
  }

  public DateTime withHourOfDay(int hourOfDay) {
    return with(Calendar.HOUR_OF_DAY, hourOfDay);
  }

  public DateTime withMinuteOfHour(int minuteOfHour) {
    return with(Calendar.MINUTE, minuteOfHour);
  }

  public DateTime withSecondOfMinute(int secondOfMinute) {
    return with(Calendar.SECOND, secondOfMinute);
  }

  public DateTime withMillisOfSecond(int millisOfSecond) {
    return with(Calendar.MILLISECOND, millisOfSecond);
  }

  public DateTime plusMonths(int interval) {
    return add(Calendar.MONTH, interval);
  }

  public DateTime plusWeeks(int weeks) {
    return add(Calendar.WEEK_OF_MONTH, weeks);
  }

  public DateTime plusDays(int interval) {
    return add(Calendar.DATE, interval);
  }

  public DateTime plusHours(int hours) {
    return add(Calendar.HOUR_OF_DAY, hours);
  }

  public DateTime plusMinutes(int minutes) {
    return add(Calendar.MINUTE, minutes);
  }

  public DateTime plusSeconds(int seconds) {
    return add(Calendar.SECOND, seconds);
  }

  public DateTime plusMillis(int millis) {
    return add(Calendar.MILLISECOND, millis);
  }

  public DateTime minusSeconds(int seconds) {
    return subtract(Calendar.SECOND, seconds);
  }

  public DateTime minusDays(int days) {
    return subtract(Calendar.DATE, days);
  }

  public DateTime minusMinutes(int minutes) {
    return subtract(Calendar.MINUTE, minutes);
  }

  public DateTime minusMillis(long millis) {
    return new DateTime(timestamp - millis, timeZone);
  }

  public boolean isAfterNow() {
    return isAfter(currentTimeMillis());
  }

  public boolean isAfter(DateTime dateTime) {
    return isAfter(dateTime.getMillis());
  }

  private boolean isAfter(long timestamp) {
    return this.timestamp > timestamp;
  }

  public boolean isBeforeNow() {
    return timestamp < DateTimeUtils.currentTimeMillis();
  }

  public boolean isBefore(DateTime dateTime) {
    return timestamp < dateTime.getMillis();
  }

  public DateTime toUTC() {
    return toTimeZone(UTC);
  }

  public DateTime toLocal() {
    return toTimeZone(TimeZone.getDefault());
  }

  public boolean isLastDayOfMonth() {
    return getDayOfMonth() == getNumberOfDaysInMonth();
  }

  public int getNumberOfDaysInMonth() {
    return getCalendar().getActualMaximum(Calendar.DAY_OF_MONTH);
  }

  private DateTime toTimeZone(TimeZone timeZone) {
    if (timeZone == this.timeZone) {
      return this;
    }
    Calendar current = getCalendar();
    Calendar target = new GregorianCalendar(timeZone);
    target.setTimeInMillis(current.getTimeInMillis());
    return new DateTime(target);
  }

  private DateTime with(int field, int value) {
    Calendar calendar = getCalendar();
    calendar.set(field, value);
    return new DateTime(calendar);
  }

  private DateTime subtract(int field, int value) {
    return add(field, -value);
  }

  private DateTime add(int field, int value) {
    Calendar calendar = getCalendar();
    calendar.add(field, value);
    return new DateTime(calendar);
  }

  private Calendar getCalendar() {
    Calendar calendar = new GregorianCalendar(timeZone);
    calendar.setTimeInMillis(timestamp);
    return calendar;
  }

  public net.fortuna.ical4j.model.DateTime toDateTime() {
    return timestamp == 0 ? null : new net.fortuna.ical4j.model.DateTime(timestamp);
  }

  public net.fortuna.ical4j.model.Date toDate() {
    return timestamp == 0 ? null : new net.fortuna.ical4j.model.Date(timestamp + getOffset());
  }

  public LocalDate toLocalDate() {
    return timestamp == 0 ? null : LocalDate.of(getYear(), getMonthOfYear(), getDayOfMonth());
  }

  public LocalDateTime toLocalDateTime() {
    return timestamp == 0 ? null : LocalDateTime.of(getYear(), getMonthOfYear(), getDayOfMonth(), getHourOfDay(), getMinuteOfHour());
  }

  public long toAppleEpoch() {
    return (timestamp - APPLE_EPOCH) / 1000;
  }

  public int getDayOfWeekInMonth() {
    return getCalendar().get(Calendar.DAY_OF_WEEK_IN_MONTH);
  }

  public int getMaxDayOfWeekInMonth() {
    return getCalendar().getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);
  }

  public WeekDay getWeekDay() {
    switch (getCalendar().get(Calendar.DAY_OF_WEEK)) {
      case SUNDAY:
        return WeekDay.SU;
      case MONDAY:
        return WeekDay.MO;
      case TUESDAY:
        return WeekDay.TU;
      case WEDNESDAY:
        return WeekDay.WE;
      case THURSDAY:
        return WeekDay.TH;
      case FRIDAY:
        return WeekDay.FR;
      case SATURDAY:
        return WeekDay.SA;
    }
    throw new RuntimeException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DateTime)) {
      return false;
    }
    DateTime dateTime = (DateTime) o;
    return timestamp == dateTime.timestamp && Objects.equals(timeZone, dateTime.timeZone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timeZone, timestamp);
  }

  public String toString(String format) {
    Calendar calendar = getCalendar();
    SimpleDateFormat simpleDateFormat =
        new SimpleDateFormat(format, Locale.getDefault());
    simpleDateFormat.setCalendar(calendar);
    return simpleDateFormat.format(calendar.getTime());
  }

  @Override
  public String toString() {
    return toString("yyyy-MM-dd HH:mm:ss.SSSZ");
  }
}
