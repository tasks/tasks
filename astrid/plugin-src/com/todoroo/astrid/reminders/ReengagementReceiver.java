/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;

public class ReengagementReceiver extends BroadcastReceiver {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired TaskService taskService;

    private static final int TASK_LIMIT = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Preferences.getBoolean(R.string.p_rmd_enabled, true))
            return;

        DependencyInjectionService.getInstance().inject(this);

        int reengagementReminders = Preferences.getInt(ReengagementService.PREF_REENGAGEMENT_COUNT, 1);
        Preferences.setInt(ReengagementService.PREF_REENGAGEMENT_COUNT, reengagementReminders + 1);

        Intent notifIntent = new Intent(context, TaskListActivity.class);

        QueryTemplate template = new QueryTemplate().where(TaskCriteria.activeVisibleMine());
        String sql = SortHelper.adjustQueryForFlagsAndSort(template.toString(), 0, SortHelper.SORT_AUTO) + " LIMIT " + TASK_LIMIT; //$NON-NLS-1$

        boolean hasTasks = false;
        TodorooCursor<Task> tasks = taskService.query(Query.select(Task.ID).where(TaskCriteria.activeVisibleMine()).limit(TASK_LIMIT));
        try {
            hasTasks = tasks.getCount() > 0;
        } finally {
            tasks.close();
        }

        String title = Notifications.getRandomReminder(context.getResources().getStringArray(R.array.rmd_reengage_notif_titles));
        if (title.contains("%s")) { //$NON-NLS-1$
            String name = ""; //$NON-NLS-1$
            if (actFmPreferenceService.isLoggedIn()) {
                JSONObject thisUser = ActFmPreferenceService.thisUser();
                name = thisUser.optString("first_name"); //$NON-NLS-1$
                if (TextUtils.isEmpty(name))
                    name = thisUser.optString("name"); //$NON-NLS-1$
                if (TextUtils.isEmpty(name))
                    name = context.getString(R.string.rmd_reengage_name_default);
            }
            title = String.format(title, name);
        }

        String text = Notifications.getRandomReminder(context.getResources().getStringArray(hasTasks ? R.array.rmd_reengage_dialog_options : R.array.rmd_reengage_dialog_empty_options));

        FilterWithCustomIntent filter = new FilterWithCustomIntent(context.getString(R.string.rmd_NoA_filter),
                context.getString(R.string.rmd_NoA_filter),
                sql,
                null);
        filter.customTaskList = new ComponentName(context, ReengagementFragment.class);
        filter.customExtras = new Bundle();
        filter.customExtras.putString(ReengagementFragment.EXTRA_TEXT, text);

        notifIntent.setAction("NOTIFY_reengagement"); //$NON-NLS-1$
        notifIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        notifIntent.putExtra(ReengagementFragment.EXTRA_TEXT, text);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        notifIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_REENGAGEMENT);


        NotificationManager manager = new AndroidNotificationManager(context);
        Notification notification = new Notification(R.drawable.notif_astrid,
                text, System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(context,
                title,
                text,
                pendingIntent);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if(Preferences.getBoolean(R.string.p_rmd_persistent, true)) {
            notification.flags |= Notification.FLAG_NO_CLEAR |
                Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        } else {
            notification.defaults = Notification.DEFAULT_LIGHTS;
        }

        manager.notify(0, notification);
        Flags.set(Flags.REFRESH); // Forces a reload when app launches

        ReengagementService.scheduleReengagementAlarm(context);
    }

}
