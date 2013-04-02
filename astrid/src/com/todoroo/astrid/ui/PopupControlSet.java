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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;

public abstract class PopupControlSet extends TaskEditControlSet {

    protected final View displayView;
    protected Dialog dialog;
    private final String titleString;

    public interface PopupDialogClickListener {
        public boolean onClick(DialogInterface d, int which);
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

    public PopupControlSet(Activity activity, int viewLayout, int displayViewLayout, final int title) {
        super(activity, viewLayout);
        if (displayViewLayout != -1)
            this.displayView = LayoutInflater.from(activity).inflate(displayViewLayout, null);
        else
            this.displayView = null;

        titleString = (title > 0) ? activity.getString(title) : ""; //$NON-NLS-1$

        if (displayView != null) {
            displayView.setOnClickListener(getDisplayClickListener());
        }
    }
    @Override
    public View getDisplayView() {
        return displayView;
    }

    protected Dialog buildDialog(String title, final PopupDialogClickListener okClickListener, DialogInterface.OnCancelListener cancelClickListener) {
        int theme = ThemeService.getEditDialogTheme();
        dialog = new Dialog(activity, theme);
        if (title.length() == 0)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        else
            dialog.setTitle(title);

        View v = getView();

        dialog.setContentView(v, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        Button dismiss = (Button) v.findViewById(R.id.edit_dlg_ok);
        if (dismiss != null) {
            dismiss.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (okClickListener.onClick(dialog, 0))
                        DialogUtilities.dismissDialog(activity, dialog);
                }
            });
        }

        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;

        if (AndroidUtilities.isTabletSized(activity)) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            if ((metrics.widthPixels / metrics.density) >= AndroidUtilities.MIN_TABLET_HEIGHT)
                params.width = (3 * metrics.widthPixels) / 5;
            else if ((metrics.widthPixels / metrics.density) >= AndroidUtilities.MIN_TABLET_WIDTH)
                params.width = (4 * metrics.widthPixels) / 5;
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
    public String writeToModel(Task task) {
        if (initialized && dialog != null)
            dialog.dismiss();
        return super.writeToModel(task);
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        refreshDisplayView();
    }

    protected abstract void refreshDisplayView();
}
