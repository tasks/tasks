package org.tasks.injection;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.tasks.activities.AddAttachmentActivity;
import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.dialogs.AccountSelectionDialog;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.dialogs.LocationPickerDialog;
import org.tasks.reminders.MissedCallDialog;
import org.tasks.reminders.NotificationDialog;
import org.tasks.reminders.SnoozeDialog;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
                LocationPickerDialog.class,
                NotificationDialog.class,
                SnoozeDialog.class,
                MissedCallDialog.class,
                CalendarSelectionDialog.class,
                AccountSelectionDialog.class,
                AddAttachmentDialog.class
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

    @Provides
    public Activity getActivity() {
        return dialogFragment.getActivity();
    }
}
