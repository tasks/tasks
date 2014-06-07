package com.todoroo.astrid.gcal;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class CalendarStartupReceiver extends InjectingBroadcastReceiver {

    public static final String BROADCAST_RESCHEDULE_CAL_ALARMS = AstridApiConstants.API_PACKAGE + ".SCHEDULE_CAL_REMINDERS"; //$NON-NLS-1$

    @Inject CalendarAlarmScheduler calendarAlarmScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ContextManager.setContext(context);
        calendarAlarmScheduler.scheduleCalendarAlarms(context, false);
    }
}
