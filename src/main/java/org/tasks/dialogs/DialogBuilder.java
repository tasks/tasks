package org.tasks.dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AlertDialog;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class DialogBuilder {
    private Activity activity;
    private final ActivityPreferences activityPreferences;

    @Inject
    public DialogBuilder(Activity activity, ActivityPreferences activityPreferences) {
        this.activity = activity;
        this.activityPreferences = activityPreferences;
    }

    public AlertDialog.Builder newDialog() {
        return new AlertDialog.Builder(activity, activityPreferences.getDialogTheme());
    }

    public AlertDialog.Builder newMessageDialog(String message) {
        return newDialog().setMessage(message);
    }

    public AlertDialog.Builder newMessageDialog(int message, Object... formatArgs) {
        return newMessageDialog(activity.getString(message, formatArgs));
    }

    public ProgressDialog newProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(activity, activityPreferences.getDialogTheme());
        dialog.setMessage(activity.getString(R.string.DLG_wait));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
