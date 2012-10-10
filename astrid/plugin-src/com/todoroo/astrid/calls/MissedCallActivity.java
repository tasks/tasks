/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.calls;

import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import com.todoroo.astrid.activity.EditPreferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.NotificationFragment.SnoozeDialog;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.SnoozeCallback;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.ThemeService;

public class MissedCallActivity extends Activity {

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$
    public static final String EXTRA_TIME = "time";  //$NON-NLS-1$
    public static final String EXTRA_CONTACT_ID = "contactId"; //$NON-NLS-1$

    private static final String PREF_IGNORE_PRESSES = "missedCallsIgnored"; //$NON-NLS-1$

    // Prompt user to ignore all missed calls after this many ignore presses
    private static final int IGNORE_PROMPT_COUNT = 3;

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
            if (ignorePresses == IGNORE_PROMPT_COUNT) {
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
    private View ignoreSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new StartupService().onStartupApplication(this);
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.missed_call_activity);

        Intent intent = getIntent();

        name = intent.getStringExtra(EXTRA_NAME);
        number = intent.getStringExtra(EXTRA_NUMBER);
        timeString = intent.getStringExtra(EXTRA_TIME);
        long contactId = intent.getExtras().getLong(EXTRA_CONTACT_ID);

        int color = ThemeService.getThemeColor();

        returnCallButton = (TextView) findViewById(R.id.call_now);
        callLaterButton = (TextView) findViewById(R.id.call_later);
        ignoreButton = (TextView) findViewById(R.id.call_ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
        dismissButton = findViewById(R.id.dismiss);
        ((TextView) findViewById(R.id.reminder_title))
            .setText(getString(R.string.MCA_title,
                    TextUtils.isEmpty(name) ? number : name, timeString));

       ImageView pictureView = ((ImageView) findViewById(R.id.contact_picture));
       if (contactId >= 0) {
           Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
           InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), uri);
           Bitmap b = null;
           try {
               b = BitmapFactory.decodeStream(input);
           } catch (OutOfMemoryError e) {
               //
           }
           if (b != null) {
               pictureView.setImageBitmap(b);
               pictureView.setVisibility(View.VISIBLE);
           }
       }

        Resources r = getResources();
        returnCallButton.setBackgroundColor(r.getColor(color));
        callLaterButton.setBackgroundColor(r.getColor(color));

        addListeners();


        if (!Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            findViewById(R.id.missed_calls_speech_bubble).setVisibility(View.GONE);
        } else {
            TextView dialogView = (TextView) findViewById(R.id.reminder_message);
            dialogView.setText(Notifications.getRandomReminder(getResources().getStringArray(R.array.MCA_dialog_speech_options)));
        }
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(ignoreListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(MissedCallActivity.this, EditPreferences.class);
                startActivity(editPreferences);
                finish();
            }
        });

        returnCallButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent call = new Intent(Intent.ACTION_VIEW);
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
