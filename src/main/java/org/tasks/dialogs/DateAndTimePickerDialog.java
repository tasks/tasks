package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;

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

    public static void datePickerDialog(FragmentManager fragmentManager, DateTime initial, final OnDatePicked onDatePicked, final DialogInterface.OnDismissListener onDismissListener) {
        MyCalendarDatePickerDialog dialog = new MyCalendarDatePickerDialog();
        dialog.setOnDismissListener(onDismissListener);
        dialog.initialize(new CalendarDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int year, int month, int day) {
                onDatePicked.onDatePicked(newDateTime().withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day).withMillisOfDay(0));
            }
        }, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
        dialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
    }

    public static void timePickerDialog(FragmentManager fragmentManager, Context context, DateTime initial, final OnTimePicked onTimePicked, final DialogInterface.OnDismissListener onDismissListener) {
        final boolean[] success = new boolean[1];
        RadialTimePickerDialog dialog = new RadialTimePickerDialog();
        dialog.initialize(new RadialTimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialog radialTimePickerDialog, int hours, int minutes) {
                success[0] = true;
                onTimePicked.onTimePicked(newDateTime().withMillisOfDay(0).withHourOfDay(hours).withMinuteOfHour(minutes).getMillisOfDay());
            }
        }, initial.getHourOfDay(), initial.getMinuteOfHour(), DateFormat.is24HourFormat(context));
        dialog.setOnDismissListener(new RadialTimePickerDialog.OnDialogDismissListener() {
            @Override
            public void onDialogDismiss(DialogInterface dialogInterface) {
                if (!success[0]) {
                    onDismissListener.onDismiss(dialogInterface);
                }
            }
        });
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
