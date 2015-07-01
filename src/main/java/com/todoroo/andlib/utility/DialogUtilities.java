/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;

@Deprecated
public class DialogUtilities {

    private static final Logger log = LoggerFactory.getLogger(DialogUtilities.class);

    /**
     * Displays a progress dialog. Must be run on the UI thread
     */
    public static ProgressDialog progressDialog(Activity context, String text) {
        ProgressDialog dialog = new ProgressDialog(context, R.style.TasksDialog);
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
