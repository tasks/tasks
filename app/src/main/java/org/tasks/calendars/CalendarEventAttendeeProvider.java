package org.tasks.calendars;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import timber.log.Timber;

public class CalendarEventAttendeeProvider {

  private static final String[] COLUMNS = {
    CalendarContract.Attendees.ATTENDEE_NAME, CalendarContract.Attendees.ATTENDEE_EMAIL,
  };

  private final PermissionChecker permissionChecker;
  private final ContentResolver contentResolver;

  @Inject
  public CalendarEventAttendeeProvider(
      @ForApplication Context context, PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
    contentResolver = context.getContentResolver();
  }

  public List<AndroidCalendarEventAttendee> getAttendees(long id) {
    if (!permissionChecker.canAccessCalendars()) {
      return Collections.emptyList();
    }

    List<AndroidCalendarEventAttendee> attendees = new ArrayList<>();
    Cursor cursor = null;
    try {
      //noinspection ResourceType
      cursor =
          contentResolver.query(
              CalendarContract.Attendees.CONTENT_URI,
              COLUMNS,
              CalendarContract.Attendees.EVENT_ID + " = ? ",
              new String[] {Long.toString(id)},
              null);
      if (cursor != null && cursor.getCount() > 0) {
        int emailIndex = cursor.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_EMAIL);
        int nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_NAME);
        while (cursor.moveToNext()) {
          attendees.add(
              new AndroidCalendarEventAttendee(
                  cursor.getString(nameIndex), cursor.getString(emailIndex)));
        }
      }
    } catch (Exception e) {
      Timber.e(e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return attendees;
  }
}
