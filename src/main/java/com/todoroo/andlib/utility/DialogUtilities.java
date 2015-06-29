/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class DialogUtilities {

    private static final Logger log = LoggerFactory.getLogger(DialogUtilities.class);
    private ActivityPreferences activityPreferences;

    @Inject
    public DialogUtilities(ActivityPreferences activityPreferences) {
        this.activityPreferences = activityPreferences;
    }

    /**
     * Displays a dialog box with an OK button
     */
    public static void okDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener) {
        if(activity.isFinishing()) {
            return;
        }

        tryOnUiThread(activity, new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(R.string.DLG_information_title)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, okListener)
                .show().setOwnerActivity(activity);
            }
        });
    }

    /**
     * Displays a dialog box with an OK button
     */
    public static void okDialog(final Activity activity, final String title,
            final int icon, final CharSequence text) {
        if(activity.isFinishing()) {
            return;
        }

        tryOnUiThread(activity, new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(text)
                .setIcon(icon)
                .setPositiveButton(android.R.string.ok, null)
                .show().setOwnerActivity(activity);
            }
        });
    }

    /**
     * Displays a dialog box with OK and Cancel buttons
     */
    public static void okCancelDialog(final Activity activity, final String text,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {

        okCancelCustomDialog(activity, text, android.R.string.ok, android.R.string.cancel, okListener, cancelListener);

    }

    /**
    * Displays a dialog box with custom titled OK and cancel button titles
    */

    public static void okCancelCustomDialog(final Activity activity, final String text,
            final int okTitleId, final int cancelTitleId,
            final DialogInterface.OnClickListener okListener,
            final DialogInterface.OnClickListener cancelListener) {
        if(activity.isFinishing()) {
            return;
        }

        tryOnUiThread(activity, new Runnable() {
            final boolean[] selectedOk = new boolean[1];
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setMessage(text)
                        .setPositiveButton(okTitleId, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectedOk[0] = true;
                                okListener.onClick(dialog, which);
                            }
                        })
                        .setNegativeButton(cancelTitleId, cancelListener)
                        .show();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!selectedOk[0] && cancelListener != null) {
                            cancelListener.onClick(dialog, Dialog.BUTTON_NEGATIVE);
                        }
                    }
                });
                dialog.setOwnerActivity(activity);
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
                    log.error(e.getMessage(), e);
                    DialogUtilities.okDialog(activity, activity.getString(R.string.DLG_error, e.toString()), null);
                } finally {
                    DialogUtilities.dismissDialog(activity, progressdiag);
                }
            }
        }).start();

        return progressdiag;
    }

    /**
     * Displays a progress dialog. Must be run on the UI thread
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
     */
    public static void dismissDialog(Activity activity, final Dialog dialog) {
        if(dialog == null) {
            return;
        }
        tryOnUiThread(activity, new Runnable() {
            @Override
            public void run() {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    // could have killed activity
                    log.error(e.getMessage(), e);
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
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
