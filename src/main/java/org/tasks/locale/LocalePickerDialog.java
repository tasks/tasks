package org.tasks.locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.common.collect.Iterables.toArray;
import static org.tasks.locale.LocaleUtils.localeFromString;


public class LocalePickerDialog extends InjectingDialogFragment {

    public static LocalePickerDialog newLocalePickerDialog() {
        return new LocalePickerDialog();
    }

    public interface LocaleSelectionHandler {
        void onLocaleSelected(String locale);
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForApplication Context context;

    private LocaleSelectionHandler callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Map<String, String> translations = new HashMap<>();
        for (String translation : getResources().getStringArray(R.array.localization)) {
            translations.put(localeFromString(translation).getDisplayName(), translation);
        }
        final List<String> display = new ArrayList<>(translations.keySet());
        Collections.sort(display);
        return dialogBuilder.newDialog()
                .setItems(toArray(display, String.class), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        callback.onLocaleSelected(translations.get(display.get(i)));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.default_value, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        callback.onLocaleSelected(null);
                    }
                })
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
