package org.tasks.locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.common.base.Function;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;


public class LocalePickerDialog extends InjectingDialogFragment {

    public static LocalePickerDialog newLocalePickerDialog() {
        return new LocalePickerDialog();
    }

    public interface LocaleSelectionHandler {
        void onLocaleSelected(Locale locale);
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForApplication Context context;
    @Inject Locale locale;

    private LocaleSelectionHandler callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Locale> locales = new ArrayList<>();
        locales.add(locale.withLanguage(null)); // device locale
        for (String override : getResources().getStringArray(R.array.localization)) {
            locales.add(locale.withLanguage(override));
        }
        final List<String> display = transform(locales, new Function<Locale, String>() {
            @Override
            public String apply(Locale input) {
                return input.getDisplayName();
            }
        });
        return dialogBuilder.newDialog()
                .setItems(display, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        callback.onLocaleSelected(locales.get(i));
                    }
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
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
