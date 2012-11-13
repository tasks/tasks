package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class CalendarAlarmReceiver extends BroadcastReceiver {

    public static final int REQUEST_CODE_CAL_REMINDER = 100;
    public static final String BROADCAST_CALENDAR_REMINDER = Constants.PACKAGE + ".CALENDAR_EVENT";

    private static final String[] EVENTS_PROJECTION = {
        Calendars.EVENTS_DTSTART_COL,
        Calendars.EVENTS_DTEND_COL,
        Calendars.EVENTS_NAME_COL,
    };

    private static final String[] ATTENDEES_PROJECTION = {
        Calendars.ATTENDEES_NAME_COL,
        Calendars.ATTENDEES_EMAIL_COL,
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Preferences.getBoolean(R.string.p_calendar_reminders, true))
            return;
        try {
            Uri data = intent.getData();
            if (data == null)
                return;

            String uriString = data.toString();
            int pathIndex = uriString.indexOf("://");
            if (pathIndex > 0)
                pathIndex += 3;
            else return;
            long eventId = Long.parseLong(uriString.substring(pathIndex));
            boolean fromPostpone = CalendarAlarmScheduler.URI_PREFIX_POSTPONE.equals(data.getScheme());
            if (eventId > 0) {
                showCalReminder(context, eventId, fromPostpone);
            }
        } catch (IllegalArgumentException e) { // Some cursor read failed, or badly formed uri
            e.printStackTrace();
        }
    }

    private void showCalReminder(Context context,
            long eventId, boolean fromPostpone) {
        ContentResolver cr = context.getContentResolver();
        Uri eventUri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);

        if (AndroidUtilities.getSdkVersion() <= 7)
            return;

        String[] eventArg = new String[] { Long.toString(eventId) };
        Cursor event = cr.query(eventUri,
                EVENTS_PROJECTION,
                Calendars.ID_COLUMN_NAME + " = ?",
                eventArg,
                null);
        try {
            if (event.moveToFirst()) {
                int dtstartIndex = event.getColumnIndexOrThrow(Calendars.EVENTS_DTSTART_COL);
                int dtendIndex = event.getColumnIndexOrThrow(Calendars.EVENTS_DTEND_COL);
                int titleIndex = event.getColumnIndexOrThrow(Calendars.EVENTS_NAME_COL);

                String title = event.getString(titleIndex);
                long startTime = event.getLong(dtstartIndex);
                long endTime = event.getLong(dtendIndex);

                boolean shouldShowReminder;
                if (fromPostpone) {
                    long timeAfter = DateUtilities.now() - endTime;
                    shouldShowReminder = (timeAfter > DateUtilities.ONE_MINUTE * 2);
                } else {
                    long timeUntil = startTime - DateUtilities.now();
                    shouldShowReminder = (timeUntil > 0 && timeUntil < DateUtilities.ONE_MINUTE * 20);
                }

                if (shouldShowReminder) {
                    // Get attendees
                    Cursor attendees = cr.query(Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_ATTENDEES),
                            ATTENDEES_PROJECTION,
                            Calendars.ATTENDEES_EVENT_ID_COL + " = ? ",
                            eventArg,
                            null);
                    try {
                        // Do something with attendees
                        int emailIndex = attendees.getColumnIndexOrThrow(Calendars.ATTENDEES_EMAIL_COL);
                        int nameIndex = attendees.getColumnIndexOrThrow(Calendars.ATTENDEES_NAME_COL);

                        ArrayList<String> names = new ArrayList<String>();
                        ArrayList<String> emails = new ArrayList<String>();

                        Account[] accountArray = AccountManager.get(context).getAccounts();
                        Set<String> phoneAccounts = new HashSet<String>();
                        for (Account a : accountArray) {
                            phoneAccounts.add(a.name);
                        }

                        String astridUser = ActFmPreferenceService.thisUser().optString("email");
                        if (!TextUtils.isEmpty(astridUser))
                            phoneAccounts.add(astridUser);

                        boolean includesMe = false;
                        for (attendees.moveToFirst(); !attendees.isAfterLast(); attendees.moveToNext()) {
                            String name = attendees.getString(nameIndex);
                            String email = attendees.getString(emailIndex);
                            if (!TextUtils.isEmpty(email)) {
                                if (phoneAccounts.contains(email)) {
                                    includesMe = true;
                                    continue;
                                }
                                if (Constants.DEBUG)
                                    Log.w(CalendarAlarmScheduler.TAG, "Attendee: " + name + ", email: " + email);
                                names.add(name);
                                emails.add(email);
                            }
                        }

                        if (emails.size() > 0 && includesMe) {
                            Intent reminderActivity = new Intent(context, CalendarReminderActivity.class);
                            reminderActivity.putStringArrayListExtra(CalendarReminderActivity.TOKEN_NAMES, names);
                            reminderActivity.putStringArrayListExtra(CalendarReminderActivity.TOKEN_EMAILS, emails);
                            reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_ID, eventId);
                            reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_NAME, title);
                            reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_START_TIME, startTime);
                            reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_END_TIME, endTime);
                            reminderActivity.putExtra(CalendarReminderActivity.TOKEN_FROM_POSTPONE, fromPostpone);
                            reminderActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            context.startActivity(reminderActivity);
                        }
                    } finally {
                        attendees.close();
                    }
                }
            }
        } finally {
            event.close();
        }
    }

}
