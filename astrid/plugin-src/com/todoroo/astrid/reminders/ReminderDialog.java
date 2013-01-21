/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.ResourceDrawableCache;

/**
 * A dialog that shows your task reminder
 *
 * @author sbosley
 *
 */
public class ReminderDialog extends Dialog {

    private static final int MAX_FACES = 4;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TagService tagService;

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
                StatisticsService.reportEvent(StatisticsConstants.TASK_SNOOZE);
            }
        };
        final OnTimeSetListener onTimeSet = new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                Date alarmTime = new Date();
                alarmTime.setHours(hours);
                alarmTime.setMinutes(minutes);
                if(alarmTime.getTime() < DateUtilities.now())
                    alarmTime.setDate(alarmTime.getDate() + 1);
                dialogSnooze.snoozeForTime(alarmTime.getTime());
            }
        };

        if (Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            setContentView(R.layout.astrid_reminder_view);
            setupSpeechBubble(activity, taskId);
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
                if (task != null)
                    taskService.setComplete(task, true);
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

    private void setupSpeechBubble(Activity activity, long taskId) {
        ((TextView) findViewById(R.id.reminder_message)).setText(
                Notifications.getRandomReminder(activity.getResources().getStringArray(R.array.reminder_responses)));

        if (Preferences.getBoolean(R.string.p_rmd_social, true)) {
            Task task = taskService.fetchById(taskId, Task.ID, Task.SHARED_WITH);
            addFacesToReminder(activity, task);
        }
    }

    private void addFacesToReminder(Activity activity, Task task) {
        if (task == null)
            return;
        Resources resources = activity.getResources();
        LinkedHashSet<String> pictureUrls = new LinkedHashSet<String>();
        AtomicBoolean isSharedTask = new AtomicBoolean(false);

        addSharedWithFaces(task, pictureUrls, isSharedTask);

        if (pictureUrls.size() < MAX_FACES) {
            addTagFaces(task.getId(), pictureUrls, isSharedTask);
        }

        if (pictureUrls.size() > 0) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            LinearLayout layout = new LinearLayout(activity);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            containerParams.setMargins((int) (-7 * metrics.density), 0, 0, 0);
            layout.setLayoutParams(containerParams);

            int padding = (int) (8 * metrics.density);

            int count = 0;
            for (String url : pictureUrls) {
                AsyncImageView image = new AsyncImageView(activity);
                image.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_default_person_image));
                image.setUrl(url);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (35 * metrics.density), (int) (35 * metrics.density));
                lp.setMargins(padding, padding, 0, padding);
                image.setLayoutParams(lp);
                layout.addView(image);
                if (++count >= MAX_FACES)
                    break;
            }

            LinearLayout container = (LinearLayout) findViewById(R.id.speech_bubble_content);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(layout, 0);

            ((TextView) findViewById(R.id.reminder_message)).setText(
                    Notifications.getRandomReminder(activity.getResources().getStringArray(R.array.reminders_social)));

            task.setValue(Task.SOCIAL_REMINDER, Task.REMINDER_SOCIAL_FACES);
        } else {
            if (isSharedTask.get())
                task.setValue(Task.SOCIAL_REMINDER, Task.REMINDER_SOCIAL_NO_FACES);
            else
                task.setValue(Task.SOCIAL_REMINDER, Task.REMINDER_SOCIAL_PRIVATE);
        }
    }

    private void addPicturesFromJSONArray(JSONArray array, LinkedHashSet<String> pictureUrls, AtomicBoolean isSharedTask) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject person = array.getJSONObject(i);
            if (person.has("picture")) { //$NON-NLS-1$
                if (person.optLong("id") == ActFmPreferenceService.userId()) //$NON-NLS-1$
                    continue;
                isSharedTask.set(true);
                String pictureUrl = person.getString("picture"); //$NON-NLS-1$
                if (!TextUtils.isEmpty(pictureUrl)) {
                    pictureUrls.add(pictureUrl);
                }
            }
        }
    }

    private void addSharedWithFaces(Task t, LinkedHashSet<String> pictureUrls, AtomicBoolean isSharedTask) {
        try {
            JSONObject sharedWith = new JSONObject(t.getValue(Task.SHARED_WITH));
            if (sharedWith.has("p")) { //$NON-NLS-1$
                JSONArray people = sharedWith.getJSONArray("p"); //$NON-NLS-1$
                addPicturesFromJSONArray(people, pictureUrls, isSharedTask);
            }
        } catch (JSONException e) {
            //
        }
    }

    private void addTagFaces(long taskId, LinkedHashSet<String> pictureUrls, AtomicBoolean isSharedTask) {
        TodorooCursor<TagData> tags = tagService.getTagDataForTask(taskId, TagData.MEMBER_COUNT.gt(0), TagData.MEMBERS);
        try {
            TagData td = new TagData();
            for (tags.moveToFirst(); !tags.isAfterLast() && pictureUrls.size() < MAX_FACES; tags.moveToNext()) {
                td.readFromCursor(tags);
                try {
                    JSONArray people = new JSONArray(td.getValue(TagData.MEMBERS));
                    addPicturesFromJSONArray(people, pictureUrls, isSharedTask);
                } catch (JSONException e) {
                    //
                }
            }
        } finally {
            tags.close();
        }
    }
}
