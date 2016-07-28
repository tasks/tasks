package org.tasks.injection;

import org.tasks.dialogs.NativeDatePickerDialog;
import org.tasks.dialogs.NativeTimePickerDialog;
import org.tasks.locale.LocalePickerDialog;

public interface BaseNativeDialogFragmentComponent {
    void inject(LocalePickerDialog localePickerDialog);

    void inject(NativeDatePickerDialog nativeDatePickerDialog);

    void inject(NativeTimePickerDialog nativeTimePickerDialog);
}
