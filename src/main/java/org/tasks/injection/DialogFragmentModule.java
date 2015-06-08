package org.tasks.injection;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.tasks.dialogs.LocationPickerDialog;
import org.tasks.reminders.NotificationDialog;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
                LocationPickerDialog.class,
                NotificationDialog.class
        },
        library = true)
public class DialogFragmentModule {
    private DialogFragment dialogFragment;

    public DialogFragmentModule(DialogFragment dialogFragment) {
        this.dialogFragment = dialogFragment;
    }

    @Provides
    public FragmentActivity getFragmentActivity() {
        return dialogFragment.getActivity();
    }
}
