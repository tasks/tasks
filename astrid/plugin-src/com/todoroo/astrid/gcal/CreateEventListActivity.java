package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.User;

public class CreateEventListActivity extends Activity {

    public static final String TOKEN_LIST_NAME = "listName"; //$NON-NLS-1$

    @Autowired
    private UserDao userDao;

    private ArrayList<String> names;
    private ArrayList<String> emails;
    private HashMap<String, User> emailsToUsers;

    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);
        setContentView(R.layout.create_event_list_activity);

        saveButton = (Button) findViewById(R.id.save);
        cancelButton = (Button) findViewById(R.id.cancel);

        Intent intent = getIntent();
        names = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_NAMES);
        emails = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_EMAILS);

        initializeUserMap();

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

    private void addListeners() {
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    private void save() {
        // Perform save
    }

    //    private void maybeDoStuff() {
    //      LinearLayout root = (LinearLayout) findViewById(R.id.reminder_root);
    //
    //      for (int i = 0; i < emails.size(); i++) {
    //          String email = emails.get(i);
    //          if (email.equals(ActFmPreferenceService.thisUser().optString("email", null)))
    //              continue;
    //          String displayString = email;
    //          if (!TextUtils.isEmpty(names.get(i))) {
    //              displayString = names.get(i);
    //          } else if (emailsToUsers.containsKey(email)) {
    //              User u = emailsToUsers.get(email);
    //              displayString = u.getDisplayName();
    //          }
    //
    //          TextView tv = new TextView(this);
    //          tv.setText(displayString);
    //          tv.setTextColor(getResources().getColor(android.R.color.white));
    //          root.addView(tv);
    //      }
    //    }

}
