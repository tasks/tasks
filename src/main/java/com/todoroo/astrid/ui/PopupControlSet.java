/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

public abstract class PopupControlSet extends TaskEditControlSetBase {

    protected final View displayView;
    protected final ActivityPreferences preferences;
    protected Dialog dialog;
    private final String titleString;

    public interface PopupDialogClickListener {
        boolean onClick(DialogInterface d, int which);
    }

    final PopupDialogClickListener okListener = new PopupDialogClickListener() {
        @Override
        public boolean onClick(DialogInterface d, int which) {
            return onOkClick();
        }
    };

    final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface d) {
            onCancelClick();
        }
    };

    public PopupControlSet(ActivityPreferences preferences, Activity activity, int viewLayout, int taskEditViewLayout, final int title) {
        super(activity, viewLayout, false);
        this.preferences = preferences;
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
        dialog = new Dialog(activity, preferences.getEditDialogTheme());
        if (title.length() == 0) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            dialog.setTitle(title);
        }

        View v = getDialogView();

        dialog.setContentView(v, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        Button dismiss = (Button) v.findViewById(R.id.edit_dlg_ok);
        if (dismiss != null) {
            dismiss.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (okClickListener.onClick(dialog, 0)) {
                        DialogUtilities.dismissDialog(activity, dialog);
                    }
                }
            });
        }

        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;

        if (ActivityPreferences.isTabletSized(activity)) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            if ((metrics.widthPixels / metrics.density) >= ActivityPreferences.MIN_TABLET_HEIGHT) {
                params.width = (3 * metrics.widthPixels) / 5;
            } else if ((metrics.widthPixels / metrics.density) >= ActivityPreferences.MIN_TABLET_WIDTH) {
                params.width = (4 * metrics.widthPixels) / 5;
            }
        }

        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        dialog.setOnCancelListener(cancelClickListener);
        dialog.setOwnerActivity(PopupControlSet.this.activity);
        additionalDialogSetup();
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

    protected void additionalDialogSetup() {
        // Will be called after dialog is set up.
        // Subclasses can override
    }

    /**
     * @return true if the dialog should be dismissed as the result of
     * the click. Default is true.
     */
    protected boolean onOkClick() {
        refreshDisplayView();
        return true;
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
