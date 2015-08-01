package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Context;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class AlarmManager {

    private final android.app.AlarmManager alarmManager;

    @Inject
    public AlarmManager(@ForApplication Context context) {
        alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void cancel(PendingIntent pendingIntent) {
        alarmManager.cancel(pendingIntent);
    }

    public void wakeup(long time, PendingIntent pendingIntent) {
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    public void noWakeup(long time, PendingIntent pendingIntent) {
        alarmManager.set(android.app.AlarmManager.RTC, time, pendingIntent);
    }

    public void setInexactRepeating(long interval, PendingIntent pendingIntent) {
        alarmManager.setInexactRepeating(android.app.AlarmManager.RTC, 0, interval, pendingIntent);
    }
}
