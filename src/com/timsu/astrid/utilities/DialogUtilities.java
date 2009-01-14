package com.timsu.astrid.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.timsu.astrid.R;

public class DialogUtilities {

    public static void okDialog(Context context, String text,
            DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(context)
        .setTitle(R.string.information_title)
        .setMessage(text)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.ok, okListener)
        .show();
    }

    public static void okCancelDialog(Context context, String text,
            DialogInterface.OnClickListener okListener,
            DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(context)
        .setTitle(R.string.information_title)
        .setMessage(text)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.ok, okListener)
        .setNegativeButton(android.R.string.cancel, cancelListener)
        .show();
    }
}
