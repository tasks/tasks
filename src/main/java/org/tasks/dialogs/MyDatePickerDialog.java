package org.tasks.dialogs;

import android.content.DialogInterface;

import com.fourmob.datetimepicker.date.DatePickerDialog;

public class MyDatePickerDialog extends DatePickerDialog {

    private DialogInterface.OnDismissListener listener;

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (listener != null) {
            listener.onDismiss(dialog);
        }
    }
}
