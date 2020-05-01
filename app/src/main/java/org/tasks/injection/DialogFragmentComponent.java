package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.activities.RemoteListPicker;
import org.tasks.calendars.CalendarPicker;
import org.tasks.dialogs.AddAttachmentDialog;
import org.tasks.dialogs.ColorPalettePicker;
import org.tasks.dialogs.ColorWheelPicker;
import org.tasks.dialogs.DateTimePicker;
import org.tasks.dialogs.ExportTasksDialog;
import org.tasks.dialogs.GeofenceDialog;
import org.tasks.dialogs.IconPickerDialog;
import org.tasks.dialogs.ImportTasksDialog;
import org.tasks.dialogs.NewFilterDialog;
import org.tasks.dialogs.RecordAudioDialog;
import org.tasks.dialogs.SortDialog;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.dialogs.WhatsNewDialog;
import org.tasks.locale.LocalePickerDialog;
import org.tasks.reminders.NotificationDialog;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.repeats.BasicRecurrenceDialog;
import org.tasks.repeats.CustomRecurrenceDialog;

@Subcomponent(modules = DialogFragmentModule.class)
public interface DialogFragmentComponent {

  void inject(RemoteListPicker remoteListPicker);

  void inject(NotificationDialog notificationDialog);

  void inject(CalendarPicker calendarPicker);

  void inject(AddAttachmentDialog addAttachmentDialog);

  void inject(SnoozeDialog snoozeDialog);

  void inject(SortDialog sortDialog);

  void inject(RecordAudioDialog recordAudioDialog);

  void inject(CustomRecurrenceDialog customRecurrenceDialog);

  void inject(BasicRecurrenceDialog basicRecurrenceDialog);

  void inject(GeofenceDialog geofenceDialog);

  void inject(IconPickerDialog iconPickerDialog);

  void inject(ExportTasksDialog exportTasksDialog);

  void inject(ImportTasksDialog importTasksDialog);

  void inject(LocalePickerDialog localePickerDialog);

  void inject(ThemePickerDialog themePickerDialog);

  void inject(ColorWheelPicker colorWheelPicker);

  void inject(ColorPalettePicker colorPalettePicker);

  void inject(DateTimePicker dateTimePicker);

  void inject(NewFilterDialog newFilterDialog);

  void inject(WhatsNewDialog whatsNewDialog);
}
