package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.activities.RemoteListNativePicker;
import org.tasks.dialogs.ExportTasksDialog;
import org.tasks.dialogs.ImportTasksDialog;
import org.tasks.dialogs.NativeDatePickerDialog;
import org.tasks.dialogs.NativeTimePickerDialog;
import org.tasks.dialogs.SeekBarDialog;
import org.tasks.locale.LocalePickerDialog;

@Subcomponent(modules = NativeDialogFragmentModule.class)
public interface NativeDialogFragmentComponent {

  void inject(RemoteListNativePicker remoteListNativePicker);

  void inject(LocalePickerDialog localePickerDialog);

  void inject(NativeDatePickerDialog nativeDatePickerDialog);

  void inject(NativeTimePickerDialog nativeTimePickerDialog);

  void inject(SeekBarDialog seekBarDialog);

  void inject(ExportTasksDialog exportTasksDialog);

  void inject(ImportTasksDialog importTasksDialog);
}
