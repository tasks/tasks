package org.tasks.injection;

import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.widget.WidgetConfigDialog;

import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.dialogs.AccountSelectionDialog;
import org.tasks.dialogs.AddAttachmentDialog;
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

    void inject(AccountSelectionDialog accountSelectionDialog);

    void inject(SnoozeDialog snoozeDialog);

    void inject(WidgetConfigDialog widgetConfigDialog);

    void inject(ThemePickerDialog themePickerDialog);
}
