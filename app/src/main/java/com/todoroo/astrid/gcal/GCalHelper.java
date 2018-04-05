/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.Time;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.TimeZone;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.calendars.AndroidCalendarEvent;
import org.tasks.calendars.CalendarEventProvider;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class GCalHelper {

  /** If task has no estimated time, how early to set a task in calendar (seconds) */
  private static final long DEFAULT_CAL_TIME = DateUtilities.ONE_HOUR;

  private final TaskDao taskDao;
  private final Preferences preferences;
  private final PermissionChecker permissionChecker;
  private final CalendarEventProvider calendarEventProvider;
  private final ContentResolver cr;

  @Inject
  public GCalHelper(
      @ForApplication Context context,
      TaskDao taskDao,
      Preferences preferences,
      PermissionChecker permissionChecker,
      CalendarEventProvider calendarEventProvider) {
    this.taskDao = taskDao;
    this.preferences = preferences;
    this.permissionChecker = permissionChecker;
    this.calendarEventProvider = calendarEventProvider;
    cr = context.getContentResolver();
  }

  private String getTaskEventUri(Task task) {
    String uri;
    if (!TextUtils.isEmpty(task.getCalendarURI())) {
      uri = task.getCalendarURI();
    } else {
      task = taskDao.fetch(task.getId());
      if (task == null) {
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

  private void createTaskEventIfEnabled(Task t, boolean deleteEventIfExists) {
    if (preferences.isDefaultCalendarSet()) {
      Uri calendarUri = createTaskEvent(t, new ContentValues(), deleteEventIfExists);
      if (calendarUri != null) {
        t.setCalendarUri(calendarUri.toString());
      }
    }
  }

  public Uri createTaskEvent(Task task, ContentValues values) {
    return createTaskEvent(task, values, true);
  }

  private Uri createTaskEvent(Task task, ContentValues values, boolean deleteEventIfExists) {
    if (!permissionChecker.canAccessCalendars()) {
      return null;
    }

    String eventuri = getTaskEventUri(task);

    if (!TextUtils.isEmpty(eventuri) && deleteEventIfExists) {
      calendarEventProvider.deleteEvent(task);
    }

    try {
      values.put(CalendarContract.Events.TITLE, task.getTitle());
      values.put(CalendarContract.Events.DESCRIPTION, task.getNotes());
      values.put(CalendarContract.Events.HAS_ALARM, 0);
      boolean valuesContainCalendarId =
          (values.containsKey(CalendarContract.Events.CALENDAR_ID)
              && !TextUtils.isEmpty(values.getAsString(CalendarContract.Events.CALENDAR_ID)));
      if (!valuesContainCalendarId) {
        String calendarId = preferences.getDefaultCalendar();
        if (!TextUtils.isEmpty(calendarId)) {
          values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        }
      }

      createStartAndEndDate(task, values);

      //noinspection MissingPermission
      Uri eventUri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
      cr.notifyChange(eventUri, null);

      return eventUri;
    } catch (Exception e) {
      // won't work on emulator
      Timber.e(e);
    }

    return null;
  }

  public void rescheduleRepeatingTask(Task task) {
    String taskUri = getTaskEventUri(task);
    if (TextUtils.isEmpty(taskUri)) {
      return;
    }

    Uri eventUri = Uri.parse(taskUri);

    AndroidCalendarEvent event = calendarEventProvider.getEvent(eventUri);
    if (event == null) {
      task.setCalendarUri("");
      return;
    }
    ContentValues cv = new ContentValues();
    cv.put(CalendarContract.Events.CALENDAR_ID, event.getCalendarId());

    Uri uri = createTaskEvent(task, cv, false);
    task.setCalendarUri(uri.toString());
  }

  public void createStartAndEndDate(Task task, ContentValues values) {
    long dueDate = task.getDueDate();
    long tzCorrectedDueDate = dueDate + TimeZone.getDefault().getOffset(dueDate);
    long tzCorrectedDueDateNow =
        DateUtilities.now() + TimeZone.getDefault().getOffset(DateUtilities.now());
    // FIXME: doesnt respect timezones, see story 17443653
    if (task.hasDueDate()) {
      if (task.hasDueTime()) {
        long estimatedTime = task.getEstimatedSeconds() * 1000;
        if (estimatedTime <= 0) {
          estimatedTime = DEFAULT_CAL_TIME;
        }
        if (preferences.getBoolean(R.string.p_end_at_deadline, true)) {
          values.put(CalendarContract.Events.DTSTART, dueDate);
          values.put(CalendarContract.Events.DTEND, dueDate + estimatedTime);
        } else {
          values.put(CalendarContract.Events.DTSTART, dueDate - estimatedTime);
          values.put(CalendarContract.Events.DTEND, dueDate);
        }
        // setting a duetime to a previously timeless event requires explicitly setting allDay=0
        values.put(CalendarContract.Events.ALL_DAY, "0");
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
      } else {
        values.put(CalendarContract.Events.DTSTART, tzCorrectedDueDate);
        values.put(CalendarContract.Events.DTEND, tzCorrectedDueDate);
        values.put(CalendarContract.Events.ALL_DAY, "1");
      }
    } else {
      values.put(CalendarContract.Events.DTSTART, tzCorrectedDueDateNow);
      values.put(CalendarContract.Events.DTEND, tzCorrectedDueDateNow);
      values.put(CalendarContract.Events.ALL_DAY, "1");
    }
    if ("1".equals(values.get(CalendarContract.Events.ALL_DAY))) {
      values.put(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC);
    } else {
      values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    }
  }
}
