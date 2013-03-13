/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.webkit.WebView;

import com.todoroo.astrid.api.R;

public class DialogUtilities {

    /**
     * Displays a dialog box with a EditText and an ok / cancel
     *
     * @param activity
     * @param text
     * @param okListener
     */
    public static void viewDialog(final Activity activity, final String text,
            final View view, final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        if(activity.isFinishing())
            return;

        tryOnUiThread(activity, new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(R.string.DLG_question_title)
                .setMessage(text)
                .setView(view)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .show().setOwnerActivity(activity);
            }
        });
    }

    /**
     * Display an OK dialog with HTML content
     *
     * @param context
     * @param html
     * @param title
     */
    @SuppressWarnings("nls")
    public static void htmlDialog(Context context, String html, int title) {
        WebView webView = new WebView(context);
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
        webView.setBackgroundColor(0);

        new AlertDialog.Builder(context)
        .setTitle(title)
        .setView(webView)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, null)
        .show();
    }

    /**
     * Displays a dialog box with an OK button
     *
     * @param activity
     * @param text
     * @param okListener
     */
    public static void okDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener) {
        if(activity.isFinishing())
            return;

        tryOnUiThread(activity, new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(R.string.DLG_information_title)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, okListener)
                .show().setOwnerActivity(activity);
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
    public static void okDialog(final Activity activity, final String title,
            final int icon, final CharSequence text,
            final DialogInterface.OnClickListener okListener) {
        if(activity.isFinishing())
            return;

        tryOnUiThread(activity, new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(text)
                .setIcon(icon)
                .setPositiveButton(android.R.string.ok, okListener)
                .show().setOwnerActivity(activity);
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
    public static void okCancelDialog(final Activity activity, final String title,
            final String text, final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {

        okCancelCustomDialog(activity, title, text, android.R.string.ok, android.R.string.cancel, android.R.drawable.ic_dialog_alert, okListener, cancelListener);
    }

    /**
     * Displays a dialog box with OK and Cancel buttons
     *
     * @param activity
     * @param text
     * @param okListener
     * @param cancelListener
     */
    public static void okCancelDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {

        okCancelCustomDialog(activity, activity.getString(R.string.DLG_confirm_title), text, android.R.string.ok, android.R.string.cancel, android.R.drawable.ic_dialog_alert, okListener, cancelListener);

    }

    /**
    * Displays a dialog box with custom titled OK and cancel button titles
    */

    public static void okCancelCustomDialog(final Activity activity, final String title, final String text,
            final int okTitleId, final int cancelTitleId,
            final int icon,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        if(activity.isFinishing())
            return;

        tryOnUiThread(activity, new Runnable() {
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(R.string.DLG_confirm_title)
                .setMessage(text)
                .setTitle(title)
                .setIcon(icon)
                .setPositiveButton(okTitleId, okListener)
                .setNegativeButton(cancelTitleId, cancelListener)
                .show().setOwnerActivity(activity);
            }
        });
    }

    /** Run runnable with progress dialog */
    public static ProgressDialog runWithProgressDialog(final Activity activity, final Runnable runnable) {
        final ProgressDialog progressdiag = progressDialog(activity, activity.getString(R.string.DLG_wait));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    DialogUtilities.okDialog(activity,
                            activity.getString(R.string.DLG_error, e.toString()), null);
                } finally {
                    DialogUtilities.dismissDialog(activity, progressdiag);
                }
            }
        }).start();

        return progressdiag;
    }

    /**
     * Displays a progress dialog. Must be run on the UI thread
     * @param context
     * @param text
     * @return
     */
    public static ProgressDialog progressDialog(Activity context, String text) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(text);
        dialog.show();
        dialog.setOwnerActivity(context);
        return dialog;
    }

    /**
     * Dismiss a dialog off the UI thread
     *
     * @param activity
     * @param dialog
     */
    public static void dismissDialog(Activity activity, final Dialog dialog) {
        if(dialog == null)
            return;
        tryOnUiThread(activity, new Runnable() {
            public void run() {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    // could have killed activity
                }
            }
        });
    }


    private static void tryOnUiThread(Activity activity, final Runnable runnable) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    // probably window was closed
                }
            }
        });
    }
}
