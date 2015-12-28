/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.ActivityPreferences;

public abstract class PopupControlSet extends TaskEditControlSetBase {

    protected final View displayView;
    protected final ActivityPreferences preferences;
    private DialogBuilder dialogBuilder;
    protected AlertDialog dialog;
    private final String titleString;

    public interface PopupDialogClickListener {
        boolean onClick(DialogInterface d, int which);
    }

    final PopupDialogClickListener okListener = new PopupDialogClickListener() {
        @Override
        public boolean onClick(DialogInterface d, int which) {
            onOkClick();
            return true;
        }
    };

    final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface d) {
            onCancelClick();
        }
    };

    public PopupControlSet(ActivityPreferences preferences, Activity activity, int viewLayout,
                           int taskEditViewLayout, final int title, DialogBuilder dialogBuilder) {
        super(activity, viewLayout, false);
        this.preferences = preferences;
        this.dialogBuilder = dialogBuilder;
        if (taskEditViewLayout != -1) {
            this.displayView = inflateWithTemplate(taskEditViewLayout);
        } else {
            this.displayView = null;
        }

        titleString = (title > 0) ? activity.getString(title) : ""; //$NON-NLS-1$

        if (displayView != null) {
            displayView.setOnClickListener(getDisplayClickListener());
        }
    }

    @Override
    public View getView() {
        return displayView;
    }

    protected View getDialogView() {
        return super.getView();
    }

    protected Dialog buildDialog(String title, final PopupDialogClickListener okClickListener, DialogInterface.OnCancelListener cancelClickListener) {
        AlertDialog.Builder builder = dialogBuilder.newDialog()
                .setView(getDialogView())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (okClickListener.onClick(dialog, 0)) {
                            dialog.dismiss();
                        }
                    }
                })
                .setOnCancelListener(cancelClickListener);

        additionalDialogSetup(builder);
        dialog = builder.show();
        return dialog;
    }

    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog == null) {
                    buildDialog(titleString, okListener, cancelListener);
                }
                dialog.show();
            }
        };
    }

    protected void additionalDialogSetup(AlertDialog.Builder builder) {
        // Will be called after dialog is set up.
        // Subclasses can override
    }

    protected void onOkClick() {
        refreshDisplayView();
    }

    protected void onCancelClick() {
        refreshDisplayView();
    }

    public Dialog getDialog() {
        return dialog;
    }

    @Override
    public void writeToModel(Task task) {
        if (initialized && dialog != null) {
            dialog.dismiss();
        }
        super.writeToModel(task);
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        refreshDisplayView();
    }

    protected abstract void refreshDisplayView();
}
