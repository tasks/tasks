package org.tasks.activities;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;

import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ThemeManager;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;


public class TimePickerActivity extends InjectingAppCompatActivity implements TimePickerDialog.OnTimeSetListener, DialogInterface.OnDismissListener {

    private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";

    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    @Inject ThemeManager themeManager;

    private DateTime initial;
    private boolean isChangingConfigurations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initial = new DateTime(getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()));

        FragmentManager fragmentManager = getFragmentManager();
        MyTimePickerDialog dialog = (MyTimePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (dialog == null) {
            dialog = new MyTimePickerDialog();
            dialog.initialize(null, initial.getHourOfDay(), initial.getMinuteOfHour(), 0, DateFormat.is24HourFormat(this));
            dialog.setAccentColor(themeManager.getAppTheme().getDateTimePickerAccent());
            dialog.show(fragmentManager, FRAG_TAG_TIME_PICKER);
        }
        dialog.setOnDismissListener(this);
        dialog.setOnTimeSetListener(this);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, final int hours, final int minutes, int seconds) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_TIMESTAMP, initial
                    .startOfDay()
                    .withHourOfDay(hours)
                    .withMinuteOfHour(minutes)
                    .getMillis());
        }});
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isChangingConfigurations = true;
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
