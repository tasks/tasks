package org.tasks.dialogs;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ListAdapter;
import java.util.List;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public class AlertDialogBuilder {

  private final AlertDialog.Builder builder;
  private final Context context;
  private final Theme theme;
  private final Locale locale;

  AlertDialogBuilder(Context context, Theme theme, Locale locale) {
    this.context = context;
    this.theme = theme;
    this.locale = locale;
    ContextThemeWrapper wrapper = theme.getThemedDialog(context);
    theme.applyToContext(wrapper);
    builder = new AlertDialog.Builder(wrapper);
  }

  public AlertDialogBuilder setMessage(int message, Object... formatArgs) {
    return setMessage(context.getString(message, formatArgs));
  }

  public AlertDialogBuilder setMessage(String message) {
    builder.setMessage(message);
    return this;
  }

  public AlertDialogBuilder setMessage(CharSequence charSequence) {
    builder.setMessage(charSequence);
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

  public AlertDialogBuilder setItems(
      List<String> strings, DialogInterface.OnClickListener onClickListener) {
    return setItems(strings.toArray(new String[strings.size()]), onClickListener);
  }

  public AlertDialogBuilder setItems(
      String[] strings, DialogInterface.OnClickListener onClickListener) {
    builder.setItems(addDirectionality(strings.clone()), onClickListener);
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
    builder.setSingleChoiceItems(
        addDirectionality(strings.toArray(new String[strings.size()])),
        selectedIndex,
        onClickListener);
    return this;
  }

  private String[] addDirectionality(String[] strings) {
    if (atLeastJellybeanMR1()) {
      for (int i = 0; i < strings.length; i++) {
        strings[i] = withDirectionality(strings[i]);
      }
    }
    return strings;
  }

  private String withDirectionality(String string) {
    return locale.getDirectionalityMark() + string;
  }

  public AlertDialogBuilder setSingleChoiceItems(
      ListAdapter adapter, int selectedIndex, DialogInterface.OnClickListener onClickListener) {
    builder.setSingleChoiceItems(adapter, selectedIndex, onClickListener);
    return this;
  }

  public AlertDialogBuilder setNeutralButton(
      int reverse, DialogInterface.OnClickListener onClickListener) {
    builder.setNeutralButton(reverse, onClickListener);
    return this;
  }

  public AlertDialogBuilder setTitle(String title) {
    builder.setTitle(title);
    return this;
  }

  public AlertDialogBuilder setOnDismissListener(
      DialogInterface.OnDismissListener onDismissListener) {
    builder.setOnDismissListener(onDismissListener);
    return this;
  }

  public AlertDialog create() {
    AlertDialog dialog = builder.create();
    theme.applyToContext(dialog.getContext());
    return dialog;
  }

  public AlertDialog showThemedListView() {
    AlertDialog dialog = create();
    theme.applyToContext(dialog.getListView().getContext());
    dialog.show();
    locale.applyDirectionality(dialog);
    return dialog;
  }

  public AlertDialog show() {
    AlertDialog dialog = create();
    dialog.show();
    locale.applyDirectionality(dialog);
    return dialog;
  }
}
