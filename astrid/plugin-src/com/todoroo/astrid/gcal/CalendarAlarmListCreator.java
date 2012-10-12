package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;

public class CalendarAlarmListCreator extends Activity {

    public static final String TOKEN_LIST_ID = "listId"; //$NON-NLS-1$

    @Autowired
    private UserDao userDao;

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    private ArrayList<String> names;
    private ArrayList<String> emails;
    private HashMap<String, User> emailsToUsers;

    private TagData tagData;
    private TextView inviteAll;
    private TextView moreOptions;
    private TextView ignoreButton;
    private View dismissButton;
    private View ignoreSettingsButton;

    private final OnClickListener dismissListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            AndroidUtilities.callOverridePendingTransition(CalendarAlarmListCreator.this, 0, android.R.anim.fade_out);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);
        setContentView(R.layout.calendar_alarm_list_creator);

        Intent intent = getIntent();

        tagData = tagDataService.fetchById(intent.getLongExtra(TOKEN_LIST_ID, -1), TagData.PROPERTIES);
        inviteAll = (TextView) findViewById(R.id.invite_all);
        moreOptions = (TextView) findViewById(R.id.list_settings);
        ignoreButton = (TextView) findViewById(R.id.ignore);
        ignoreSettingsButton = findViewById(R.id.ignore_settings);

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
        dialogView.setText("Stuff here");
    }

    private void addListeners() {
        ignoreButton.setOnClickListener(dismissListener);
        dismissButton.setOnClickListener(dismissListener);

        ignoreSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPreferences = new Intent(CalendarAlarmListCreator.this, EditPreferences.class);
                startActivity(editPreferences);
                finish();
            }
        });
    }
}
