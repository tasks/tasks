package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.preferences.ThemeManager;

import javax.inject.Inject;

public class DialogBuilder {
    private final Activity activity;
    private ThemeManager themeManager;

    @Inject
    public DialogBuilder(Activity activity, ThemeManager themeManager) {
        this.activity = activity;
        this.themeManager = themeManager;
    }

    public AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(buildDialogWrapper());
    }

    public AlertDialog.Builder newMessageDialog(int message, Object... formatArgs) {
        return newDialog().setMessage(activity.getString(message, formatArgs));
    }

    public ProgressDialog newProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(buildDialogWrapper());
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

    private ContextThemeWrapper buildDialogWrapper() {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(activity, themeManager.getBaseTheme().getDialogThemeResId());
        Resources.Theme theme = wrapper.getTheme();
        theme.applyStyle(themeManager.getColorTheme().getResId(), true);
        theme.applyStyle(themeManager.getAccentTheme().getResId(), true);
        return wrapper;
    }
}
