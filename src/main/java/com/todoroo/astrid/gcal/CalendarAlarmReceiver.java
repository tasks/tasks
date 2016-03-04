package com.todoroo.astrid.gcal;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.calendars.AndroidCalendarEvent;
import org.tasks.calendars.CalendarEventProvider;
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

    private static final String[] ATTENDEES_PROJECTION = {
            CalendarContract.Attendees.ATTENDEE_NAME,
            CalendarContract.Attendees.ATTENDEE_EMAIL,
    };

    @Inject Preferences preferences;
    @Inject PermissionChecker permissionChecker;
    @Inject CalendarEventProvider calendarEventProvider;

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
        AndroidCalendarEvent event = calendarEventProvider.getEvent(eventId);
        if (event == null) {
            return;
        }

        ContentResolver cr = context.getContentResolver();
        String[] eventArg = new String[]{Long.toString(eventId)};
        boolean shouldShowReminder;
        if (fromPostpone) {
            long timeAfter = DateUtilities.now() - event.getEnd();
            shouldShowReminder = (timeAfter > DateUtilities.ONE_MINUTE * 2);
        } else {
            long timeUntil = event.getStart() - DateUtilities.now();
            shouldShowReminder = (timeUntil > 0 && timeUntil < DateUtilities.ONE_MINUTE * 20);
        }

        if (shouldShowReminder) {
            // Get attendees
            Cursor attendees = cr.query(CalendarContract.Attendees.CONTENT_URI,
                    ATTENDEES_PROJECTION,
                    CalendarContract.Attendees.EVENT_ID + " = ? ",
                    eventArg,
                    null);
            try {
                // Do something with attendees
                int emailIndex = attendees.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_EMAIL);
                int nameIndex = attendees.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_NAME);

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
                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_NAME, event.getTitle());
                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_START_TIME, event.getStart());
                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_END_TIME, event.getEnd());
                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_FROM_POSTPONE, fromPostpone);
                    reminderActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    context.startActivity(reminderActivity);
                }
            } finally {
                attendees.close();
            }
        }
    }
}
