package com.todoroo.astrid.gcal;

import android.content.Context;
import android.content.Intent;

import org.tasks.BuildConfig;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class CalendarStartupReceiver extends InjectingBroadcastReceiver {

    public static final String BROADCAST_RESCHEDULE_CAL_ALARMS = BuildConfig.APPLICATION_ID + ".SCHEDULE_CAL_REMINDERS"; //$NON-NLS-1$

    @Inject CalendarAlarmScheduler calendarAlarmScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        calendarAlarmScheduler.scheduleCalendarAlarms(context, false);
    }
}
