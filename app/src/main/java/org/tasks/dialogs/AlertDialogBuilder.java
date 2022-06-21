package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.ListAdapter;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class AlertDialogBuilder {

  private final AlertDialog.Builder builder;
  private final Context context;

  AlertDialogBuilder(Context context) {
    this.context = context;
    builder = new MaterialAlertDialogBuilder(context);
  }

  public AlertDialogBuilder setMessage(int message, Object... formatArgs) {
    return setMessage(context.getString(message, formatArgs));
  }

  public AlertDialogBuilder setMessage(String message) {
    builder.setMessage(message);
    return this;
  }

  public AlertDialogBuilder setPositiveButton(
      int ok, DialogInterface.OnClickListener onClickListener) {
    builder.setPositiveButton(ok, onClickListener);
    return this;
  }

  public AlertDialogBuilder setNegativeButton(
      int cancel, DialogInterface.OnClickListener onClickListener) {
    builder.setNegativeButton(cancel, onClickListener);
    return this;
  }

  public AlertDialogBuilder setTitle(int title) {
    builder.setTitle(title);
    return this;
  }

  public AlertDialogBuilder setTitle(int title, Object... formatArgs) {
    builder.setTitle(context.getString(title, formatArgs));
    return this;
  }

  public AlertDialogBuilder setItems(
      List<String> strings, DialogInterface.OnClickListener onClickListener) {
    return setItems(strings.toArray(new String[0]), onClickListener);
  }

  public AlertDialogBuilder setItems(
      String[] strings, DialogInterface.OnClickListener onClickListener) {
    builder.setItems(strings.clone(), onClickListener);
    return this;
  }

  public AlertDialogBuilder setAdapter(
      ListAdapter adapter, DialogInterface.OnClickListener onClickListener) {
    builder.setAdapter(adapter, onClickListener);
    return this;
  }

  public AlertDialogBuilder setView(View dialogView) {
    builder.setView(dialogView);
    return this;
  }

  public AlertDialogBuilder setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
    builder.setOnCancelListener(onCancelListener);
    return this;
  }

  public AlertDialogBuilder setSingleChoiceItems(
      List<String> strings, int selectedIndex, DialogInterface.OnClickListener onClickListener) {
    return setSingleChoiceItems(strings.toArray(new String[0]), selectedIndex, onClickListener);
  }

  public AlertDialogBuilder setSingleChoiceItems(
      String[] strings, int selectedIndex, OnClickListener onClickListener) {
    builder.setSingleChoiceItems(strings, selectedIndex, onClickListener);
    return this;
  }

  public AlertDialogBuilder setSingleChoiceItems(
      ListAdapter adapter, int selectedIndex, DialogInterface.OnClickListener onClickListener) {
    builder.setSingleChoiceItems(adapter, selectedIndex, onClickListener);
    return this;
  }

  public AlertDialogBuilder setNeutralButton(
      int resId, DialogInterface.OnClickListener onClickListener) {
    builder.setNeutralButton(resId, onClickListener);
    return this;
  }

  AlertDialogBuilder setTitle(String title) {
    builder.setTitle(title);
    return this;
  }

  public AlertDialogBuilder setOnDismissListener(
      DialogInterface.OnDismissListener onDismissListener) {
    builder.setOnDismissListener(onDismissListener);
    return this;
  }

  public AlertDialogBuilder setCancelable(boolean cancelable) {
    builder.setCancelable(cancelable);
    return this;
  }

  public AlertDialog create() {
    return builder.create();
  }

  public AlertDialog show() {
    AlertDialog dialog = create();
    dialog.show();
    return dialog;
  }
}
