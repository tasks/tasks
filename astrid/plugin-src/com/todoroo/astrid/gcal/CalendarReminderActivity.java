package com.todoroo.astrid.gcal;

import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.EditPreferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class CalendarReminderActivity extends Activity {

    public static final String TOKEN_NAMES = "names";
    public static final String TOKEN_EMAILS = "emails";
    public static final String TOKEN_EVENT_ID = "eventId";
    public static final String TOKEN_EVENT_NAME = "eventName";
    public static final String TOKEN_EVENT_START_TIME = "eventStartTime";
    public static final String TOKEN_EVENT_END_TIME = "eventEndTime";

    public static final String TOKEN_FROM_POSTPONE = "fromPostpone";

    private static final String PREF_IGNORE_PRESSES = "calEventsIgnored";

    // Prompt user to ignore all missed calls after this many ignore presses
    private static final int IGNORE_PROMPT_COUNT = 3;

    @Autowired
    private TagDataService tagDataService;

    private String eventName;
    private long startTime;
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
            AndroidUtilities.callOverridePendingTransition(CalendarReminderActivity.this, 0, android.R.anim.fade_out);
        }
    };

    private final OnClickListener ignoreListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            // Check for number of ignore presses
            int ignorePresses = Preferences.getInt(PREF_IGNORE_PRESSES, 0);
            ignorePresses++;
            if (ignorePresses == IGNORE_PROMPT_COUNT) {
                DialogUtilities.okCancelCustomDialog(CalendarReminderActivity.this,
                        getString(R.string.CRA_ignore_title),
                        getString(R.string.CRA_ignore_body),
                        R.string.CRA_ignore_all,
                        R.string.CRA_ignore_this,
                        0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Preferences.setBoolean(R.string.p_calendar_reminders, false);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new StartupService().onStartupApplication(this);
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.calendar_reminder_activity);


        Intent intent = getIntent();
        fromPostpone = intent.getBooleanExtra(TOKEN_FROM_POSTPONE, false);
        eventId = intent.getLongExtra(TOKEN_EVENT_ID, -1);
        eventName = intent.getStringExtra(TOKEN_EVENT_NAME);
        startTime = intent.getLongExtra(TOKEN_EVENT_START_TIME, DateUtilities.now());
        endTime = intent.getLongExtra(TOKEN_EVENT_END_TIME, DateUtilities.now() + DateUtilities.ONE_HOUR);

        createListButton = (TextView) findViewById(R.id.create_list);
        postponeButton = (TextView) findViewById(R.id.postpone);
        ignoreButton = (TextView) findViewById(R.id.ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
        dismissButton = findViewById(R.id.dismiss);

        setupUi();

        addListeners();
    }

    private void setupUi() {
        ((TextView) findViewById(R.id.reminder_title))
            .setText(getString(R.string.CRA_title));

        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        String speechText;
        if (fromPostpone)
            speechText = getString(R.string.CRA_speech_bubble_end, eventName);
        else
            speechText = getString(R.string.CRA_speech_bubble_start, eventName);

        dialogView.setText(speechText);

        createListButton.setBackgroundColor(getResources().getColor(ThemeService.getThemeColor()));

        if (fromPostpone)
            postponeButton.setVisibility(View.GONE);
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(ignoreListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(CalendarReminderActivity.this, EditPreferences.class);
                startActivity(editPreferences);
                dismissListener.onClick(v);
            }
        });

        if (!fromPostpone)
            postponeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    postpone();
                }
            });

        createListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String listName = getString(R.string.CRA_default_list_name, eventName);
                TagData existing = tagDataService.getTagByName(listName, TagData.PROPERTIES);
                if (existing != null) {
                    listExists(existing);
                } else {
                    createNewList(listName);
                }
            }
        });
    }

    private void listExists(final TagData tag) {
        DialogUtilities.okCancelCustomDialog(this,
            getString(R.string.CRA_list_exists_title),
            getString(R.string.CRA_list_exists_body, tag.getValue(TagData.NAME)),
            R.string.CRA_create_new,
            R.string.CRA_use_existing,
            0,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    createNewList(tag.getValue(TagData.NAME) + " "
                            + DateUtilities.getDateStringHideYear(CalendarReminderActivity.this, new Date(startTime)));
                }
            },
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FilterWithCustomIntent filter = TagFilterExposer.filterFromTagData(CalendarReminderActivity.this, tag);

                    Intent listIntent = new Intent(CalendarReminderActivity.this, TaskListActivity.class);
                    listIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
                    listIntent.putExtras(filter.customExtras);

                    startActivity(listIntent);
                    dismissButton.performClick();
                }
            });
    }

    private void createNewList(String name) {
        Intent newListIntent = new Intent(this, CalendarAlarmListCreator.class);
        newListIntent.putStringArrayListExtra(TOKEN_NAMES, getIntent().getStringArrayListExtra(TOKEN_NAMES));
        newListIntent.putStringArrayListExtra(TOKEN_EMAILS, getIntent().getStringArrayListExtra(TOKEN_EMAILS));
        newListIntent.putExtra(CalendarAlarmListCreator.TOKEN_LIST_NAME, name);

        startActivity(newListIntent);
        dismissButton.performClick(); // finish with animation
    }

    private void postpone() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent eventAlarm = new Intent(this, CalendarAlarmReceiver.class);
        eventAlarm.setAction(CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER);
        eventAlarm.setData(Uri.parse(CalendarAlarmScheduler.URI_PREFIX_POSTPONE + "://" + eventId));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER, eventAlarm, 0);

        am.cancel(pendingIntent);

        long alarmTime = endTime + DateUtilities.ONE_MINUTE * 5;
        if (Constants.DEBUG)
            Log.w(CalendarAlarmScheduler.TAG, "Scheduling calendar alarm for " + new Date(alarmTime));
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        dismissButton.performClick();
    }

}
