package org.tasks.calendars;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.provider.BaseColumns._ID;

public class CalendarEventProvider {

    private static final String[] COLUMNS = {
            _ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.CALENDAR_ID
    };

    private final ContentResolver contentResolver;
    private final TaskDao taskDao;
    private final PermissionChecker permissionChecker;
    private final CalendarEventAttendeeProvider calendarEventAttendeeProvider;

    @Inject
    public CalendarEventProvider(@ForApplication Context context, PermissionChecker permissionChecker,
                                 CalendarEventAttendeeProvider calendarEventAttendeeProvider,
                                 TaskDao taskDao) {
        this.permissionChecker = permissionChecker;
        this.calendarEventAttendeeProvider = calendarEventAttendeeProvider;
        this.taskDao = taskDao;
        contentResolver = context.getContentResolver();
    }

    @Nullable
    public AndroidCalendarEvent getEvent(long eventId) {
        List<AndroidCalendarEvent> events = getCalendarEvents(CalendarContract.Events.CONTENT_URI,
                _ID + " = ?", new String[] { Long.toString(eventId) });
        return events.isEmpty() ? null : events.get(0);
    }

    @Nullable
    public AndroidCalendarEvent getEvent(Uri eventUri) {
        List<AndroidCalendarEvent> events = getCalendarEvents(eventUri, null, null);
        return events.isEmpty() ? null : events.get(0);
    }

    public boolean deleteEvent(Task task) {
        if (!task.containsNonNullValue(Task.CALENDAR_URI)) {
            task = taskDao.fetch(task.getId(), Task.CALENDAR_URI);
            if(task == null) {
                return false;
            }
        }
        String uri = task.getCalendarURI();
        task.setCalendarUri("");
        return deleteEvent(uri);
    }

    private boolean deleteEvent(String eventUri) {
        return !TextUtils.isEmpty(eventUri) && deleteEvent(Uri.parse(eventUri));
    }

    private boolean deleteEvent(Uri eventUri) {
        return getEvent(eventUri) != null && contentResolver.delete(eventUri, null, null) > 0;
    }

    public List<AndroidCalendarEvent> getEventsBetween(long start, long end) {
        return getCalendarEvents(
                CalendarContract.Events.CONTENT_URI,
                CalendarContract.Events.DTSTART + " > ? AND " + CalendarContract.Events.DTSTART + " < ?",
                new String[] { Long.toString(start), Long.toString(end) });
    }

    private List<AndroidCalendarEvent> getCalendarEvents(Uri uri, String selection, String[] selectionArgs) {
        if (!permissionChecker.canAccessCalendars()) {
            return Collections.emptyList();
        }

        List<AndroidCalendarEvent> events = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, COLUMNS, selection, selectionArgs, null);
            if (cursor != null && cursor.getCount() > 0) {
                int idIndex = cursor.getColumnIndex(_ID);
                int startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART);
                int endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND);
                int titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE);
                int calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    events.add(new AndroidCalendarEvent(
                            id,
                            cursor.getString(titleIndex),
                            cursor.getLong(startIndex),
                            cursor.getLong(endIndex),
                            cursor.getInt(calendarIdIndex),
                            calendarEventAttendeeProvider.getAttendees(id)));
                }
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }
}
