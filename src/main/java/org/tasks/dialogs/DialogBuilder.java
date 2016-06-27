package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.themes.Theme;

import javax.inject.Inject;

public class DialogBuilder {
    private final Activity activity;
    private final Theme theme;

    @Inject
    public DialogBuilder(Activity activity, Theme theme) {
        this.activity = activity;
        this.theme = theme;
    }

    public AlertDialogBuilder newDialog() {
        return new AlertDialogBuilder(activity, theme);
    }

    @Deprecated
    public AlertDialogBuilder newMessageDialog(int message, Object... formatArgs) {
        return newDialog().setMessage(message, formatArgs);
    }

    public ProgressDialog newProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(activity, theme.getDialogStyle());
        theme.applyToContext(progressDialog.getContext());
        if (AndroidUtilities.preLollipop()) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
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
