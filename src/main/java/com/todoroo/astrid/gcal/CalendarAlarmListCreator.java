package com.todoroo.astrid.gcal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.todoroo.andlib.utility.AndroidUtilities;

import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.BasicPreferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.ResourceResolver;

import java.util.ArrayList;

import javax.inject.Inject;

public class CalendarAlarmListCreator extends InjectingAppCompatActivity {

    public static final String TOKEN_LIST_NAME = "listName"; //$NON-NLS-1$

    @Inject TagDataDao tagDataDao;
    @Inject ActivityPreferences preferences;
    @Inject ResourceResolver resourceResolver;

    private ArrayList<String> names;
    private ArrayList<String> emails;

    private String tagName;
    private TextView inviteAll;
    private TextView moreOptions;
    private TextView ignoreButton;
    private View dismissButton;
    private View ignoreSettingsButton;

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TagData tagData = new TagData();
            tagData.setName(tagName);
            tagDataDao.persist(tagData);
            dismissWithAnimation();
        }
    };

    private void dismissWithAnimation() {
        finish();
        AndroidUtilities.callOverridePendingTransition(CalendarAlarmListCreator.this, 0, android.R.anim.fade_out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyTranslucentDialogTheme();
        setContentView(R.layout.calendar_alarm_list_creator);

        Intent intent = getIntent();

        tagName = intent.getStringExtra(TOKEN_LIST_NAME);
        inviteAll = (TextView) findViewById(R.id.invite_all);
        moreOptions = (TextView) findViewById(R.id.list_settings);
        ignoreButton = (TextView) findViewById(R.id.ignore);
        dismissButton = findViewById(R.id.dismiss);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);
        emails = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_EMAILS);
        names = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_NAMES);

        setupUi();

        addListeners();
    }

    private void setupUi() {
        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        StringBuilder builder = new StringBuilder(getString(R.string.CRA_created_list_dialog, tagName));
        String attendeesString = buildAttendeesString();
        int color = resourceResolver.getData(R.attr.asThemeTextColor);

        String title;
        if (!TextUtils.isEmpty(attendeesString)) {
            builder.append(" ") //$NON-NLS-1$
            .append(attendeesString)
            .append(" ") //$NON-NLS-1$
            .append(getString(R.string.CRA_invitation_prompt));
            inviteAll.setBackgroundColor(color);
            title = getString(R.string.CRA_share_list_title);
        } else {
            title = getString(R.string.CRA_list_created_title);
            moreOptions.setBackgroundColor(color);
            inviteAll.setVisibility(View.GONE);
            ignoreButton.setVisibility(View.GONE);
            ignoreSettingsButton.setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.reminder_title))
            .setText(title);

        dialogView.setText(builder.toString());
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(dismissListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(CalendarAlarmListCreator.this, BasicPreferences.class);
                startActivity(editPreferences);
                dismissListener.onClick(v);
            }
        });

        inviteAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set members json and save
                moreOptions.performClick();
            }
        });

        moreOptions.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarAlarmListCreator.this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.TOKEN_CREATE_NEW_LIST_NAME, tagName);
                intent.putExtra(TaskListActivity.TOKEN_CREATE_NEW_LIST, true);
                startActivity(intent);
                dismissWithAnimation();
            }
        });
    }

    private String buildAttendeesString() {
        if (emails.size() == 0) {
            return ""; //$NON-NLS-1$
        } else if (emails.size() == 1) {
            String displayName = getDisplayName(0);
            return getString(R.string.CRA_one_attendee, displayName);
        } else { // emails.size() >= 2
            String displayName1 = getDisplayName(0);
            String displayName2 = getDisplayName(1);

            int extras = emails.size() - 2;
            if (extras > 0) {
                return getString(R.string.CRA_many_attendees, displayName1, displayName2, extras);
            } else {
                return getString(R.string.CRA_two_attendees, displayName1, displayName2);
            }

        }
    }

    private String getDisplayName(int index) {
        String name = names.get(index);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        return emails.get(index);
    }
}
