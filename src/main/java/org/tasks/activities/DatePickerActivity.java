package org.tasks.activities;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.dialogs.NativeDatePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static org.tasks.dialogs.NativeDatePickerDialog.newNativeDatePickerDialog;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class DatePickerActivity extends InjectingAppCompatActivity implements DatePickerDialog.OnDateSetListener, NativeDatePickerDialog.NativeDatePickerDialogCallback {

    private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";

    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    @Inject ThemeBase themeBase;
    @Inject ThemeAccent themeAccent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long timestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis());
        DateTime initial = (timestamp > 0 ? new DateTime(timestamp) : new DateTime()).startOfDay();

        FragmentManager fragmentManager = getFragmentManager();
        if (atLeastLollipop()) {
            if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
                newNativeDatePickerDialog(initial)
                        .show(fragmentManager, FRAG_TAG_DATE_PICKER);
            }
        } else {
            MyDatePickerDialog dialog = (MyDatePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER);
            if (dialog == null) {
                dialog = new MyDatePickerDialog();
                dialog.initialize(null, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
                dialog.setThemeDark(themeBase.isDarkTheme(this));
                dialog.setAccentColor(themeAccent.getAccentColor());
                dialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
            }
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
            dialog.setOnDateSetListener(this);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onDateSet(DatePickerDialog view, final int year, final int monthOfYear, final int dayOfMonth) {
        dateSet(year, monthOfYear, dayOfMonth);
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void onDateSet(final int year, final int month, final int day) {
        dateSet(year, month, day);
    }

    private void dateSet(final int year, final int month, final int day) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_TIMESTAMP, new DateTime(year, month + 1, day).getMillis());
        }});
        finish();
    }
}
