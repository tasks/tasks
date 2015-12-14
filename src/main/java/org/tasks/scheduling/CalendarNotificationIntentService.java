package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gcal.CalendarAlarmReceiver;
import com.todoroo.astrid.gcal.Calendars;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class CalendarNotificationIntentService extends RecurringIntervalIntentService {

    public static final String URI_PREFIX = "cal-reminder";
    public static final String URI_PREFIX_POSTPONE = "cal-postpone";

    @Inject Preferences preferences;
    @Inject PermissionChecker permissionChecker;
    @Inject @ForApplication Context context;
    @Inject AlarmManager alarmManager;

    public CalendarNotificationIntentService() {
        super(CalendarNotificationIntentService.class.getSimpleName());
    }

    @Override
    void run() {
        ContentResolver cr = context.getContentResolver();

        long now = DateUtilities.now();

        Cursor events = cr.query(Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS),
                new String[] { Calendars.ID_COLUMN_NAME, Calendars.EVENTS_DTSTART_COL },
                Calendars.EVENTS_DTSTART_COL + " > ? AND " + Calendars.EVENTS_DTSTART_COL + " < ?",
                new String[] { Long.toString(now + DateUtilities.ONE_MINUTE * 15), Long.toString(now + DateUtilities.ONE_DAY) },
                null);
        try {
            if (events != null && events.getCount() > 0) {
                int idIndex = events.getColumnIndex(Calendars.ID_COLUMN_NAME);
                int dtstartIndex = events.getColumnIndexOrThrow(Calendars.EVENTS_DTSTART_COL);

                for (events.moveToFirst(); !events.isAfterLast(); events.moveToNext()) {
                    Intent eventAlarm = new Intent(context, CalendarAlarmReceiver.class);
                    eventAlarm.setAction(CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER);

                    long start = events.getLong(dtstartIndex);
                    long id = events.getLong(idIndex);

                    eventAlarm.setData(Uri.parse(URI_PREFIX + "://" + id));

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                            CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER, eventAlarm, 0);

                    alarmManager.cancel(pendingIntent);

                    long alarmTime = start - DateUtilities.ONE_MINUTE * 15;
                    alarmManager.wakeup(alarmTime, pendingIntent);
                }
            }
        } finally {
            if (events != null) {
                events.close();
            }
        }
    }

    @Override
    long intervalMillis() {
        return preferences.getBoolean(R.string.p_calendar_reminders, false) && permissionChecker.canAccessCalendars()
                ? TimeUnit.HOURS.toMillis(12)
                : 0;
    }

    @Override
    String getLastRunPreference() {
        return null;
    }
}
