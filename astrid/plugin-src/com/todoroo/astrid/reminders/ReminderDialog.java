/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagMemberMetadata;
import com.todoroo.astrid.tags.TagService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A dialog that shows your task reminder
 *
 * @author sbosley
 */
public class ReminderDialog extends Dialog {

    private static final int MAX_FACES = 4;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserDao userDao;

    public ReminderDialog(final AstridActivity activity, final long taskId,
                          String title) {
        super(activity, R.style.ReminderDialog);
        DependencyInjectionService.getInstance().inject(this);
        final SnoozeCallback dialogSnooze = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                Task task = new Task();
                task.setId(taskId);
                task.setValue(Task.REMINDER_SNOOZE, time);
                PluginServices.getTaskService().save(task);
                dismiss();
            }
        };
        final OnTimeSetListener onTimeSet = new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                Date alarmTime = new Date();
                alarmTime.setHours(hours);
                alarmTime.setMinutes(minutes);
                if (alarmTime.getTime() < DateUtilities.now()) {
                    alarmTime.setDate(alarmTime.getDate() + 1);
                }
                dialogSnooze.snoozeForTime(alarmTime.getTime());
            }
        };

        if (Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            setContentView(R.layout.astrid_reminder_view);
            setupSpeechBubble(activity);
        } else {
            setContentView(R.layout.astrid_reminder_view_portrait);
            title = activity.getString(R.string.rmd_NoA_dlg_title) + " " + title; //$NON-NLS-1$
            removeSpeechBubble();
        }


        // set up listeners
        findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dismiss();
            }
        });

        findViewById(R.id.reminder_snooze).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotificationFragment.snooze(activity, onTimeSet, dialogSnooze);
            }
        });

        findViewById(R.id.reminder_complete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Task task = taskService.fetchById(taskId, Task.ID, Task.REMINDER_LAST, Task.SOCIAL_REMINDER);
                if (task != null) {
                    taskService.setComplete(task, true);
                }
                activity.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                Toast.makeText(activity,
                        R.string.rmd_NoA_completed_toast,
                        Toast.LENGTH_LONG).show();
                dismiss();
            }
        });

        findViewById(R.id.reminder_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                activity.onTaskListItemClicked(taskId);
            }
        });

        ((TextView) findViewById(R.id.reminder_title)).setText(title);

        setOwnerActivity(activity);
    }


    private void removeSpeechBubble() {
        LinearLayout container = (LinearLayout) findViewById(R.id.speech_bubble_container);
        container.setVisibility(View.GONE);
    }

    private void setupSpeechBubble(Activity activity) {
        ((TextView) findViewById(R.id.reminder_message)).setText(
                Notifications.getRandomReminder(activity.getResources().getStringArray(R.array.reminder_responses)));
    }

    private void addPicturesFromJSONArray(JSONArray array, LinkedHashSet<String> pictureUrls, List<String> names, AtomicBoolean isSharedTask) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject person = array.getJSONObject(i);
            if (person.has("picture")) { //$NON-NLS-1$
                if (ActFmPreferenceService.userId().equals(Long.toString(person.optLong("id")))) //$NON-NLS-1$
                {
                    continue;
                }
                isSharedTask.set(true);
                String pictureUrl = person.getString("picture"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(pictureUrl)) {
                    pictureUrls.add(pictureUrl);
                }

                String name = person.optString("first_name"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(name)) {
                    names.add(name);
                } else {
                    name = person.optString("name"); //$NON-NLS-1$
                    if (!TextUtils.isEmpty(name)) {
                        names.add(name);
                    }
                }


            }
        }
    }

    private void addTagFaces(long taskId, LinkedHashSet<String> pictureUrls, List<String> names, AtomicBoolean isSharedTask) {
        TodorooCursor<TagData> tags = tagService.getTagDataForTask(taskId, Criterion.all, TagData.UUID, TagData.MEMBERS);
        try {
            TagData td = new TagData();
            for (tags.moveToFirst(); !tags.isAfterLast() && pictureUrls.size() < MAX_FACES; tags.moveToNext()) {
                td.readFromCursor(tags);
                try {
                    JSONArray people = new JSONArray(td.getValue(TagData.MEMBERS));
                    addPicturesFromJSONArray(people, pictureUrls, names, isSharedTask);
                } catch (JSONException e) {
                    JSONArray people = new JSONArray();
                    TodorooCursor<User> users = userDao.query(Query.select(User.PROPERTIES)
                            .where(User.UUID.in(
                                    Query.select(TagMemberMetadata.USER_UUID)
                                            .from(TagMetadata.TABLE)
                                            .where(TagMetadata.TAG_UUID.eq(td.getUuid())))));
                    try {
                        User user = new User();
                        for (users.moveToFirst(); !users.isAfterLast(); users.moveToNext()) {
                            user.clear();
                            user.readFromCursor(users);
                            try {
                                JSONObject userJson = new JSONObject();
                                ActFmSyncService.JsonHelper.jsonFromUser(userJson, user);
                                people.put(userJson);
                            } catch (JSONException e2) {
                                //
                            }
                        }
                        try {
                            addPicturesFromJSONArray(people, pictureUrls, names, isSharedTask);
                        } catch (JSONException e2) {
                            //
                        }
                    } finally {
                        users.close();
                    }
                }
            }
        } finally {
            tags.close();
        }
    }
}
