package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;

import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tasks.time.DateTime;
import org.tasks.dialogs.MyTimePickerDialog;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;


public class TimePickerActivity extends FragmentActivity implements TimePickerDialog.OnTimeSetListener, DialogInterface.OnDismissListener {

    private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";

    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    private DateTime initial;
    private boolean isChangingConfigurations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initial = new DateTime(getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()));

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        MyTimePickerDialog dialog = (MyTimePickerDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (dialog == null) {
            dialog = new MyTimePickerDialog();
            dialog.initialize(null, initial.getHourOfDay(), initial.getMinuteOfHour(), DateFormat.is24HourFormat(this), false);
            dialog.show(supportFragmentManager, FRAG_TAG_TIME_PICKER);
        }
        dialog.setOnDismissListener(this);
        dialog.setOnTimeSetListener(this);
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, final int hours, final int minutes) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_TIMESTAMP, initial
                    .startOfDay()
                    .withHourOfDay(hours)
                    .withMinuteOfHour(minutes)
                    .getMillis());
        }});
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        isChangingConfigurations = true;
        return super.onRetainCustomNonConfigurationInstance();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (isChangingConfigurations) {
            isChangingConfigurations = false;
            return;
        }
        finish();
    }
}
