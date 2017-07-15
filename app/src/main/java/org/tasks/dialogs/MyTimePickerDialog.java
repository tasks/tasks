package org.tasks.dialogs;

import android.content.DialogInterface;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

public class MyTimePickerDialog extends TimePickerDialog {

    private DialogInterface.OnDismissListener listener;

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (listener != null) {
            listener.onDismiss(dialog);
        }
    }
}
