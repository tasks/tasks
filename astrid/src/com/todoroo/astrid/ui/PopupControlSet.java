package com.todoroo.astrid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;

import com.todoroo.astrid.helper.TaskEditControlSet;

public abstract class PopupControlSet extends TaskEditControlSet {

    private final View displayView;
    protected final Activity activity;
    protected final Dialog dialog;

    public PopupControlSet(Activity activity, int viewLayout, int displayViewLayout, final int title) {
        super(activity, viewLayout);
        if (displayViewLayout != -1)
            this.displayView = LayoutInflater.from(activity).inflate(displayViewLayout, null);
        else
            this.displayView = null;

        final DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                refreshDisplayView();
            }
        };

        final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                refreshDisplayView();
            }
        };

        this.activity = activity;


        dialog = getDialogBuilder(title, okListener, cancelListener).create();
        dialog.setOwnerActivity(PopupControlSet.this.activity);

        if (displayView != null) {
            displayView.setOnClickListener(getDisplayClickListener());
        }
    }

    public View getDisplayView() {
        return displayView;
    }

    protected AlertDialog.Builder getDialogBuilder(int title, DialogInterface.OnClickListener okListener, DialogInterface.OnCancelListener cancelListener) {
        return new AlertDialog.Builder(this.activity)
        .setTitle(title)
        .setView(getView())
        .setPositiveButton(android.R.string.ok, okListener)
        .setOnCancelListener(cancelListener);
    }

    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        };
    }

    protected abstract void refreshDisplayView();
}
