package com.todoroo.astrid.calls;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.reminders.NotificationFragment.SnoozeDialog;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.service.ThemeService;

public class MissedCallActivity extends Activity {

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$

    private static final String PREF_IGNORE_PRESSES = "missedCallsIgnored";

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            AndroidUtilities.callOverridePendingTransition(MissedCallActivity.this, 0, android.R.anim.fade_out);
        }
    };

    private final OnClickListener ignoreListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            // Check for number of ignore presses
            int ignorePresses = Preferences.getInt(PREF_IGNORE_PRESSES, 0);
            ignorePresses++;
            if (ignorePresses % 3 == 0) {
                DialogUtilities.okCancelCustomDialog(MissedCallActivity.this,
                        getString(R.string.MCA_ignore_title),
                        getString(R.string.MCA_ignore_body),
                        R.string.MCA_ignore_all,
                        R.string.MCA_ignore_this,
                        0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Preferences.setBoolean(R.string.p_field_missed_calls, false);
                                dismissListener.onClick(v);
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissListener.onClick(v);
                            }
                        });
            } else {
                dismissListener.onClick(v);
            }
            Preferences.setInt(PREF_IGNORE_PRESSES, ignorePresses);
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
        ignoreButton.setOnClickListener(ignoreListener);
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

        callLaterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnoozeDialog sd = new SnoozeDialog(MissedCallActivity.this, new SnoozeCallback() {
                    @Override
                    public void snoozeForTime(long time) {
                        // Create task with due time 'time'
                        System.err.println("Should create a task for: " + new Date(time));
                        finish();
                    }
                });
                new AlertDialog.Builder(MissedCallActivity.this)
                    .setTitle(R.string.rmd_NoA_snooze)
                    .setView(sd)
                    .setPositiveButton(android.R.string.ok, sd)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show().setOwnerActivity(MissedCallActivity.this);
            }
        });
    }
}
