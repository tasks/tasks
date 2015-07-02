/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;


import android.app.Activity;
import android.app.Dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogUtilities {

    private static final Logger log = LoggerFactory.getLogger(DialogUtilities.class);

    /**
     * Dismiss a dialog off the UI thread
     */
    @Deprecated
    public static void dismissDialog(Activity activity, final Dialog dialog) {
        if(dialog == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    dialog.dismiss();
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
