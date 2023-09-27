package org.tasks.calendars;

import static android.provider.BaseColumns._ID;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.Nullable;

import com.todoroo.astrid.data.Task;

import org.tasks.preferences.PermissionChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

public class CalendarEventProvider {

  private static final String[] COLUMNS = {
    _ID,
    CalendarContract.Events.DTSTART,
    CalendarContract.Events.DTEND,
    CalendarContract.Events.TITLE,
    CalendarContract.Events.CALENDAR_ID
  };

  private final ContentResolver contentResolver;
  private final PermissionChecker permissionChecker;

  @Inject
  public CalendarEventProvider(
      @ApplicationContext Context context,
      PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
    contentResolver = context.getContentResolver();
  }

  @Nullable
  public AndroidCalendarEvent getEvent(Uri eventUri) {
    List<AndroidCalendarEvent> events = getCalendarEvents(eventUri, null, null);
    return events.isEmpty() ? null : events.get(0);
  }

  public void deleteEvents(List<String> calendarUris) {
    for (String uri : calendarUris) {
      deleteEvent(uri);
    }
  }

  public void deleteEvent(Task task) {
    if (!permissionChecker.canAccessCalendars()) {
      return;
    }
    String uri = task.getCalendarURI();
    task.setCalendarURI("");
    deleteEvent(uri);
  }

  private void deleteEvent(String eventUri) {
    if (!isNullOrEmpty(eventUri)) {
      deleteEvent(Uri.parse(eventUri));
    }
  }

  private void deleteEvent(Uri eventUri) {
    if (getEvent(eventUri) != null) {
      contentResolver.delete(eventUri, null, null);
    }
  }

  public List<AndroidCalendarEvent> getEventsBetween(long start, long end) {
    return getCalendarEvents(
        CalendarContract.Events.CONTENT_URI,
        CalendarContract.Events.DTSTART + " > ? AND " + CalendarContract.Events.DTSTART + " < ?",
        new String[] {Long.toString(start), Long.toString(end)});
  }

  private List<AndroidCalendarEvent> getCalendarEvents(
      Uri uri, String selection, String[] selectionArgs) {
    if (!permissionChecker.canAccessCalendars()) {
      return Collections.emptyList();
    }

    List<AndroidCalendarEvent> events = new ArrayList<>();
    try (Cursor cursor = contentResolver.query(uri, COLUMNS, selection, selectionArgs, null)) {
      if (cursor != null && cursor.getCount() > 0) {
        int idIndex = cursor.getColumnIndex(_ID);
        int startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART);
        int endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND);
        int titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE);
        int calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID);
        while (cursor.moveToNext()) {
          long id = cursor.getLong(idIndex);
          events.add(
              new AndroidCalendarEvent(
                  id,
                  cursor.getString(titleIndex),
                  cursor.getLong(startIndex),
                  cursor.getLong(endIndex),
                  cursor.getInt(calendarIdIndex)));
        }
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    return events;
  }
}
