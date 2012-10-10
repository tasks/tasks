package com.todoroo.astrid.gcal;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;

@SuppressWarnings("nls")
public class CalendarAlarmReciever extends BroadcastReceiver {

    public static final String TOKEN_EVENT_ID = "eventId";

    private static final String ID_COLUMN_NAME = "_id";
    private static final boolean USE_ICS_NAMES = AndroidUtilities.getSdkVersion() >= 14;
    private static final String EVENT_START_COLUMN_NAME = (USE_ICS_NAMES ? CalendarContract.Events.DTSTART : "dtstart");

    private static final String[] EVENTS_PROJECTION = {
        EVENT_START_COLUMN_NAME,
    };

    private static final String ATTENDEES_EVENT_ID_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.EVENT_ID : "event_id");
    private static final String ATTENDEES_NAME_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_NAME : "attendeeName");
    private static final String ATTENDEES_EMAIL_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_EMAIL: "attendeeEmail");

    private static final String[] ATTENDEES_PROJECTION = {
        ATTENDEES_NAME_COL,
        ATTENDEES_EMAIL_COL,
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ContentResolver cr = context.getContentResolver();
            long eventId = intent.getLongExtra(TOKEN_EVENT_ID, -1);
            if (eventId > 0) {
                Uri eventUri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);

                String[] eventArg = new String[] { Long.toString(eventId) };
                Cursor event = cr.query(eventUri,
                        EVENTS_PROJECTION,
                        ID_COLUMN_NAME + " = ?",
                        eventArg,
                        null);
                try {
                    int timeIndex = event.getColumnIndexOrThrow(EVENT_START_COLUMN_NAME);
                    long startTime = event.getLong(timeIndex);
                    long timeUntil = startTime - DateUtilities.now();

                    if (timeUntil > 0 && timeUntil < DateUtilities.ONE_MINUTE * 20) {
                        // Get attendees
                        Cursor attendees = cr.query(Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_ATTENDEES),
                                ATTENDEES_PROJECTION,
                                ATTENDEES_EVENT_ID_COL + " = ? ",
                                eventArg,
                                null);
                        try {
                            // Do something with attendees
                        } finally {
                            attendees.close();
                        }
                    }
                } finally {
                    event.close();
                }
            }
        } catch (IllegalArgumentException e) { // Some cursor read failed
            e.printStackTrace();
        }
    }

}
