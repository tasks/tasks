/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.app.Activity;
import android.app.Dialog;
import timber.log.Timber;

public class DialogUtilities {

  /** Dismiss a dialog off the UI thread */
  @Deprecated
  public static void dismissDialog(Activity activity, final Dialog dialog) {
    if (dialog == null) {
      return;
    }
    activity.runOnUiThread(
        () -> {
          try {
            dialog.dismiss();
          } catch (Exception e) {
            Timber.e(e);
          }
        });
  }
}
