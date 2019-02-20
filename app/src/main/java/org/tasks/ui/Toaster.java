package org.tasks.ui;

import static android.widget.Toast.LENGTH_LONG;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;
import javax.inject.Inject;
import org.tasks.injection.ForActivity;
import org.tasks.locale.Locale;

public class Toaster {

  private final Context context;
  private final Locale locale;

  @Inject
  public Toaster(@ForActivity Context context, Locale locale) {
    this.context = context;
    this.locale = locale;
  }

  public void longToast(@StringRes int resId) {
    longToast(context.getString(resId));
  }

  public void longToast(@StringRes int resId, int number) {
    longToast(context.getString(resId, locale.formatNumber(number)));
  }

  private void longToast(String text) {
    Toast.makeText(context, text, LENGTH_LONG).show();
  }

  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  public void longToastUnformatted(@StringRes int resId, int number) {
    Toast.makeText(context, context.getString(resId, number), LENGTH_LONG).show();
  }
}
