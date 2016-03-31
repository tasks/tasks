package org.tasks.activities;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ThemeManager;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class DateAndTimePickerActivity extends InjectingAppCompatActivity implements DatePickerDialog.OnDateSetListener, DialogInterface.OnCancelListener {

    private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";

    private static final String EXTRA_DATE_SELECTED = "extra_date_selected";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    @Inject ThemeManager themeManager;

    private DateTime initial;
    private boolean dateSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initial = new DateTime(getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()));

        if (savedInstanceState != null) {
            dateSelected = savedInstanceState.getBoolean(EXTRA_DATE_SELECTED, false);
            if (dateSelected) {
                return;
            }
        }

        FragmentManager fragmentManager = getFragmentManager();
        MyDatePickerDialog datePickerDialog = (MyDatePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER);
        if (datePickerDialog == null) {
            datePickerDialog = new MyDatePickerDialog();
            datePickerDialog.initialize(null, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
            datePickerDialog.setAccentColor(themeManager.getAppTheme().getDateTimePickerAccent());
            datePickerDialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
        }
        datePickerDialog.setOnCancelListener(this);
        datePickerDialog.setOnDateSetListener(this);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_DATE_SELECTED, dateSelected);
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
        dateSelected = true;
        final long timestamp = new DateTime(year, month + 1, day)
                .withMillisOfDay(initial.getMillisOfDay())
                .getMillis();
        datePickerDialog.dismiss();
        startActivity(new Intent(this, TimePickerActivity.class) {{
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            putExtra(TimePickerActivity.EXTRA_TIMESTAMP, timestamp);
        }});
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
