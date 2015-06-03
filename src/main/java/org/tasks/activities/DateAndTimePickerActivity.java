package org.tasks.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.fourmob.datetimepicker.date.DatePickerDialog;

import org.joda.time.DateTime;
import org.tasks.dialogs.MyDatePickerDialog;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;

public class DateAndTimePickerActivity extends FragmentActivity implements DatePickerDialog.OnDateSetListener, DialogInterface.OnDismissListener {

    private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
    private static final int REQUEST_PICK_TIME = 12345;

    public static final String EXTRA_TIMESTAMP = "extra_timestamp";
    private static final String EXTRA_DATE_SELECTED = "extra_date_selected";

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

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        MyDatePickerDialog datePickerDialog = (MyDatePickerDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER);
        if (datePickerDialog == null) {
            datePickerDialog = new MyDatePickerDialog();
            datePickerDialog.initialize(null, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth(), false);
            datePickerDialog.show(supportFragmentManager, FRAG_TAG_DATE_PICKER);
        }
        datePickerDialog.setOnDismissListener(this);
        datePickerDialog.setOnDateSetListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_DATE_SELECTED, dateSelected);
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
        dateSelected = true;
        datePickerDialog.dismiss();
        final long timestamp = initial.withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day).getMillis();
        startActivityForResult(new Intent(this, TimePickerActivity.class) {{
            putExtra(TimePickerActivity.EXTRA_TIMESTAMP, timestamp);
        }}, REQUEST_PICK_TIME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == REQUEST_PICK_TIME) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(RESULT_OK, new Intent() {{
                    putExtra(EXTRA_TIMESTAMP, data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L));
                }});
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
