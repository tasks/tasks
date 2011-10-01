package com.todoroo.astrid.gcal;

import java.util.TimeZone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class GCalHelper {
    /** If task has no estimated time, how early to set a task in calendar (seconds)*/
    private static final long DEFAULT_CAL_TIME = DateUtilities.ONE_HOUR;

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

    public static Uri createTaskEvent(Task task, ContentResolver cr, ContentValues values) {
        String eventuri = getTaskEventUri(task);

        if(!TextUtils.isEmpty(eventuri)) {
            deleteTaskEvent(task);
        }

        try{
            // FIXME test this with empty quickadd and full quickadd and taskedit-page
            Uri uri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);
            values.put("title", task.getValue(Task.TITLE));
            values.put("description", task.getValue(Task.NOTES));
            values.put("hasAlarm", 0);
            values.put("transparency", 0);
            values.put("visibility", 0);
            boolean valuesContainCalendarId = (values.containsKey("calendar_id") &&
                    !TextUtils.isEmpty(values.getAsString("calendar_id")));
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
            Log.e("astrid-gcal", "error-creating-calendar-event", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return null;
    }

    public static void deleteTaskEvent(Task task) {
        String uri;
        if(task.containsNonNullValue(Task.CALENDAR_URI))
            uri = task.getValue(Task.CALENDAR_URI);
        else {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null)
                return;
            uri = task.getValue(Task.CALENDAR_URI);
        }

        if(!TextUtils.isEmpty(uri)) {
            try {
                Uri calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = ContextManager.getContext().getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                boolean deleted = cursor.getCount() == 0;
                cursor.close();

                if (!deleted) {
                    cr.delete(calendarUri, null, null);
                }

                task.setValue(Task.CALENDAR_URI,"");
            } catch (Exception e) {
                Log.e("astrid-gcal", "error-deleting-calendar-event", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
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
                values.put("dtstart", dueDate - estimatedTime);
                values.put("dtend", dueDate);
                // setting a duetime to a previously timeless event requires explicitly setting allDay=0
                values.put("allDay", "0");
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
    }
}