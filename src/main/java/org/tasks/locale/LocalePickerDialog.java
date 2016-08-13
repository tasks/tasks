package org.tasks.locale;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;


public class LocalePickerDialog extends InjectingNativeDialogFragment {

    public static LocalePickerDialog newLocalePickerDialog() {
        return new LocalePickerDialog();
    }

    public interface LocaleSelectionHandler {
        void onLocaleSelected(Locale locale);
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject Locale locale;

    private LocaleSelectionHandler callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Locale> locales = new ArrayList<>();
        locales.add(locale.withLanguage(null)); // device locale
        for (String override : getResources().getStringArray(R.array.localization)) {
            locales.add(locale.withLanguage(override));
        }
        final List<String> display = transform(locales, Locale::getDisplayName);
        return dialogBuilder.newDialog()
                .setItems(display, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    callback.onLocaleSelected(locales.get(i));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (LocaleSelectionHandler) activity;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
