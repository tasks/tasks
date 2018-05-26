package org.tasks.locale;

import static com.google.common.collect.Lists.transform;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.ThemeAccent;
import org.tasks.ui.SingleCheckedArrayAdapter;

public class LocalePickerDialog extends InjectingNativeDialogFragment {

  @Inject @ForActivity Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject ThemeAccent themeAccent;
  @Inject Locale locale;
  private LocaleSelectionHandler callback;

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
    final List<String> display = transform(locales, Locale::getDisplayName);
    SingleCheckedArrayAdapter adapter =
        new SingleCheckedArrayAdapter(context, display, themeAccent);
    return dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            adapter,
            display.indexOf(locale.getDisplayName()),
            (dialogInterface, i) -> {
              callback.onLocaleSelected(locales.get(i));
              dialogInterface.dismiss();
            })
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

  public interface LocaleSelectionHandler {

    void onLocaleSelected(Locale locale);
  }
}
