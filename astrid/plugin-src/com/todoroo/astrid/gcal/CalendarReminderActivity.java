package com.todoroo.astrid.gcal;

import java.util.Date;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

@SuppressWarnings("nls")
public class CalendarReminderActivity extends Activity {

    public static final String TOKEN_NAMES = "names";
    public static final String TOKEN_EMAILS = "emails";
    public static final String TOKEN_EVENT_NAME = "eventName";
    public static final String TOKEN_EVENT_TIME = "eventTime";

    private static final String PREF_IGNORE_PRESSES = "calEventsIgnored";

    // Prompt user to ignore all missed calls after this many ignore presses
    private static final int IGNORE_PROMPT_COUNT = 3;

    @Autowired
    private TagDataService tagDataService;

    private String eventName;
    private long eventTime;

    private TextView ignoreButton;
    private TextView createListButton;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new StartupService().onStartupApplication(this);
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);

        setContentView(R.layout.calendar_reminder_activity);


        Intent intent = getIntent();
        eventName = intent.getStringExtra(TOKEN_EVENT_NAME);
        eventTime = intent.getLongExtra(TOKEN_EVENT_TIME, DateUtilities.now());

        createListButton = (TextView) findViewById(R.id.create_list);
        ignoreButton = (TextView) findViewById(R.id.ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
        dismissButton = findViewById(R.id.dismiss);

        setupUi();

        addListeners();
    }

    private void setupUi() {
        ((TextView) findViewById(R.id.reminder_title))
            .setText(getString(R.string.CRA_title, eventName));

        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        dialogView.setText(getString(R.string.CRA_speech_bubble));

        createListButton.setBackgroundColor(getResources().getColor(ThemeService.getThemeColor()));
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(ignoreListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(CalendarReminderActivity.this, EditPreferences.class);
                startActivity(editPreferences);
                finish();
            }
        });

        createListButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TagData existing = tagDataService.getTag(eventName, TagData.PROPERTIES);
                if (existing != null) {
                    listExists(existing);
                } else {
                    createNewList(eventName);
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
                                + DateUtilities.getDateStringHideYear(CalendarReminderActivity.this, new Date(eventTime)));
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

    private void createNewList(String defaultName) {
        Intent newListIntent = new Intent(this, CreateEventListActivity.class);
        newListIntent.putStringArrayListExtra(TOKEN_NAMES, getIntent().getStringArrayListExtra(TOKEN_NAMES));
        newListIntent.putStringArrayListExtra(TOKEN_EMAILS, getIntent().getStringArrayListExtra(TOKEN_EMAILS));
        newListIntent.putExtra(CreateEventListActivity.TOKEN_LIST_NAME, defaultName);

        startActivity(newListIntent);
        dismissButton.performClick(); // finish with animation
    }

}
