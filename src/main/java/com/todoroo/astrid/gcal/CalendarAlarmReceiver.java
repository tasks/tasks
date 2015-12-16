package com.todoroo.astrid.gcal;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.CalendarNotificationIntentService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

public class CalendarAlarmReceiver extends InjectingBroadcastReceiver {

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

    @Inject Preferences preferences;
    @Inject PermissionChecker permissionChecker;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!preferences.getBoolean(R.string.p_calendar_reminders, true)) {
            return;
        }

        if (!permissionChecker.canAccessCalendars()) {
            return;
        }

        try {
            Uri data = intent.getData();
            if (data == null) {
                return;
            }

            String uriString = data.toString();
            int pathIndex = uriString.indexOf("://");
            if (pathIndex > 0) {
                pathIndex += 3;
            } else {
                return;
            }
            long eventId = Long.parseLong(uriString.substring(pathIndex));
            boolean fromPostpone = CalendarNotificationIntentService.URI_PREFIX_POSTPONE.equals(data.getScheme());
            if (eventId > 0) {
                showCalReminder(context, eventId, fromPostpone);
            }
        } catch (IllegalArgumentException e) {
            // Some cursor read failed, or badly formed uri
            Timber.e(e, e.getMessage());
        }
    }

    private void showCalReminder(Context context, long eventId, boolean fromPostpone) {
        ContentResolver cr = context.getContentResolver();
        Uri eventUri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);

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

                        ArrayList<String> names = new ArrayList<>();
                        ArrayList<String> emails = new ArrayList<>();

                        Account[] accountArray = AccountManager.get(context).getAccounts();
                        Set<String> phoneAccounts = new HashSet<>();
                        for (Account a : accountArray) {
                            phoneAccounts.add(a.name);
                        }

                        boolean includesMe = false;
                        for (attendees.moveToFirst(); !attendees.isAfterLast(); attendees.moveToNext()) {
                            String name = attendees.getString(nameIndex);
                            String email = attendees.getString(emailIndex);
                            if (!TextUtils.isEmpty(email)) {
                                if (phoneAccounts.contains(email)) {
                                    includesMe = true;
                                    continue;
                                }
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
