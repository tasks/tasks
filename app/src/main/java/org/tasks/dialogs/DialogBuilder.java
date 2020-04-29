package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import javax.inject.Inject;
import org.tasks.locale.Locale;

public class DialogBuilder {

  private final Activity activity;
  private final Locale locale;

  @Inject
  public DialogBuilder(Activity activity, Locale locale) {
    this.activity = activity;
    this.locale = locale;
  }

  public AlertDialogBuilder newDialog() {
    return new AlertDialogBuilder(activity, locale);
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
    return new ProgressDialog(activity);
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
