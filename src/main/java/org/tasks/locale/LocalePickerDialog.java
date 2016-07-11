package org.tasks.locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;

import java.util.Arrays;
import java.util.List;

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
        final String[] translations = context.getResources().getStringArray(R.array.localization);
        final List<String> display = Lists.transform(Arrays.asList(translations), new Function<String, String>() {
            @Override
            public String apply(String locale) {
                return localeFromString(locale).getDisplayName();
            }
        });
        return dialogBuilder.newDialog()
                .setItems(toArray(display, String.class), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        callback.onLocaleSelected(translations[i]);
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
