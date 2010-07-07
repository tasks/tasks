package com.todoroo.andlib.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;

public class DialogUtilities {

    @Autowired
    public Integer informationDialogTitleResource;

    @Autowired
    public Integer confirmDialogTitleResource;

    public DialogUtilities() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Displays a dialog box with a EditText and an ok / cancel
     *
     * @param activity
     * @param text
     * @param okListener
     */
    public void viewDialog(final Activity activity, final String text,
            final View view, final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(confirmDialogTitleResource)
                .setMessage(text)
                .setView(view)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .show();
            }
        });
    }

    /**
     * Displays a dialog box with an OK button
     *
     * @param activity
     * @param text
     * @param okListener
     */
    public void okDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(informationDialogTitleResource)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .show();
            }
        });
    }

    /**
     * Displays a dialog box with an OK button
     *
     * @param activity
     * @param text
     * @param okListener
     */
    public void okDialog(final Activity activity, final int icon, final CharSequence text,
            final DialogInterface.OnClickListener okListener) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(informationDialogTitleResource)
                .setMessage(text)
                .setIcon(icon)
                .setPositiveButton(android.R.string.ok, okListener)
                .show();
            }
        });
    }

    /**
     * Displays a dialog box with OK and Cancel buttons and custom title
     *
     * @param activity
     * @param title
     * @param text
     * @param okListener
     * @param cancelListener
     */
    public void okCancelDialog(final Activity activity, final String title,
            final String text, final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .show();
            }
        });
    }

    /**
     * Displays a dialog box with OK and Cancel buttons
     *
     * @param activity
     * @param text
     * @param okListener
     * @param cancelListener
     */
    public void okCancelDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(informationDialogTitleResource)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .show();
            }
        });
    }

    /**
     * Displays a progress dialog. Must be run on the UI thread
     * @param context
     * @param text
     * @return
     */
    public ProgressDialog progressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(text);
        dialog.show();
        return dialog;
    }

    /**
     * Dismiss a dialog off the UI thread
     *
     * @param activity
     * @param dialog
     */
    public void dismissDialog(Activity activity, final ProgressDialog dialog) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    // could have killed activity
                }
            }
        });
    }
}
