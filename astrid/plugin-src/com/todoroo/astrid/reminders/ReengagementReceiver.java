package com.todoroo.astrid.reminders;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.TaskApiDao.TaskCriteria;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABTests;
import com.todoroo.astrid.utility.Constants;

public class ReengagementReceiver extends BroadcastReceiver {

    private static final int TASK_LIMIT = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ABChooser.readChoiceForTest(ABTests.AB_TEST_REENGAGEMENT_ENABLED) == 0)
            return;

        int reengagementReminders = Preferences.getInt(ReengagementService.PREF_REENGAGEMENT_COUNT, 1);
        Preferences.setInt(ReengagementService.PREF_REENGAGEMENT_COUNT, reengagementReminders + 1);

        Intent notifIntent = new Intent(context, TaskListActivity.class);

        QueryTemplate template = new QueryTemplate().where(TaskCriteria.activeVisibleMine());
        String sql = SortHelper.adjustQueryForFlagsAndSort(template.toString(), 0, SortHelper.SORT_AUTO) + " LIMIT " + TASK_LIMIT; //$NON-NLS-1$

        FilterWithCustomIntent filter = new FilterWithCustomIntent(context.getString(R.string.rmd_NoA_filter),
                context.getString(R.string.rmd_NoA_filter),
                sql,
                null);
        filter.customTaskList = new ComponentName(context, ReengagementFragment.class);

        notifIntent.setAction("NOTIFY_reengagement"); //$NON-NLS-1$
        notifIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        notifIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_REENGAGEMENT);

        String text = context.getString(R.string.rmd_reengage_notif);
        NotificationManager manager = new AndroidNotificationManager(context);
        Notification notification = new Notification(R.drawable.notif_astrid,
                text, System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(context,
                "", //$NON-NLS-1$
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

        ReengagementService.scheduleReengagementAlarm(context);
    }

}
