package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class DialogBuilder {
    private final Activity activity;
    private final ActivityPreferences activityPreferences;

    @Inject
    public DialogBuilder(Activity activity, ActivityPreferences activityPreferences) {
        this.activity = activity;
        this.activityPreferences = activityPreferences;
    }

    public AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(activity, activityPreferences.getDialogTheme());
    }

    public AlertDialog.Builder newMessageDialog(int message, Object... formatArgs) {
        return newDialog().setMessage(activity.getString(message, formatArgs));
    }

    public ProgressDialog newProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(activity, activityPreferences.getDialogTheme());
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
