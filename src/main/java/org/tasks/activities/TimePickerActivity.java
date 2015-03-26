package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.joda.time.DateTime;
import org.tasks.dialogs.DateAndTimePickerDialog;

import static org.tasks.date.DateTimeUtils.newDateTime;

public class TimePickerActivity extends FragmentActivity {

    public static final String EXTRA_HOURS = "extra_hours";
    public static final String EXTRA_MINUTES = "extra_minutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int hour = intent.getIntExtra(EXTRA_HOURS, 0);
        int minutes = intent.getIntExtra(EXTRA_MINUTES, 0);

        DateAndTimePickerDialog.timePickerDialog(getSupportFragmentManager(), this, newDateTime().withMillisOfDay(0).withHourOfDay(hour).withMinuteOfHour(minutes), new DateAndTimePickerDialog.OnTimePicked() {
            @Override
            public void onTimePicked(int millisOfDay) {
                final DateTime dateTime = newDateTime().withMillisOfDay(millisOfDay);
                setResult(RESULT_OK, new Intent() {{
                    putExtra(EXTRA_HOURS, dateTime.getHourOfDay());
                    putExtra(EXTRA_MINUTES, dateTime.getMinuteOfHour());
                }});
                finish();
            }
        }, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }
}
