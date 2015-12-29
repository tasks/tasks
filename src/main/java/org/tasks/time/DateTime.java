package org.tasks.time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTime {

    public static final int MAX_MILLIS_PER_DAY = (int) TimeUnit.DAYS.toMillis(1) - 1;
    private static final TimeZone UTC = TimeZone.getTimeZone("GMT");
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);
    private static final int MILLIS_PER_MINUTE = (int) TimeUnit.MINUTES.toMillis(1);
    private static final int MILLIS_PER_SECOND = (int) TimeUnit.SECONDS.toMillis(1);

    private final TimeZone timeZone;
    private final long timestamp;

    public DateTime(int year, int month, int day, int hour, int minute) {
        this(year, month, day, hour, minute, 0, 0);
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second) {
        this(year, month, day, hour, minute, second, 0);
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        this(year, month, day, hour, minute, second, millisecond, TimeZone.getDefault());
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, TimeZone timeZone) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(timeZone);
        gregorianCalendar.set(year, month - 1, day, hour, minute, second);
        gregorianCalendar.set(Calendar.MILLISECOND, millisecond);
        timestamp = gregorianCalendar.getTimeInMillis();
        this.timeZone = timeZone;
    }

    public DateTime() {
        this(DateTimeUtils.currentTimeMillis());
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

    public long getMillis() {
        return timestamp;
    }

    public int getMillisOfDay() {
        Calendar calendar = getCalendar();
        long millisOfDay =
                calendar.get(Calendar.MILLISECOND) +
                        TimeUnit.SECONDS.toMillis(calendar.get(Calendar.SECOND)) +
                        TimeUnit.MINUTES.toMillis(calendar.get(Calendar.MINUTE)) +
                        TimeUnit.HOURS.toMillis(calendar.get(Calendar.HOUR_OF_DAY));
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

    public DateTime minusDays(int days) {
        return subtract(Calendar.DATE, days);
    }

    public DateTime minusMinutes(int minutes) {
        return subtract(Calendar.MINUTE, minutes);
    }

    public DateTime minusMillis(int millis) {
        return new DateTime(timestamp - millis, timeZone);
    }

    public boolean isAfter(DateTime dateTime) {
        return timestamp > dateTime.getMillis();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateTime dateTime = (DateTime) o;

        if (timestamp != dateTime.timestamp) return false;
        return !(timeZone != null ? !timeZone.equals(dateTime.timeZone) : dateTime.timeZone != null);

    }

    @Override
    public int hashCode() {
        int result = timeZone != null ? timeZone.hashCode() : 0;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    public String toString(String format) {
        Calendar calendar = getCalendar();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setCalendar(calendar);
        return simpleDateFormat.format(calendar.getTime());
    }

    @Override
    public String toString() {
        return toString("yyyy-MM-dd HH:mm:ss.SSSZ");
    }
}
