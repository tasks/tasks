package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;

import javax.inject.Inject;

public class DialogBuilder {

  private final Activity activity;

  @Inject
  public DialogBuilder(Activity activity) {
    this.activity = activity;
  }

  public AlertDialogBuilder newDialog() {
    return new AlertDialogBuilder(activity);
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
