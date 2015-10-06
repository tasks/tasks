package org.tasks.time;

import org.joda.time.DateTimeZone;

import java.util.Date;
import java.util.TimeZone;

public class DateTime {

    private final org.joda.time.DateTime dateTime;

    public DateTime(int year, int month, int day, int hour, int minute, int second) {
        this(year, month, day, hour, minute, second, 0);
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        this(year, month, day, hour, minute, second, millisecond, TimeZone.getDefault());
    }

    public DateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, TimeZone timeZone) {
        this(new org.joda.time.DateTime(year, month, day, hour, minute, second, millisecond, DateTimeZone.forTimeZone(timeZone)));
    }

    public static DateTime now() {
        return new DateTime();
    }

    public DateTime() {
        this(DateTimeUtils.currentTimeMillis());
    }

    public DateTime(org.joda.time.DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public DateTime(long timestamp) {
        this(timestamp, TimeZone.getDefault());
    }

    public DateTime(long timestamp, TimeZone timeZone) {
        dateTime = new org.joda.time.DateTime(timestamp, DateTimeZone.forTimeZone(timeZone));
    }

    public DateTime(Date d) {
        dateTime = new org.joda.time.DateTime(d, DateTimeZone.forTimeZone(TimeZone.getDefault()));
    }

    public DateTime withMillisOfDay(int millisOfDay) {
        return new DateTime(dateTime.withMillisOfDay(millisOfDay));
    }

    public long getMillis() {
        return dateTime.getMillis();
    }

    public DateTime plusMonths(int interval) {
        return new DateTime(dateTime.plusMonths(interval));
    }

    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }

    public DateTime plusDays(int interval) {
        return new DateTime(dateTime.plusDays(interval));
    }

    public int getMinuteOfHour() {
        return dateTime.getMinuteOfHour();
    }

    public String toString(String format) {
        return dateTime.toString(format);
    }

    public Date toDate() {
        return dateTime.toDate();
    }

    public int getNumberOfDaysInMonth() {
        return dateTime.dayOfMonth().getMaximumValue();
    }

    public DateTime withMillisOfSecond(int millisOfSecond) {
        return new DateTime(dateTime.withMillisOfSecond(millisOfSecond));
    }

    public DateTime withHourOfDay(int hourOfDay) {
        return new DateTime(dateTime.withHourOfDay(hourOfDay));
    }

    public DateTime withMinuteOfHour(int minuteOfHour) {
        return new DateTime(dateTime.withMinuteOfHour(minuteOfHour));
    }

    public DateTime withSecondOfMinute(int secondOfMinute) {
        return new DateTime(dateTime.withSecondOfMinute(secondOfMinute));
    }

    public int getYear() {
        return dateTime.getYear();
    }

    public DateTime minusMinutes(int minutes) {
        return new DateTime(dateTime.minusMinutes(minutes));
    }

    public boolean isBefore(DateTime dateTime) {
        return this.dateTime.isBefore(dateTime.dateTime);
    }

    public int getMillisOfDay() {
        return dateTime.getMillisOfDay();
    }

    public int getMonthOfYear() {
        return dateTime.getMonthOfYear();
    }

    public boolean isAfter(DateTime dateTime) {
        return this.dateTime.isAfter(dateTime.dateTime);
    }

    public DateTime withYear(int year) {
        return new DateTime(dateTime.withYear(year));
    }

    public DateTime withMonthOfYear(int monthOfYear) {
        return new DateTime(dateTime.withMonthOfYear(monthOfYear));
    }

    public int getHourOfDay() {
        return dateTime.getHourOfDay();
    }

    public DateTime withDayOfMonth(int dayOfMonth) {
        return new DateTime(dateTime.withDayOfMonth(dayOfMonth));
    }

    public DateTime plusMinutes(int minutes) {
        return new DateTime(dateTime.plusMinutes(minutes));
    }

    public DateTime plusHours(int hours) {
        return new DateTime(dateTime.plusHours(hours));
    }

    public DateTime plusWeeks(int weeks) {
        return new DateTime(dateTime.plusWeeks(weeks));
    }

    public boolean isBeforeNow() {
        return dateTime.isBefore(DateTimeUtils.currentTimeMillis());
    }

    public DateTime minusMillis(int millis) {
        return new DateTime(dateTime.minusMillis(millis));
    }

    public DateTime minusDays(int days) {
        return new DateTime(dateTime.minusDays(days));
    }

    public DateTime toUTC() {
        return new DateTime(dateTime.toDateTime(DateTimeZone.UTC));
    }

    public int getSecondOfMinute() {
        return dateTime.getSecondOfMinute();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateTime dateTime1 = (DateTime) o;

        return !(dateTime != null ? !dateTime.equals(dateTime1.dateTime) : dateTime1.dateTime != null);

    }

    @Override
    public int hashCode() {
        return dateTime != null ? dateTime.hashCode() : 0;
    }

    @Override
    public String toString() {
        return dateTime.toString();
    }
}
