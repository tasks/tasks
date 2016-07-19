package org.tasks.injection;

import org.tasks.dialogs.NativeThemePickerDialog;
import org.tasks.locale.LocalePickerDialog;

public interface BaseNativeDialogFragmentComponent {
    void inject(NativeThemePickerDialog themePickerDialog);

    void inject(LocalePickerDialog localePickerDialog);
}
