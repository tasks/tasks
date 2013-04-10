package com.todoroo.astrid.ui;

import android.app.Dialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.AstridActivity;

public class AstridDialog extends Dialog {

    private final Button[] buttons;
    private final TextView title;
    private final TextView message;
    private final LinearLayout root;

    public AstridDialog(AstridActivity activity, boolean forcePortrait) {
        super(activity, R.style.ReminderDialog);
        setContentView(forcePortrait ? R.layout.astrid_dialog_view_portrait : R.layout.astrid_dialog_view);

        buttons = new Button[3];
        buttons[0] = (Button) findViewById(R.id.button0);
        buttons[1] = (Button) findViewById(R.id.button1);
        buttons[2] = (Button) findViewById(R.id.button2);

        title = (TextView) findViewById(R.id.dialog_title);
        message = (TextView) findViewById(R.id.reminder_message);
        root = (LinearLayout) findViewById(R.id.dialog_root);

        findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        setOwnerActivity(activity);
    }

    public AstridDialog setButtonText(int resId, int buttonIndex) {
        buttons[buttonIndex].setText(resId);
        buttons[buttonIndex].setVisibility(View.VISIBLE);
        return this;
    }

    public AstridDialog setButtonColor(int color, int buttonIndex) {
        buttons[buttonIndex].setBackgroundColor(color);
        return this;
    }

    public AstridDialog setButtonListener(View.OnClickListener listener, int buttonIndex) {
        buttons[buttonIndex].setOnClickListener(listener);
        return this;
    }

    public AstridDialog setButtonListeners(View.OnClickListener... listeners) {
        int index = 0;
        for (View.OnClickListener l : listeners) {
            buttons[index].setOnClickListener(l);
            index++;
            if (index >= buttons.length)
                break;
        }
        return this;
    }

    public AstridDialog setAstridText(int resId) {
        message.setText(resId);
        return this;
    }

    public AstridDialog setAstridTitle(int resId) {
        title.setText(resId);
        return this;
    }

    public AstridDialog addView(View v, int index) {
        root.addView(v, index);
        return this;
    }

}
