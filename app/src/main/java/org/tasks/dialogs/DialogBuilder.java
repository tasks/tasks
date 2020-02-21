package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import com.todoroo.andlib.utility.AndroidUtilities;
import javax.inject.Inject;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public class DialogBuilder {

  private final Activity activity;
  private final Theme theme;
  private final Locale locale;

  @Inject
  public DialogBuilder(Activity activity, Theme theme, Locale locale) {
    this.activity = activity;
    this.theme = theme;
    this.locale = locale;
  }

  public AlertDialogBuilder newDialog() {
    return new AlertDialogBuilder(activity, theme, locale);
  }

  public AlertDialogBuilder newDialog(int title) {
    return newDialog().setTitle(title);
  }

  public AlertDialogBuilder newDialog(String title) {
    return newDialog().setTitle(title);
  }

  public AlertDialogBuilder newDialog(int title, Object... formatArgs) {
    return newDialog().setTitle(title, formatArgs);
  }

  ProgressDialog newProgressDialog() {
    ProgressDialog progressDialog =
        new ProgressDialog(activity, theme.getThemeBase().getAlertDialogStyle());
    theme.applyToContext(progressDialog.getContext());
    if (AndroidUtilities.preLollipop()) {
      ColorDrawable background =
          new ColorDrawable(activity.getResources().getColor(android.R.color.transparent));
      progressDialog.getWindow().setBackgroundDrawable(background);
    }
    return progressDialog;
  }

  public ProgressDialog newProgressDialog(int messageId) {
    ProgressDialog dialog = newProgressDialog();
    dialog.setIndeterminate(true);
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    dialog.setMessage(activity.getString(messageId));
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }
}
