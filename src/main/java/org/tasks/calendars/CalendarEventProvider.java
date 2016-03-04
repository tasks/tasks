package org.tasks.calendars;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;

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
            CalendarContract.Events.TITLE
    };

    private final ContentResolver contentResolver;
    private final PermissionChecker permissionChecker;

    @Inject
    public CalendarEventProvider(@ForApplication Context context, PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
        contentResolver = context.getContentResolver();
    }

    @Nullable
    public AndroidCalendarEvent getEvent(long eventId) {
        List<AndroidCalendarEvent> events = getCalendarEvents(CalendarContract.Events.CONTENT_URI,
                _ID + " = ?", new String[] { Long.toString(eventId) });
        return events.isEmpty() ? null : events.get(0);
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
                while (cursor.moveToNext()) {
                    events.add(new AndroidCalendarEvent(
                            cursor.getLong(idIndex),
                            cursor.getString(titleIndex),
                            cursor.getLong(startIndex),
                            cursor.getLong(endIndex)));
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
