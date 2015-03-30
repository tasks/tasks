package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.joda.time.DateTime;

import static org.tasks.date.DateTimeUtils.newDateTime;

public class DateAndTimePickerDialog {

    public interface OnDatePicked {
        void onDatePicked(DateTime date);
    }

    public interface OnTimePicked {
        void onTimePicked(int millisOfDay);
    }

    public interface OnDateTimePicked {
        void onDateTimePicked(DateTime dateTime);
    }

    private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
    private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";

    public static void datePickerDialog(FragmentManager fragmentManager, DateTime initial, final OnDatePicked onDatePicked, DialogInterface.OnDismissListener dismissed) {
        MyDatePickerDialog dialog = new MyDatePickerDialog();
        dialog.initialize(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                onDatePicked.onDatePicked(newDateTime().withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day).withMillisOfDay(0));
            }
        }, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth(), true);
        dialog.setOnDismissListener(dismissed);
        dialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
    }

    public static void timePickerDialog(FragmentManager fragmentManager, Context context, DateTime initial, final OnTimePicked onTimePicked, DialogInterface.OnDismissListener dismissed) {
        MyTimePickerDialog dialog = new MyTimePickerDialog();
        dialog.initialize(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout radialPickerLayout, int hours, int minutes) {
                onTimePicked.onTimePicked(newDateTime().withMillisOfDay(0).withHourOfDay(hours).withMinuteOfHour(minutes).getMillisOfDay());
            }
        }, initial.getHourOfDay(), initial.getMinuteOfHour(), DateFormat.is24HourFormat(context), true);
        dialog.setOnDismissListener(dismissed);
        dialog.show(fragmentManager, FRAG_TAG_TIME_PICKER);
    }

    public static void dateAndTimePickerDialog(final FragmentManager fragmentManager, final Context context, final DateTime dateTime, final OnDateTimePicked dateTimePicked, final DialogInterface.OnDismissListener dismissed) {
        datePickerDialog(fragmentManager, dateTime, new OnDatePicked() {
            @Override
            public void onDatePicked(final DateTime date) {
                timePickerDialog(fragmentManager, context, dateTime, new OnTimePicked() {
                    @Override
                    public void onTimePicked(int millisOfDay) {
                        dateTimePicked.onDateTimePicked(date.withMillisOfDay(millisOfDay));
                    }
                }, dismissed);
            }
        }, dismissed);
    }
}
