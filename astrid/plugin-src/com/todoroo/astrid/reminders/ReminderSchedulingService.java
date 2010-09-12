package com.todoroo.astrid.reminders;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.alarms.AlarmService;

/**
 * Schedules reminders in the background to prevent ANR's
 *
 * @author Tim Su
 *
 */
public class ReminderSchedulingService extends Service {

    /** Receive the alarm - start the synchronize service! */
    @SuppressWarnings("nls")
    @Override
    public void onStart(Intent intent, int startId) {
        ContextManager.setContext(this);
        try {
            ReminderService.getInstance().scheduleAllAlarms();
            AlarmService.getInstance().scheduleAllAlarms();
        } catch (Exception e) {
            Log.e("reminder-scheduling", "reminder-startup", e);
        }

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
