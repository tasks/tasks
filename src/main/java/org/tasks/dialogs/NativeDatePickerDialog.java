package org.tasks.dialogs;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;

import javax.inject.Inject;

public class NativeDatePickerDialog extends InjectingNativeDialogFragment implements DatePickerDialog.OnDateSetListener {

    public static NativeDatePickerDialog newNativeDatePickerDialog(DateTime initial) {
        NativeDatePickerDialog dialog = new NativeDatePickerDialog();
        dialog.initial = initial;
        return dialog;
    }

    public interface NativeDatePickerDialogCallback {
        void cancel();

        void onDateSet(int year, int month, int day);
    }

    @Inject Theme theme;

    private NativeDatePickerDialogCallback callback;
    private DateTime initial;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(theme.wrap(getActivity()), this, 0, 0, 0);
        if (initial != null) {
            datePickerDialog.updateDate(initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
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
}
