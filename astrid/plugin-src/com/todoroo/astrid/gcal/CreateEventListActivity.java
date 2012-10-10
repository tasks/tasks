package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Constants;

public class CreateEventListActivity extends Activity {

    public static final String TOKEN_LIST_NAME = "listName"; //$NON-NLS-1$

    @Autowired
    private UserDao userDao;

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    private ArrayList<String> names;
    private ArrayList<String> emails;
    private HashMap<String, User> emailsToUsers;

    private boolean loggedIn;

    private Button saveButton;
    private Button cancelButton;
    private ListView membersList;
    private EditText tagName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DependencyInjectionService.getInstance().inject(this);
        setContentView(R.layout.create_event_list_activity);

        saveButton = (Button) findViewById(R.id.save);
        cancelButton = (Button) findViewById(R.id.cancel);
        tagName = (EditText) findViewById(R.id.list_name);
        membersList = (ListView) findViewById(R.id.members_list);

        Intent intent = getIntent();
        names = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_NAMES);
        emails = intent.getStringArrayListExtra(CalendarReminderActivity.TOKEN_EMAILS);
        tagName.setText(intent.getStringExtra(TOKEN_LIST_NAME));

        loggedIn = actFmPreferenceService.isLoggedIn();

        initializeUserMap();

        initializeListView();

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
        String name = tagName.getText().toString();
        TagData checkName = tagDataService.getTag(name, TagData.PROPERTIES);
        if (checkName != null) {
            DialogUtilities.okDialog(this, getString(R.string.CRA_list_exists_title), null);
            return;
        }

        TagData newTag = new TagData();
        newTag.setValue(TagData.NAME, name);

        if (loggedIn) { // include members
            JSONArray membersArray = getMembersArray();
            if (Constants.DEBUG)
                Log.w(CalendarAlarmScheduler.TAG, "Creating tag with members: " + membersArray.toString()); //$NON-NLS-1$

            newTag.setValue(TagData.MEMBERS, membersArray.toString());
            newTag.setValue(TagData.MEMBER_COUNT, membersArray.length());
        }

        tagDataService.save(newTag);

        FilterWithCustomIntent filter = TagFilterExposer.filterFromTagData(this, newTag);

        Intent listIntent = new Intent(this, TaskListActivity.class);
        listIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        listIntent.putExtras(filter.customExtras);

        startActivity(listIntent);
        finish();
    }

    private void initializeListView() {
        if (loggedIn) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, android.R.id.text1);
            membersList.setAdapter(adapter);
            membersList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            emails.remove(ActFmPreferenceService.thisUser().optString("email", null)); //$NON-NLS-1$
            for (int i = 0; i < emails.size(); i++) {
                String email = emails.get(i);
                adapter.add(email);
                membersList.setItemChecked(i, true);
            }

            if (adapter.getCount() == 0)
                membersList.setVisibility(View.GONE);
        } else {
            membersList.setVisibility(View.GONE);
        }
    }

    private JSONArray getMembersArray() {
        JSONArray array = new JSONArray();
        for (int i = 0; i < membersList.getCount(); i++) {
            if (membersList.isItemChecked(i)) {
                try {
                    JSONObject member = new JSONObject();
                    member.put("email", (String) membersList.getItemAtPosition(i)); //$NON-NLS-1$
                    array.put(member);
                } catch (JSONException e) {
                    // ignored
                }
            }
        }
        return array;
    }

}
