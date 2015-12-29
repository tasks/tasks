package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Context;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;

public class AlarmManager {

    private final android.app.AlarmManager alarmManager;
    private final Preferences preferences;

    @Inject
    public AlarmManager(@ForApplication Context context, Preferences preferences) {
        this.preferences = preferences;
        alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void cancel(PendingIntent pendingIntent) {
        alarmManager.cancel(pendingIntent);
    }

    public void wakeupAdjustingForQuietHours(long time, PendingIntent pendingIntent) {
        wakeup(adjustForQuietHours(time), pendingIntent);
    }

    public void wakeup(long time, PendingIntent pendingIntent) {
        if (preferences.isDozeNotificationEnabled()) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else if (atLeastKitKat()) {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    public void noWakeup(long time, PendingIntent pendingIntent) {
        if (preferences.isDozeNotificationEnabled()) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC, time, pendingIntent);
        } else if (atLeastKitKat()) {
            alarmManager.setExact(android.app.AlarmManager.RTC, time, pendingIntent);
        } else {
            alarmManager.set(android.app.AlarmManager.RTC, time, pendingIntent);
        }
    }

    public void setInexactRepeating(long interval, PendingIntent pendingIntent) {
        alarmManager.setInexactRepeating(android.app.AlarmManager.RTC, 0, interval, pendingIntent);
    }

    long adjustForQuietHours(long time) {
        if (preferences.quietHoursEnabled()) {
            DateTime dateTime = new DateTime(time);
            DateTime start = dateTime.withMillisOfDay(preferences.getInt(R.string.p_rmd_quietStart));
            DateTime end = dateTime.withMillisOfDay(preferences.getInt(R.string.p_rmd_quietEnd));
            if (start.isAfter(end)) {
                if (dateTime.isBefore(end)) {
                    return end.getMillis();
                } else if (dateTime.isAfter(start)) {
                    return end.plusDays(1).getMillis();
                }
            } else {
                if (dateTime.isAfter(start) && dateTime.isBefore(end)) {
                    return end.getMillis();
                }
            }
        }
        return time;
    }
}
