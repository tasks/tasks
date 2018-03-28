package org.tasks.dialogs;

import android.content.DialogInterface;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

public class MyDatePickerDialog extends DatePickerDialog {

  private DialogInterface.OnCancelListener listener;

  @Override
  public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
    this.listener = listener;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    if (listener != null) {
      listener.onCancel(dialog);
    }
  }
}
