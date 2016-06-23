package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ListAdapter;

import org.tasks.preferences.ThemeManager;

public class AlertDialogBuilder {

    private final AlertDialog.Builder builder;
    private final Context context;
    private final ThemeManager themeManager;

    public AlertDialogBuilder(Context context, ThemeManager themeManager) {
        this.context = context;
        this.themeManager = themeManager;
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, themeManager.getDialogThemeResId());
        themeManager.applyThemeToContext(wrapper);
        builder = new AlertDialog.Builder(wrapper);
    }

    public AlertDialogBuilder setMessage(int message, Object... formatArgs) {
        return setMessage(context.getString(message, formatArgs));
    }

    public AlertDialogBuilder setMessage(String message) {
        builder.setMessage(message);
        return this;
    }

    public AlertDialogBuilder setPositiveButton(int ok, DialogInterface.OnClickListener onClickListener) {
        builder.setPositiveButton(ok, onClickListener);
        return this;
    }

    public AlertDialogBuilder setNegativeButton(int cancel, DialogInterface.OnClickListener onClickListener) {
        builder.setNegativeButton(cancel, onClickListener);
        return this;
    }

    public AlertDialogBuilder setTitle(int title) {
        builder.setTitle(title);
        return this;
    }

    public AlertDialogBuilder setItems(String[] strings, DialogInterface.OnClickListener onClickListener) {
        builder.setItems(strings, onClickListener);
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

    public AlertDialogBuilder setAdapter(ListAdapter adapter, DialogInterface.OnClickListener listener) {
        builder.setAdapter(adapter, listener);
        return this;
    }

    public AlertDialogBuilder setSingleChoiceItems(String[] strings, int selectedIndex, DialogInterface.OnClickListener onClickListener) {
        builder.setSingleChoiceItems(strings, selectedIndex, onClickListener);
        return this;
    }

    public AlertDialogBuilder setSingleChoiceItems(ListAdapter adapter, int selectedIndex, DialogInterface.OnClickListener onClickListener) {
        builder.setSingleChoiceItems(adapter, selectedIndex, onClickListener);
        return this;
    }

    public AlertDialogBuilder setNeutralButton(int reverse, DialogInterface.OnClickListener onClickListener) {
        builder.setNeutralButton(reverse, onClickListener);
        return this;
    }

    public AlertDialogBuilder setTitle(String title) {
        builder.setTitle(title);
        return this;
    }

    public AlertDialogBuilder setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        builder.setOnDismissListener(onDismissListener);
        return this;
    }

    public AlertDialogBuilder setCancelable(boolean b) {
        builder.setCancelable(b);
        return this;
    }

    public AlertDialog create() {
        AlertDialog dialog = builder.create();
        themeManager.applyThemeToContext(dialog.getContext());
        return dialog;
    }

    public AlertDialog showThemedListView() {
        AlertDialog dialog = create();
        themeManager.applyThemeToContext(dialog.getListView().getContext());
        dialog.show();
        return dialog;
    }

    public AlertDialog show() {
        AlertDialog dialog = create();
        dialog.show();
        return dialog;
    }
}
