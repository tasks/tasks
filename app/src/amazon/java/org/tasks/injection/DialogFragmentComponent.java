package org.tasks.injection;

import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.RecordAudioDialog;
import org.tasks.dialogs.SortDialog;
import org.tasks.reminders.MissedCallDialog;
import org.tasks.reminders.NotificationDialog;
import org.tasks.reminders.SnoozeDialog;

import dagger.Subcomponent;

@Subcomponent(modules = DialogFragmentModule.class)
public interface DialogFragmentComponent {
    void inject(NotificationDialog notificationDialog);

    void inject(MissedCallDialog missedCallDialog);

    void inject(CalendarSelectionDialog calendarSelectionDialog);

    void inject(AddAttachmentDialog addAttachmentDialog);

    void inject(SnoozeDialog snoozeDialog);

    void inject(SortDialog sortDialog);

    void inject(ColorPickerDialog colorPickerDialog);

    void inject(RecordAudioDialog recordAudioDialog);
}
