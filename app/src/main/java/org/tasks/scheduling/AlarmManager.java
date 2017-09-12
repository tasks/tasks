package org.tasks.scheduling;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;

public class AlarmManager {

    private final android.app.AlarmManager alarmManager;

    @Inject
    public AlarmManager(@ForApplication Context context) {
        alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void cancel(PendingIntent pendingIntent) {
        alarmManager.cancel(pendingIntent);
    }

    @SuppressLint("NewApi")
    public void wakeup(long time, PendingIntent pendingIntent) {
        if (atLeastMarshmallow()) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else if (atLeastKitKat()) {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    @SuppressLint("NewApi")
    public void noWakeup(long time, PendingIntent pendingIntent) {
        if (atLeastKitKat()) {
            alarmManager.setExact(android.app.AlarmManager.RTC, time, pendingIntent);
        } else {
            alarmManager.set(android.app.AlarmManager.RTC, time, pendingIntent);
        }
    }
}
