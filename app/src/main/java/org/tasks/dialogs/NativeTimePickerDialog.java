package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TimePicker;
import com.todoroo.andlib.utility.DateUtilities;
import javax.inject.Inject;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;

public class NativeTimePickerDialog extends InjectingNativeDialogFragment
    implements TimePickerDialog.OnTimeSetListener {

  private static final String EXTRA_HOUR = "extra_hour";
  private static final String EXTRA_MINUTE = "extra_minute";
  @Inject Theme theme;
  private NativeTimePickerDialogCallback callback;

  public static NativeTimePickerDialog newNativeTimePickerDialog(DateTime initial) {
    NativeTimePickerDialog dialog = new NativeTimePickerDialog();
    Bundle args = new Bundle();
    args.putInt(EXTRA_HOUR, initial.getHourOfDay());
    args.putInt(EXTRA_MINUTE, initial.getMinuteOfHour());
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context context = theme.wrap(getActivity());
    Bundle args = getArguments();
    TimePickerDialog timePickerDialog =
        new TimePickerDialog(
            context,
            this,
            args.getInt(EXTRA_HOUR),
            args.getInt(EXTRA_MINUTE),
            DateUtilities.is24HourFormat(context));
    timePickerDialog.setTitle("");
    return timePickerDialog;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    callback.cancel();
  }

  @Override
  public void onTimeSet(TimePicker timePicker, int hour, int minute) {
    callback.onTimeSet(hour, minute);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (NativeTimePickerDialogCallback) activity;
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }

  public interface NativeTimePickerDialogCallback {

    void cancel();

    void onTimeSet(int hour, int minute);
  }
}
