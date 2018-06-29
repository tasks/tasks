package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.activities.RemoteListSupportPicker;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.RecordAudioDialog;
import org.tasks.dialogs.SortDialog;
import org.tasks.gtasks.CreateListDialog;
import org.tasks.gtasks.DeleteListDialog;
import org.tasks.gtasks.RenameListDialog;
import org.tasks.reminders.MissedCallDialog;
import org.tasks.reminders.NotificationDialog;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.repeats.BasicRecurrenceDialog;
import org.tasks.repeats.CustomRecurrenceDialog;

@Subcomponent(modules = DialogFragmentModule.class)
public interface DialogFragmentComponent {

  void inject(RemoteListSupportPicker remoteListSupportPicker);

  void inject(NotificationDialog notificationDialog);

  void inject(MissedCallDialog missedCallDialog);

  void inject(CalendarSelectionDialog calendarSelectionDialog);

  void inject(AddAttachmentDialog addAttachmentDialog);

  void inject(SnoozeDialog snoozeDialog);

  void inject(SortDialog sortDialog);

  void inject(ColorPickerDialog colorPickerDialog);

  void inject(RecordAudioDialog recordAudioDialog);

  void inject(CreateListDialog createListDialog);

  void inject(DeleteListDialog deleteListDialog);

  void inject(RenameListDialog renameListDialog);

  void inject(CustomRecurrenceDialog customRecurrenceDialog);

  void inject(BasicRecurrenceDialog basicRecurrenceDialog);
}
