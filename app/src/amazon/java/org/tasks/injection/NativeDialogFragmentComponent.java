package org.tasks.injection;

import org.tasks.dialogs.DonationDialog;
import org.tasks.dialogs.ExportTasksDialog;
import org.tasks.dialogs.ImportTasksDialog;
import org.tasks.dialogs.NativeDatePickerDialog;
import org.tasks.dialogs.NativeTimePickerDialog;
import org.tasks.dialogs.SeekBarDialog;
import org.tasks.locale.LocalePickerDialog;

import dagger.Subcomponent;

@Subcomponent(modules = NativeDialogFragmentModule.class)
public interface NativeDialogFragmentComponent {
    void inject(LocalePickerDialog localePickerDialog);

    void inject(NativeDatePickerDialog nativeDatePickerDialog);

    void inject(NativeTimePickerDialog nativeTimePickerDialog);

    void inject(SeekBarDialog seekBarDialog);

    void inject(ExportTasksDialog exportTasksDialog);

    void inject(ImportTasksDialog importTasksDialog);

    void inject(DonationDialog donationDialog);
}
