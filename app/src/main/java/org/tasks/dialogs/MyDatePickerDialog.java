package org.tasks.dialogs;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener;
import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

public class MyDatePickerDialog extends DatePickerDialog implements OnDateSetListener {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  public static final int NO_DATE = -1;

  public static MyDatePickerDialog newDatePicker(Fragment target, int rc, long initial) {
    Bundle arguments = new Bundle();
    arguments.putLong(EXTRA_TIMESTAMP, initial);
    MyDatePickerDialog dialog = new MyDatePickerDialog();
    dialog.setArguments(arguments);
    dialog.setTargetFragment(target, rc);
    return dialog;
  }

  public interface DatePickerCallback{ // TODO: remove this after removing DateAndTimePickerActivity
    void onDatePicked(DialogInterface dialog, long timestamp);
  }

  private DatePickerCallback callback;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      long timestamp = getArguments().getLong(EXTRA_TIMESTAMP, currentTimeMillis());
      DateTime initial = (timestamp > 0 ? new DateTime(timestamp) : new DateTime()).startOfDay();
      initialize(
          null,
          initial.getYear(),
          initial.getMonthOfYear() - 1,
          initial.getDayOfMonth());
      setVersion(DatePickerDialog.Version.VERSION_2);
      int firstDayOfWeek = new Preferences(getContext()).getFirstDayOfWeek();
      if (firstDayOfWeek >= 1 && firstDayOfWeek <= 7) {
        setFirstDayOfWeek(firstDayOfWeek);
      }
      setThemeDark(getResources().getBoolean(R.bool.is_dark)); // TODO: remove this after removing DateAndTimePickerActivity
    }

    setOnDateSetListener(this);

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    if (context instanceof DatePickerCallback) {
      callback = (DatePickerCallback) context;
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    if (getTargetFragment() == null) {
      callback.onDatePicked(dialog, NO_DATE);
    } else {
      getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_CANCELED, null);
    }
  }

  @Override
  public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
    long result = new DateTime(year, monthOfYear + 1, dayOfMonth).getMillis();
    if (getTargetFragment() == null) {
      callback.onDatePicked(getDialog(), result);
    } else {
      Intent data = new Intent();
      data.putExtra(EXTRA_TIMESTAMP, result);
      getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, data);
    }
  }
}
