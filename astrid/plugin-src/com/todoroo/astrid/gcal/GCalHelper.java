/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import java.util.TimeZone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class GCalHelper {
    /** If task has no estimated time, how early to set a task in calendar (seconds)*/
    private static final long DEFAULT_CAL_TIME = DateUtilities.ONE_HOUR;

    public static final String CALENDAR_ID_COLUMN = "calendar_id"; //$NON-NLS-1$

    public static String getTaskEventUri(Task task) {
        String uri;
        if (!TextUtils.isEmpty(task.getValue(Task.CALENDAR_URI)))
            uri = task.getValue(Task.CALENDAR_URI);
        else {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null)
                return null;
            uri = task.getValue(Task.CALENDAR_URI);
        }

        return uri;
    }

    public static void createTaskEventIfEnabled(Task t) {
        if (!t.hasDueDate())
            return;
        createTaskEventIfEnabled(t, true);
    }

    private static void createTaskEventIfEnabled(Task t, boolean deleteEventIfExists) {
        boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null
            && !Preferences.getStringValue(R.string.gcal_p_default).equals("-1"); //$NON-NLS-1$
        if (gcalCreateEventEnabled) {
            ContentResolver cr = ContextManager.getContext().getContentResolver();
            Uri calendarUri = GCalHelper.createTaskEvent(t, cr, new ContentValues(), deleteEventIfExists);
            if (calendarUri != null)
                t.setValue(Task.CALENDAR_URI, calendarUri.toString());
        }
    }

    public static Uri createTaskEvent(Task task, ContentResolver cr, ContentValues values) {
        return createTaskEvent(task, cr, values, true);
    }

    @SuppressWarnings("nls")
    public static Uri createTaskEvent(Task task, ContentResolver cr, ContentValues values, boolean deleteEventIfExists) {
        String eventuri = getTaskEventUri(task);

        if(!TextUtils.isEmpty(eventuri) && deleteEventIfExists) {
            deleteTaskEvent(task);
        }

        try{
            Uri uri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);
            values.put("title", task.getValue(Task.TITLE));
            values.put("description", task.getValue(Task.NOTES));
            values.put("hasAlarm", 0);
            if (AndroidUtilities.getSdkVersion() < 14) {
                values.put("transparency", 0);
                values.put("visibility", 0);
            }
            boolean valuesContainCalendarId = (values.containsKey(CALENDAR_ID_COLUMN) &&
                    !TextUtils.isEmpty(values.getAsString(CALENDAR_ID_COLUMN)));
            if (!valuesContainCalendarId) {
                String calendarId = Calendars.getDefaultCalendar();
                if (!TextUtils.isEmpty(calendarId)) {
                    values.put("calendar_id", calendarId);
                }
            }

            createStartAndEndDate(task, values);

            Uri eventUri = cr.insert(uri, values);
            cr.notifyChange(eventUri, null);

            return eventUri;

        } catch (Exception e) {
            // won't work on emulator
            Log.v("astrid-gcal",
                    "error-creating-calendar-event", e);
        }

        return null;
    }

    public static void rescheduleRepeatingTask(Task task, ContentResolver cr) {
        String taskUri = getTaskEventUri(task);
        if (TextUtils.isEmpty(taskUri))
            return;

        Uri eventUri = Uri.parse(taskUri);
        String calendarId = getCalendarId(eventUri, cr);
        if (calendarId == null) { // Bail out, no calendar id
            task.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(CALENDAR_ID_COLUMN, calendarId);

        Uri uri = createTaskEvent(task, cr, cv, false);
        task.setValue(Task.CALENDAR_URI, uri.toString());
    }

    private static String getCalendarId(Uri uri, ContentResolver cr) {
        Cursor calendar = cr.query(uri, new String[] { CALENDAR_ID_COLUMN }, null, null, null);
        try {
            calendar.moveToFirst();
            return calendar.getString(0);
        } catch (CursorIndexOutOfBoundsException e) {
            return null;
        } finally  {
            calendar.close();
        }
    }

    @SuppressWarnings("nls")
    public static boolean deleteTaskEvent(Task task) {
        boolean eventDeleted = false;
        String uri;
        if(task.containsNonNullValue(Task.CALENDAR_URI))
            uri = task.getValue(Task.CALENDAR_URI);
        else {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null)
                return false;
            uri = task.getValue(Task.CALENDAR_URI);
        }

        if(!TextUtils.isEmpty(uri)) {
            try {
                Uri calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = ContextManager.getContext().getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                try {
                    boolean alreadydeleted = cursor.getCount() == 0;

                    if (!alreadydeleted) {
                        cr.delete(calendarUri, null, null);
                        eventDeleted = true;
                    }
                } finally {
                    cursor.close();
                }

                task.setValue(Task.CALENDAR_URI,"");
            } catch (Exception e) {
                Log.e("astrid-gcal", "error-deleting-calendar-event", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return eventDeleted;
    }

    @SuppressWarnings("nls")
    static void createStartAndEndDate(Task task, ContentValues values) {
        long dueDate = task.getValue(Task.DUE_DATE);
        long tzCorrectedDueDate = dueDate + TimeZone.getDefault().getOffset(dueDate);
        long tzCorrectedDueDateNow = DateUtilities.now() + TimeZone.getDefault().getOffset(DateUtilities.now());
        // FIXME: doesnt respect timezones, see story 17443653
        if(task.hasDueDate()) {
            if(task.hasDueTime()) {
                long estimatedTime = task.getValue(Task.ESTIMATED_SECONDS)  * 1000;
                if(estimatedTime <= 0)
                    estimatedTime = DEFAULT_CAL_TIME;
                if (Preferences.getBoolean(R.string.p_end_at_deadline, true)) {
                    values.put("dtstart", dueDate);
                    values.put("dtend", dueDate + estimatedTime);
                }else{
                    values.put("dtstart", dueDate - estimatedTime);
                    values.put("dtend", dueDate);
                }
                // setting a duetime to a previously timeless event requires explicitly setting allDay=0
                values.put("allDay", "0");
                values.put("eventTimezone", TimeZone.getDefault().getID());
            } else {
                values.put("dtstart", tzCorrectedDueDate);
                values.put("dtend", tzCorrectedDueDate);
                values.put("allDay", "1");
            }
        } else {
            values.put("dtstart", tzCorrectedDueDateNow);
            values.put("dtend", tzCorrectedDueDateNow);
            values.put("allDay", "1");
        }
        adjustDateForIcs(values);
    }

    @SuppressWarnings("nls")
    private static void adjustDateForIcs(ContentValues values) {
        if (AndroidUtilities.getSdkVersion() >= 14) {
            if ("1".equals(values.get("allDay"))) {
                values.put("eventTimezone", Time.TIMEZONE_UTC);
            } else {
                values.put("eventTimezone", TimeZone.getDefault().getID());
            }
        }
    }
}
