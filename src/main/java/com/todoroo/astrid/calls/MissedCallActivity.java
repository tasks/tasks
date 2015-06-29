/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.calls;

import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingFragmentActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.ResourceResolver;

import java.io.InputStream;

import javax.inject.Inject;

public class MissedCallActivity extends InjectingFragmentActivity {

    private static final Logger log = LoggerFactory.getLogger(MissedCallActivity.class);

    public static final String EXTRA_NUMBER = "number"; //$NON-NLS-1$
    public static final String EXTRA_NAME = "name"; //$NON-NLS-1$
    public static final String EXTRA_TIME = "time";  //$NON-NLS-1$
    public static final String EXTRA_CONTACT_ID = "contactId"; //$NON-NLS-1$

    private static final String PREF_IGNORE_PRESSES = "missedCallsIgnored"; //$NON-NLS-1$

    // Prompt user to ignore all missed calls after this many ignore presses
    private static final int IGNORE_PROMPT_COUNT = 3;

    @Inject StartupService startupService;
    @Inject TaskService taskService;
    @Inject ActivityPreferences preferences;
    @Inject ResourceResolver resourceResolver;

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
            int ignorePresses = preferences.getInt(PREF_IGNORE_PRESSES, 0);
            ignorePresses++;
            if (ignorePresses == IGNORE_PROMPT_COUNT) {
                DialogUtilities.okCancelCustomDialog(MissedCallActivity.this,
                        getString(R.string.MCA_ignore_body),
                        R.string.MCA_ignore_all,
                        R.string.MCA_ignore_this,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                preferences.setBoolean(R.string.p_field_missed_calls, false);
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
            preferences.setInt(PREF_IGNORE_PRESSES, ignorePresses);
        }
    };

    private String name;
    private String number;

    private TextView returnCallButton;
    private TextView callLaterButton;
    private TextView ignoreButton;
    private View ignoreSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);

        setContentView(R.layout.missed_call_activity);

        Intent intent = getIntent();

        name = intent.getStringExtra(EXTRA_NAME);
        number = intent.getStringExtra(EXTRA_NUMBER);
        String timeString = intent.getStringExtra(EXTRA_TIME);
        long contactId = intent.getExtras().getLong(EXTRA_CONTACT_ID);

        int color = resourceResolver.getData(R.attr.asThemeTextColor);

        returnCallButton = (TextView) findViewById(R.id.call_now);
        callLaterButton = (TextView) findViewById(R.id.call_later);
        ignoreButton = (TextView) findViewById(R.id.call_ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
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
               log.error(e.getMessage(), e);
           }
           if (b != null) {
               pictureView.setImageBitmap(b);
               pictureView.setVisibility(View.VISIBLE);
           }
       }

        returnCallButton.setBackgroundColor(color);
        callLaterButton.setBackgroundColor(color);

        addListeners();
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(ignoreListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(MissedCallActivity.this, BasicPreferences.class);
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
                Task task = new Task() {{
                    setTitle(TextUtils.isEmpty(name)
                            ? getString(R.string.MCA_task_title_no_name, number)
                            : getString(R.string.MCA_task_title_name, name, number));
                }};
                taskService.save(task);
                TaskIntents
                        .getEditTaskStack(MissedCallActivity.this, null, task.getId())
                        .startActivities();
            }
        });
    }
}
