package org.tasks.dialogs;

import android.content.DialogInterface;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;

public class MyCalendarDatePickerDialog extends CalendarDatePickerDialog {

    private DialogInterface.OnDismissListener listener;

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (listener != null) {
            listener.onDismiss(dialog);
        }
    }
}
