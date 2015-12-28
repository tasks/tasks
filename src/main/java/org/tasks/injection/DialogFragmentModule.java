package org.tasks.injection;

import android.app.Activity;
import android.app.DialogFragment;

import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.dialogs.AccountSelectionDialog;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.reminders.MissedCallDialog;
import org.tasks.reminders.NotificationDialog;
import org.tasks.reminders.SnoozeDialog;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
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
    public Activity getActivity() {
        return dialogFragment.getActivity();
    }
}
