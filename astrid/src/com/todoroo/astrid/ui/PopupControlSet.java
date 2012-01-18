package com.todoroo.astrid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.ThemeService;

public abstract class PopupControlSet extends TaskEditControlSet {

    protected final View displayView;
    protected final Activity activity;
    protected final Dialog dialog;
    protected final TextView displayText;

    public PopupControlSet(Activity activity, int viewLayout, int displayViewLayout, final int title) {
        super(activity, viewLayout);
        if (displayViewLayout != -1){
            this.displayView = LayoutInflater.from(activity).inflate(displayViewLayout, null);
            displayText = (TextView) displayView.findViewById(R.id.display_row_title);
            if (displayText != null) {
            displayText.setMaxLines(2);
            }
        }
        else {
            this.displayView = null;
            this.displayText = null;
        }

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
        int theme = ThemeService.getEditDialogTheme();
        final Dialog d = new Dialog(activity, theme);
        if (title == 0)
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        else
            d.setTitle(title);
        View v = getView();
        d.setContentView(v, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
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

        LayoutParams params = d.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;
        Configuration config = activity.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (AndroidUtilities.getSdkVersion() >= 9 && size == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            params.width = metrics.widthPixels / 2;
        }
        d.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

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
