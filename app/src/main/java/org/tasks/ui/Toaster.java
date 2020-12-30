package org.tasks.ui;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;
import dagger.hilt.android.qualifiers.ActivityContext;
import javax.inject.Inject;
import org.tasks.locale.Locale;

public class Toaster {

  private final Context context;
  private final Locale locale;

  @Inject
  public Toaster(@ActivityContext Context context, Locale locale) {
    this.context = context;
    this.locale = locale;
  }

  public void longToast(@StringRes int resId, int number) {
    longToast(context.getString(resId, locale.formatNumber(number)));
  }

  public void longToast(@StringRes int resId) {
    longToast(context.getString(resId));
  }

  public void longToast(String text) {
    toast(text, LENGTH_LONG);
  }

  public void toast(@StringRes int resId, Object... args) {
    toast(context.getString(resId, args), LENGTH_SHORT);
  }

  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  public void longToastUnformatted(@StringRes int resId, int number) {
    Toast.makeText(context, context.getString(resId, number), LENGTH_LONG).show();
  }

  private void toast(String text, int duration) {
    if (!isNullOrEmpty(text)) {
      Toast.makeText(context, text, duration).show();
    }
  }
}
