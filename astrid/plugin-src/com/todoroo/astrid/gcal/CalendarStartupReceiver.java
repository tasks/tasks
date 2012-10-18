package com.todoroo.astrid.gcal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;

public class CalendarStartupReceiver extends BroadcastReceiver {

    public static final String BROADCAST_RESCHEDULE_CAL_ALARMS = AstridApiConstants.PACKAGE + ".SCHEDULE_CAL_REMINDERS"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        scheduleCalendarAlarms(context);
    }

    public static void scheduleCalendarAlarms(final Context context) {
        if (!Preferences.getBoolean(R.string.p_calendar_reminders, true))
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                CalendarAlarmScheduler.scheduleAllCalendarAlarms(context);
            }
        }).start();
    }

}
