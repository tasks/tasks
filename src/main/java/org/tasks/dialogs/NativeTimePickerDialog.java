package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TimePicker;

import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;

import javax.inject.Inject;

public class NativeTimePickerDialog extends InjectingNativeDialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static NativeTimePickerDialog newNativeTimePickerDialog(DateTime initial) {
        NativeTimePickerDialog dialog = new NativeTimePickerDialog();
        dialog.initial = initial;
        return dialog;
    }

    public interface NativeTimePickerDialogCallback {
        void cancel();

        void onTimeSet(int hour, int minute);
    }

    @Inject Theme theme;

    private NativeTimePickerDialogCallback callback;
    private DateTime initial;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = theme.wrap(getActivity());
        TimePickerDialog timePickerDialog = new TimePickerDialog(context, this, 0, 0, DateUtilities.is24HourFormat(context));
        timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialogInterface, i) -> {
            callback.cancel();
        });
        if (initial != null) {
            timePickerDialog.updateTime(initial.getHourOfDay(), initial.getMinuteOfHour());
        }
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
}
