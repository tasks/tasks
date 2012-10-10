package com.todoroo.astrid.gcal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.todoroo.andlib.utility.DateUtilities;

@SuppressWarnings("nls")
public class CalendarAlarmScheduler {

    public static void scheduleCalendarAlarms(Context context) {
        ContentResolver cr = context.getContentResolver();

        long now = DateUtilities.now();

        Cursor events = cr.query(Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS),
                new String[] { Calendars.ID_COLUMN_NAME, Calendars.EVENTS_DTSTART_COL },
                Calendars.EVENTS_DTSTART_COL + " > ? AND " + Calendars.EVENTS_DTSTART_COL + " < ?",
                new String[] { Long.toString(now + DateUtilities.ONE_MINUTE * 20), Long.toString(now + DateUtilities.ONE_DAY) },
                null);
        try {
            if (events.moveToFirst()) {
                int idIndex = events.getColumnIndex(Calendars.ID_COLUMN_NAME);
                int timeIndex = events.getColumnIndexOrThrow(Calendars.EVENTS_DTSTART_COL);

                long start = events.getLong(timeIndex);
                long id = events.getLong(idIndex);

                long alarmTime = start - DateUtilities.ONE_MINUTE * 15;
                Intent eventAlarm = new Intent(context, CalendarAlarmReceiver.class);
                eventAlarm.setAction(CalendarAlarmReceiver.BROADCAST_CALENDAR_REMINDER);
                eventAlarm.putExtra(CalendarAlarmReceiver.TOKEN_EVENT_ID, id);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        CalendarAlarmReceiver.REQUEST_CODE_CAL_REMINDER, eventAlarm, 0);

                AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                am.cancel(pendingIntent);

                am.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            }
        } finally {
            events.close();
        }

    }

}
