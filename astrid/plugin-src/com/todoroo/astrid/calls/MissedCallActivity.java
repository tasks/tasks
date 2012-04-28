package com.todoroo.astrid.calls;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.service.ThemeService;

public class MissedCallActivity extends Activity {

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            AndroidUtilities.callOverridePendingTransition(MissedCallActivity.this, 0, android.R.anim.fade_out);
        }
    };

    private String name;
    private String number;

    private TextView returnCallButton;
    private TextView callLaterButton;
    private TextView ignoreButton;
    private View dismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.missed_call_activity);

        Intent intent = getIntent();

        name = intent.getStringExtra(EXTRA_NAME);
        number = intent.getStringExtra(EXTRA_NUMBER);

        int color = ThemeService.getThemeColor();

        returnCallButton = (TextView) findViewById(R.id.call_now);
        callLaterButton = (TextView) findViewById(R.id.call_later);
        ignoreButton = (TextView) findViewById(R.id.call_ignore);
        dismissButton = findViewById(R.id.dismiss);

        Resources r = getResources();
        returnCallButton.setBackgroundColor(r.getColor(color));
        callLaterButton.setBackgroundColor(r.getColor(color));

        addListeners();

        String dialog;

        if (TextUtils.isEmpty(name)) {
            dialog = getString(R.string.MCA_dialog_without_name, number);
        } else {
            dialog = getString(R.string.MCA_dialog_with_name, name);
        }

        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        dialogView.setText(dialog);
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(dismissListener);
        dismissButton.setOnClickListener(dismissListener);

        returnCallButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent call = new Intent(Intent.ACTION_CALL);
                call.setData(Uri.parse("tel:" + number)); //$NON-NLS-1$
                startActivity(call);
                finish();
            }
        });
    }
}
