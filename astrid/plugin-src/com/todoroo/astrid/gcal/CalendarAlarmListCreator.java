package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.EditPreferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;

public class CalendarAlarmListCreator extends Activity {

    public static final String TOKEN_LIST_NAME = "listName"; //$NON-NLS-1$

    @Autowired
    private UserDao userDao;

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private TagMetadataDao tagMetadataDao;

    private ArrayList<String> names;
    private ArrayList<String> emails;
    private HashMap<String, User> emailsToUsers;

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
            tagData.setValue(TagData.NAME, tagName);
            tagDataService.save(tagData);
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
        DependencyInjectionService.getInstance().inject(this);
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

        initializeUserMap();

        setupUi();

        addListeners();
    }

    private void initializeUserMap() {
        emailsToUsers = new HashMap<String, User>();
        TodorooCursor<User> users = userDao.query(Query.select(User.PROPERTIES).where(User.EMAIL.in(emails.toArray(new String[emails.size()]))));
        try {
            for (users.moveToFirst(); !users.isAfterLast(); users.moveToNext()) {
                User u = new User(users);
                emailsToUsers.put(u.getValue(User.EMAIL), u);
            }
        } finally {
            users.close();
        }
    }

    private void setupUi() {
        TextView dialogView = (TextView) findViewById(R.id.reminder_message);
        StringBuilder builder = new StringBuilder(getString(R.string.CRA_created_list_dialog, tagName));
        String attendeesString = buildAttendeesString();
        int color = ThemeService.getThemeColor();

        String title;
        if (!TextUtils.isEmpty(attendeesString)) {
            builder.append(" ") //$NON-NLS-1$
            .append(attendeesString)
            .append(" ") //$NON-NLS-1$
            .append(getString(R.string.CRA_invitation_prompt));
            inviteAll.setBackgroundColor(getResources().getColor(color));
            title = getString(R.string.CRA_share_list_title);
        } else {
            title = getString(R.string.CRA_list_created_title);
            moreOptions.setBackgroundColor(getResources().getColor(color));
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
                Intent editPreferences = new Intent(CalendarAlarmListCreator.this, EditPreferences.class);
                startActivity(editPreferences);
                dismissListener.onClick(v);
            }
        });

        inviteAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set members json and save
                if (!actFmPreferenceService.isLoggedIn()) {
                    moreOptions.performClick();
                    return;
                } else {
                    TagData tagData = new TagData();
                    tagData.setValue(TagData.NAME, tagName);
                    tagData.setValue(TagData.MEMBER_COUNT, emails.size());
                    tagDataService.save(tagData);
                    for (String email : emails) {
                        tagMetadataDao.createMemberLink(tagData.getId(), tagData.getUuid(), email, false);
                    }
                    dismissWithAnimation();
                }
            }
        });

        moreOptions.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarAlarmListCreator.this, TaskListActivity.class);
                intent.putExtra(TaskListActivity.TOKEN_CREATE_NEW_LIST_NAME, tagName);
                intent.putExtra(TaskListActivity.TOKEN_CREATE_NEW_LIST_MEMBERS, buildMembersArray().toString());
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
            if (extras > 0)
                return getString(R.string.CRA_many_attendees, displayName1, displayName2, extras);
            else
                return getString(R.string.CRA_two_attendees, displayName1, displayName2);

        }
    }

    private JSONArray buildMembersArray() {
        JSONArray array = new JSONArray();
        for (String email : emails) {
            JSONObject member = new JSONObject();
            try {
                member.put("email", email); //$NON-NLS-1$
                array.put(member);
            } catch (JSONException e) {
                Log.e(CalendarAlarmScheduler.TAG, "Error creating json member " + email, e); //$NON-NLS-1$
            }
        }
        return array;
    }

    private String getDisplayName(int index) {
        String name = names.get(index);
        if (!TextUtils.isEmpty(name))
            return name;
        String email = emails.get(index);
        if (emailsToUsers.containsKey(email)) {
            User u = emailsToUsers.get(email);
            String userName = u.getDisplayName();
            if (!TextUtils.isEmpty(userName))
                return userName;
        }
        return email;
    }
}
