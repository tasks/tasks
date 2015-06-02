package org.tasks.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;

import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;


public class TimePickerActivity extends FragmentActivity implements TimePickerDialog.OnTimeSetListener {

    public static final String EXTRA_HOURS = "extra_hours";
    public static final String EXTRA_MINUTES = "extra_minutes";
    private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        TimePickerDialog dialog = (TimePickerDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (dialog == null) {
            Intent intent = getIntent();
            int hours = intent.getIntExtra(EXTRA_HOURS, 0);
            int minutes = intent.getIntExtra(EXTRA_MINUTES, 0);
            dialog = new TimePickerDialog();
            dialog.initialize(null, hours, minutes, DateFormat.is24HourFormat(this), false);
            dialog.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
        }
        dialog.setOnTimeSetListener(this);
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, final int hours, final int minutes) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_HOURS, hours);
            putExtra(EXTRA_MINUTES, minutes);
        }});
        finish();
    }
}
