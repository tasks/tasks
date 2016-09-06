package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;

import org.tasks.R;
import org.tasks.calendars.AndroidCalendarEvent;
import org.tasks.calendars.CalendarEventProvider;
import org.tasks.injection.ForApplication;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class CalendarNotificationIntentService extends RecurringIntervalIntentService {

    private static final long FIFTEEN_MINUTES = TimeUnit.MINUTES.toMillis(15);

    private static final String URI_PREFIX = "cal-reminder";
    public static final String URI_PREFIX_POSTPONE = "cal-postpone";

    @Inject Preferences preferences;
    @Inject CalendarEventProvider calendarEventProvider;
    @Inject @ForApplication Context context;
    @Inject AlarmManager alarmManager;

    public CalendarNotificationIntentService() {
        super(CalendarNotificationIntentService.class.getSimpleName());
    }

    @Override
    void run() {
        long now = DateUtilities.now();
        long end = now + TimeUnit.DAYS.toMillis(1);

        for (final AndroidCalendarEvent event : calendarEventProvider.getEventsBetween(now, end)) {
            Intent eventAlarm = new Intent(context, CalendarAlarmReceiver.class);
            eventAlarm.setAction(CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER);
            eventAlarm.setData(Uri.parse(URI_PREFIX + "://" + event.getId()));

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER, eventAlarm, PendingIntent.FLAG_CANCEL_CURRENT);

            long reminderTime = event.getStart() - FIFTEEN_MINUTES;
            alarmManager.wakeup(reminderTime, pendingIntent);
            Timber.d("Scheduled reminder for %s at %s", event, reminderTime);
        }
    }

    @Override
    long intervalMillis() {
        return preferences.getBoolean(R.string.p_calendar_reminders, false) ? TimeUnit.HOURS.toMillis(12) : 0;
    }

    @Override
    String getLastRunPreference() {
        return null;
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
