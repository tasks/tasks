package com.todoroo.astrid.gcal;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.service.StartupService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.ResourceResolver;
import org.tasks.scheduling.AlarmManager;
import org.tasks.scheduling.CalendarNotificationIntentService;

import javax.inject.Inject;

public class CalendarReminderActivity extends ThemedInjectingAppCompatActivity {

    public static final String TOKEN_EVENT_ID = "eventId";
    public static final String TOKEN_EVENT_NAME = "eventName";
    public static final String TOKEN_EVENT_END_TIME = "eventEndTime";

    public static final String TOKEN_FROM_POSTPONE = "fromPostpone";

    private static final String PREF_IGNORE_PRESSES = "calEventsIgnored";

    // Prompt user to ignore all missed calls after this many ignore presses
    private static final int IGNORE_PROMPT_COUNT = 3;

    @Inject StartupService startupService;
    @Inject Preferences preferences;
    @Inject ResourceResolver resourceResolver;
    @Inject DialogBuilder dialogBuilder;
    @Inject AlarmManager alarmManager;

    private String eventName;
    private long endTime;
    private long eventId;

    private boolean fromPostpone;

    private TextView ignoreButton;
    private TextView createListButton;
    private TextView postponeButton;
    private View dismissButton;
    private View ignoreSettingsButton;

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private final OnClickListener ignoreListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            // Check for number of ignore presses
            int ignorePresses = preferences.getInt(PREF_IGNORE_PRESSES, 0);
            ignorePresses++;
            if (ignorePresses == IGNORE_PROMPT_COUNT) {
                dialogBuilder.newMessageDialog(R.string.CRA_ignore_body)
                        .setPositiveButton(R.string.CRA_ignore_all, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                preferences.setBoolean(R.string.p_calendar_reminders, false);
                                dismissListener.onClick(v);
                            }
                        })
                        .setNegativeButton(R.string.CRA_ignore_this, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissListener.onClick(v);
                            }
                        })
                        .show();
            } else {
                dismissListener.onClick(v);
            }
            preferences.setInt(PREF_IGNORE_PRESSES, ignorePresses);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);

        setContentView(R.layout.calendar_reminder_activity);

        Intent intent = getIntent();
        fromPostpone = intent.getBooleanExtra(TOKEN_FROM_POSTPONE, false);
        eventId = intent.getLongExtra(TOKEN_EVENT_ID, -1);
        eventName = intent.getStringExtra(TOKEN_EVENT_NAME);
        endTime = intent.getLongExtra(TOKEN_EVENT_END_TIME, DateUtilities.now() + DateUtilities.ONE_HOUR);

        createListButton = (TextView) findViewById(R.id.create_list);
        postponeButton = (TextView) findViewById(R.id.postpone);
        ignoreButton = (TextView) findViewById(R.id.ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
        dismissButton = findViewById(R.id.dismiss);

        setupUi();

        addListeners();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void setupUi() {
        ((TextView) findViewById(R.id.reminder_title))
            .setText(getString(R.string.CRA_title));

        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        String speechText;
        if (fromPostpone) {
            speechText = getString(R.string.CRA_speech_bubble_end, eventName);
        } else {
            speechText = getString(R.string.CRA_speech_bubble_start, eventName);
        }

        dialogView.setText(speechText);

        createListButton.setBackgroundColor(resourceResolver.getData(R.attr.asThemeTextColor));

        if (fromPostpone) {
            postponeButton.setVisibility(View.GONE);
        }
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(ignoreListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(CalendarReminderActivity.this, BasicPreferences.class);
                startActivity(editPreferences);
                dismissListener.onClick(v);
            }
        });

        if (!fromPostpone) {
            postponeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    postpone();
                }
            });
        }

        createListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewList(getString(R.string.CRA_default_list_name, eventName));
            }
        });
    }

    private void createNewList(final String name) {
        startActivity(new Intent(CalendarReminderActivity.this, TaskListActivity.class) {{
            putExtra(TaskListActivity.TOKEN_CREATE_NEW_LIST_NAME, name);
        }});
        dismissButton.performClick(); // finish with animation
    }

    private void postpone() {
        Intent eventAlarm = new Intent(this, CalendarAlarmReceiver.class);
        eventAlarm.setAction(CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER);
        eventAlarm.setData(Uri.parse(CalendarNotificationIntentService.URI_PREFIX_POSTPONE + "://" + eventId));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER, eventAlarm, 0);

        alarmManager.cancel(pendingIntent);

        long alarmTime = endTime + DateUtilities.ONE_MINUTE * 5;
        alarmManager.wakeup(alarmTime, pendingIntent);
        dismissButton.performClick();
    }

}
