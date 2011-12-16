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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;

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
            public void onClick(DialogInterface d, int which) {
                onOkClick();
            }
        };

        final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                onCancelClick();
            }
        };

        this.activity = activity;


        dialog = buildDialog(title, okListener, cancelListener);


        if (displayView != null) {
            displayView.setOnClickListener(getDisplayClickListener());
        }
    }

    @Override
    public View getDisplayView() {
        return displayView;
    }

    protected Dialog buildDialog(int title, final DialogInterface.OnClickListener okListener, DialogInterface.OnCancelListener cancelListener) {
        int theme = ThemeService.getDialogTheme();
        final Dialog d = new Dialog(activity, theme);
        if (title == 0)
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        else
            d.setTitle(title);
        View v = getView();
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        d.setContentView(v, new LayoutParams(metrics.widthPixels - (int)(30 * metrics.density), LayoutParams.WRAP_CONTENT));
        Button dismiss = (Button) v.findViewById(R.id.edit_dlg_ok);
        if (dismiss != null) {
            dismiss.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    okListener.onClick(d, 0);
                    DialogUtilities.dismissDialog(activity, d);
                }
            });
        }
        d.setOnCancelListener(cancelListener);
        d.setOwnerActivity(PopupControlSet.this.activity);
        return d;
    }

    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        };
    }

    protected void onOkClick() {
        refreshDisplayView();
    }

    protected void onCancelClick() {
        refreshDisplayView();
    }

    protected abstract void refreshDisplayView();
}
