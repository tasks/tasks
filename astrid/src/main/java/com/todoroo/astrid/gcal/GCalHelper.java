/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.TimeZone;

import javax.inject.Inject;

import static com.todoroo.astrid.gcal.Calendars.getCalendarContentUri;

public class GCalHelper {

    private static final Logger log = LoggerFactory.getLogger(GCalHelper.class);

    /** If task has no estimated time, how early to set a task in calendar (seconds)*/
    private static final long DEFAULT_CAL_TIME = DateUtilities.ONE_HOUR;

    public static final String CALENDAR_ID_COLUMN = "calendar_id"; //$NON-NLS-1$

    private final Context context;
    private final TaskService taskService;
    private final Preferences preferences;

    @Inject
    public GCalHelper(@ForApplication Context context, TaskService taskService, Preferences preferences) {
        this.context = context;
        this.taskService = taskService;
        this.preferences = preferences;
    }

    public String getTaskEventUri(Task task) {
        String uri;
        if (!TextUtils.isEmpty(task.getCalendarURI())) {
            uri = task.getCalendarURI();
        } else {
            task = taskService.fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null) {
                return null;
            }
            uri = task.getCalendarURI();
        }

        return uri;
    }

    public void createTaskEventIfEnabled(Task t) {
        if (!t.hasDueDate()) {
            return;
        }
        createTaskEventIfEnabled(t, true);
    }

    public String getDefaultCalendar() {
        return preferences.getStringValue(R.string.gcal_p_default);
    }

    private void createTaskEventIfEnabled(Task t, boolean deleteEventIfExists) {
        boolean gcalCreateEventEnabled = getDefaultCalendar() != null
            && !getDefaultCalendar().equals("-1"); //$NON-NLS-1$
        if (gcalCreateEventEnabled) {
            ContentResolver cr = context.getContentResolver();
            Uri calendarUri = createTaskEvent(t, cr, new ContentValues(), deleteEventIfExists);
            if (calendarUri != null) {
                t.setCalendarUri(calendarUri.toString());
            }
        }
    }

    public Uri createTaskEvent(Task task, ContentResolver cr, ContentValues values) {
        return createTaskEvent(task, cr, values, true);
    }

    public Uri createTaskEvent(Task task, ContentResolver cr, ContentValues values, boolean deleteEventIfExists) {
        String eventuri = getTaskEventUri(task);

        if(!TextUtils.isEmpty(eventuri) && deleteEventIfExists) {
            deleteTaskEvent(task);
        }

        try{
            Uri uri = getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);
            values.put("title", task.getTitle());
            values.put("description", task.getNotes());
            values.put("hasAlarm", 0);
            if (AndroidUtilities.getSdkVersion() < 14) {
                values.put("transparency", 0);
                values.put("visibility", 0);
            }
            boolean valuesContainCalendarId = (values.containsKey(CALENDAR_ID_COLUMN) &&
                    !TextUtils.isEmpty(values.getAsString(CALENDAR_ID_COLUMN)));
            if (!valuesContainCalendarId) {
                String calendarId = getDefaultCalendar();
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
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public void rescheduleRepeatingTask(Task task, ContentResolver cr) {
        String taskUri = getTaskEventUri(task);
        if (TextUtils.isEmpty(taskUri)) {
            return;
        }

        Uri eventUri = Uri.parse(taskUri);
        String calendarId = getCalendarId(eventUri, cr);
        if (calendarId == null) { // Bail out, no calendar id
            task.setCalendarUri(""); //$NON-NLS-1$
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(CALENDAR_ID_COLUMN, calendarId);

        Uri uri = createTaskEvent(task, cr, cv, false);
        task.setCalendarUri(uri.toString());
    }

    private static String getCalendarId(Uri uri, ContentResolver cr) {
        Cursor calendar = cr.query(uri, new String[] { CALENDAR_ID_COLUMN }, null, null, null);
        try {
            calendar.moveToFirst();
            return calendar.getString(0);
        } catch (CursorIndexOutOfBoundsException e) {
            log.error(e.getMessage(), e);
            return null;
        } finally  {
            calendar.close();
        }
    }

    public boolean deleteTaskEvent(Task task) {
        boolean eventDeleted = false;
        String uri;
        if(task.containsNonNullValue(Task.CALENDAR_URI)) {
            uri = task.getCalendarURI();
        } else {
            task = taskService.fetchById(task.getId(), Task.CALENDAR_URI);
            if(task == null) {
                return false;
            }
            uri = task.getCalendarURI();
        }

        if(!TextUtils.isEmpty(uri)) {
            try {
                Uri calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = context.getContentResolver();
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

                task.setCalendarUri( "");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return eventDeleted;
    }

    void createStartAndEndDate(Task task, ContentValues values) {
        long dueDate = task.getDueDate();
        long tzCorrectedDueDate = dueDate + TimeZone.getDefault().getOffset(dueDate);
        long tzCorrectedDueDateNow = DateUtilities.now() + TimeZone.getDefault().getOffset(DateUtilities.now());
        // FIXME: doesnt respect timezones, see story 17443653
        if(task.hasDueDate()) {
            if(task.hasDueTime()) {
                long estimatedTime = task.getEstimatedSeconds()  * 1000;
                if(estimatedTime <= 0) {
                    estimatedTime = DEFAULT_CAL_TIME;
                }
                if (preferences.getBoolean(R.string.p_end_at_deadline, true)) {
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

    private static void adjustDateForIcs(ContentValues values) {
        if (AndroidUtilities.getSdkVersion() >= 14) {
            if ("1".equals(values.get("allDay"))) {
                values.put("eventTimezone", Time.TIMEZONE_UTC);
            } else {
                values.put("eventTimezone", TimeZone.getDefault().getID());
            }
        }
    }

    public static class CalendarResult {
        /** calendar names */
        public String[] calendars;

        /** calendar ids. null entry -> use default */
        public String[] calendarIds;

        /** default selection index */
        public int defaultIndex = -1;
    }

    /**
     * Appends all user-modifiable calendars to listPreference.
     */
    public CalendarResult getCalendars() {
        ContentResolver cr = context.getContentResolver();
        Resources r = context.getResources();

        Cursor c = cr.query(getCalendarContentUri(Calendars.CALENDAR_CONTENT_CALENDARS), Calendars.CALENDARS_PROJECTION,
                Calendars.CALENDARS_WHERE, null, Calendars.CALENDARS_SORT);
        try {
            // Fetch the current setting. Invalid calendar id will
            // be changed to default value.
            String defaultSetting = getDefaultCalendar();

            CalendarResult result = new CalendarResult();

            if (c == null || c.getCount() == 0) {
                // Something went wrong when querying calendars. Only offer them
                // the system default choice
                result.calendars = new String[] {
                        r.getString(R.string.gcal_GCP_default) };
                result.calendarIds = new String[] { null };
                result.defaultIndex = 0;
                return result;
            }

            int calendarCount = c.getCount();

            result.calendars = new String[calendarCount];
            result.calendarIds = new String[calendarCount];

            // Iterate calendars one by one, and fill up the list preference
            int row = 0;
            int idColumn = c.getColumnIndex(Calendars.ID_COLUMN_NAME);
            int nameColumn = c.getColumnIndex(Calendars.CALENDARS_DISPLAY_COL);
            while (c.moveToNext()) {
                String id = c.getString(idColumn);
                String name = c.getString(nameColumn);
                result.calendars[row] = name;
                result.calendarIds[row] = id;

                // We found currently selected calendar
                if (defaultSetting != null && defaultSetting.equals(id)) {
                    result.defaultIndex = row;
                }

                row++;
            }

            if (result.defaultIndex >= calendarCount) {
                result.defaultIndex = 0;
            }

            return result;
        } finally {
            if(c != null) {
                c.close();
            }
        }
    }
}
