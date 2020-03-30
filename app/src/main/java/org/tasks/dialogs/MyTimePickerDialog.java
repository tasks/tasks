package org.tasks.dialogs;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog.OnTimeSetListener;
import org.tasks.R;
import org.tasks.time.DateTime;

public class MyTimePickerDialog extends TimePickerDialog implements OnTimeSetListener {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  public static final int NO_TIME = -1;

  public static MyTimePickerDialog newTimePicker(Fragment target, int rc, long initial) {
    Bundle arguments = new Bundle();
    arguments.putLong(EXTRA_TIMESTAMP, initial);
    MyTimePickerDialog dialog = new MyTimePickerDialog();
    dialog.setArguments(arguments);
    dialog.setTargetFragment(target, rc);
    return dialog;
  }

  public interface TimePickerCallback { // TODO: remove this after removing DateAndTimePickerActivity
    void onTimePicked(long timestamp);
  }

  private DateTime initial;
  private TimePickerCallback callback;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    initial = newDateTime(getArguments().getLong(EXTRA_TIMESTAMP, currentTimeMillis()));

    if (savedInstanceState == null) {
      initialize(
          null,
          initial.getHourOfDay(),
          initial.getMinuteOfHour(),
          0,
          DateFormat.is24HourFormat(getContext()));
      setThemeDark(getResources().getBoolean(R.bool.is_dark)); // TODO: remove this after removing DateAndTimePickerActivity
    }

    setOnTimeSetListener(this);

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    if (context instanceof TimePickerCallback) {
      callback = (TimePickerCallback) context;
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    if (getTargetFragment() == null) {
      callback.onTimePicked(NO_TIME);
    } else {
      getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_CANCELED, null);
    }
  }

  @Override
  public void onTimeSet(TimePickerDialog view, int hours, int minutes, int second) {
    long result = initial.startOfDay().withHourOfDay(hours).withMinuteOfHour(minutes).getMillis();
    if (getTargetFragment() == null) {
      callback.onTimePicked(result);
    } else {
      Intent data = new Intent();
      data.putExtra(EXTRA_TIMESTAMP, result);
      getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, data);
    }
  }
}
