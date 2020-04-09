package org.tasks.locale;

import static android.app.Activity.RESULT_OK;
import static com.google.common.collect.Lists.transform;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

public class LocalePickerDialog extends InjectingDialogFragment {

  public static final String EXTRA_LOCALE = "extra_locale";

  @Inject DialogBuilder dialogBuilder;
  @Inject Locale locale;

  public static LocalePickerDialog newLocalePickerDialog() {
    return new LocalePickerDialog();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final List<Locale> locales = new ArrayList<>();
    locales.add(locale.withLanguage(null)); // device locale
    for (String override : getResources().getStringArray(R.array.localization)) {
      locales.add(locale.withLanguage(override));
    }
    List<String> display = transform(locales, Locale::getDisplayName);
    return dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            display,
            display.indexOf(locale.getDisplayName()),
            (dialogInterface, i) -> {
              Locale locale = locales.get(i);
              Intent data = new Intent().putExtra(EXTRA_LOCALE, locale);
              getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, data);
              dialogInterface.dismiss();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
