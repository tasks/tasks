package org.tasks.dialogs;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;
import javax.inject.Inject;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;

public class NativeDatePickerDialog extends InjectingNativeDialogFragment
    implements DatePickerDialog.OnDateSetListener {

  private static final String EXTRA_YEAR = "extra_year";
  private static final String EXTRA_MONTH = "extra_month";
  private static final String EXTRA_DAY = "extra_day";
  @Inject Theme theme;
  @Inject Preferences preferences;
  private NativeDatePickerDialogCallback callback;

  public static NativeDatePickerDialog newNativeDatePickerDialog(DateTime initial) {
    NativeDatePickerDialog dialog = new NativeDatePickerDialog();
    Bundle args = new Bundle();
    args.putInt(EXTRA_YEAR, initial.getYear());
    args.putInt(EXTRA_MONTH, initial.getMonthOfYear() - 1);
    args.putInt(EXTRA_DAY, initial.getDayOfMonth());
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  @SuppressLint("NewApi")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle args = getArguments();
    DatePickerDialog datePickerDialog =
        new DatePickerDialog(
            theme.wrap(getActivity()),
            this,
            args.getInt(EXTRA_YEAR),
            args.getInt(EXTRA_MONTH),
            args.getInt(EXTRA_DAY));
    int firstDayOfWeek = preferences.getFirstDayOfWeek();
    if (firstDayOfWeek >= 1 && firstDayOfWeek <= 7 && atLeastMarshmallow()) {
      datePickerDialog.getDatePicker().setFirstDayOfWeek(firstDayOfWeek);
    }
    datePickerDialog.setTitle("");
    return datePickerDialog;
  }

  @Override
  public void onDateSet(DatePicker datePicker, int year, int month, int day) {
    callback.onDateSet(year, month, day);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    callback.cancel();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (NativeDatePickerDialogCallback) activity;
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }

  public interface NativeDatePickerDialogCallback {

    void cancel();

    void onDateSet(int year, int month, int day);
  }
}
