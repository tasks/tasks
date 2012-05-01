package com.todoroo.astrid.calls;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.NotificationFragment.SnoozeDialog;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;

public class MissedCallActivity extends Activity {

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$
    public static final String EXTRA_TIME = "time";  //$NON-NLS-1$
    public static final String EXTRA_PHOTO = "photo"; //$NON-NLS-1$

    private static final String PREF_IGNORE_PRESSES = "missedCallsIgnored"; //$NON-NLS-1$

    @Autowired private TaskService taskService;

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
    private String timeString;

    private TextView returnCallButton;
    private TextView callLaterButton;
    private TextView ignoreButton;
    private View dismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.missed_call_activity);

        Intent intent = getIntent();

        name = intent.getStringExtra(EXTRA_NAME);
        number = intent.getStringExtra(EXTRA_NUMBER);
        timeString = intent.getStringExtra(EXTRA_TIME);
        String picture = intent.getStringExtra(EXTRA_PHOTO);

        int color = ThemeService.getThemeColor();

        returnCallButton = (TextView) findViewById(R.id.call_now);
        callLaterButton = (TextView) findViewById(R.id.call_later);
        ignoreButton = (TextView) findViewById(R.id.call_ignore);
        dismissButton = findViewById(R.id.dismiss);
        ((TextView) findViewById(R.id.reminder_title))
            .setText(getString(R.string.MCA_title,
                    TextUtils.isEmpty(name) ? number : name, timeString));

       ImageView pictureView = ((ImageView) findViewById(R.id.contact_picture));
       if (TextUtils.isEmpty(picture))
           pictureView.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_picture_2));
       else
           pictureView.setImageURI(Uri.parse(picture));

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
                final String taskTitle;
                String dialogTitle;
                if (TextUtils.isEmpty(name)) {
                    taskTitle = getString(R.string.MCA_task_title_no_name, number);
                    dialogTitle = getString(R.string.MCA_schedule_dialog_title, number);
                } else {
                    taskTitle = getString(R.string.MCA_task_title_name, name, number);
                    dialogTitle = getString(R.string.MCA_schedule_dialog_title, name);
                }
                SnoozeDialog sd = new SnoozeDialog(MissedCallActivity.this, new SnoozeCallback() {
                    @Override
                    public void snoozeForTime(long time) {

                        Task newTask = new Task();
                        newTask.setValue(Task.TITLE, taskTitle);
                        newTask.setValue(Task.DUE_DATE, time);
                        taskService.save(newTask);

                        finish();
                    }
                });
                new AlertDialog.Builder(MissedCallActivity.this)
                    .setTitle(dialogTitle)
                    .setView(sd)
                    .setPositiveButton(android.R.string.ok, sd)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show().setOwnerActivity(MissedCallActivity.this);
            }
        });
    }
}
