package org.tasks.caldav;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY_TIME;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.startOfDay;

import at.bitfire.ical4android.DateUtils;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import org.tasks.data.CaldavTask;
import timber.log.Timber;

public class CaldavConverter {

  static final DateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

  public static void apply(Task local, at.bitfire.ical4android.Task remote) {
    Completed completedAt = remote.getCompletedAt();
    if (completedAt != null) {
      local.setCompletionDate(remote.getCompletedAt().getDate().getTime());
    } else if (remote.getStatus() == Status.VTODO_COMPLETED) {
      if (!local.isCompleted()) {
        local.setCompletionDate(now());
      }
    } else {
      local.setCompletionDate(0L);
    }

    Long createdAt = remote.getCreatedAt();
    if (createdAt != null) {
      local.setCreationDate(newDateTime(createdAt).toLocal().getMillis());
    }
    local.setTitle(remote.getSummary());
    local.setNotes(remote.getDescription());
    local.setPriority(fromRemote(remote.getPriority()));
    local.setRecurrence(remote.getRRule());
    Due due = remote.getDue();
    if (due == null) {
      local.setDueDate(0L);
    } else {
      Date dueDate = due.getDate();
      if (dueDate instanceof DateTime) {
        local.setDueDate(Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getTime()));
      } else {
        try {
          local.setDueDate(
              Task.createDueDate(
                  URGENCY_SPECIFIC_DAY, DUE_DATE_FORMAT.parse(due.getValue()).getTime()));
        } catch (ParseException e) {
          Timber.e(e);
        }
      }
    }
    iCalendar.Companion.apply(remote.getDtStart(), local);
  }

  public static @Priority int fromRemote(int remotePriority) {
    // https://tools.ietf.org/html/rfc5545#section-3.8.1.9
    if (remotePriority == 0) {
      return Priority.NONE;
    }
    if (remotePriority == 5) {
      return Priority.MEDIUM;
    }
    return remotePriority < 5 ? Priority.HIGH : Priority.LOW;
  }

  public static int toRemote(int remotePriority, int localPriority) {
    switch (localPriority) {
      case Priority.NONE:
        return 0;

      case Priority.MEDIUM:
        return 5;

      case Priority.HIGH:
        return remotePriority < 5 ? Math.max(1, remotePriority) : 1;

      default:
        return remotePriority > 5 ? Math.min(9, remotePriority) : 9;
    }
  }

  public static at.bitfire.ical4android.Task toCaldav(CaldavTask caldavTask, Task task) {
    at.bitfire.ical4android.Task remote = null;
    try {
      if (!isNullOrEmpty(caldavTask.getVtodo())) {
        remote = iCalendar.Companion.fromVtodo(caldavTask.getVtodo());
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    if (remote == null) {
      remote = new at.bitfire.ical4android.Task();
    }
    remote.setCreatedAt(newDateTime(task.getCreationDate()).toUTC().getMillis());
    remote.setSummary(task.getTitle());
    remote.setDescription(task.getNotes());
    boolean allDay = !task.hasDueTime() && !task.hasStartTime();
    long dueDate = task.hasDueTime() ? task.getDueDate() : startOfDay(task.getDueDate());
    long startDate = task.hasStartTime() ? task.getHideUntil() : startOfDay(task.getHideUntil());
    if (dueDate > 0) {
      startDate = Math.min(dueDate, startDate);
      remote.setDue(new Due(allDay ? new Date(dueDate) : getDateTime(dueDate)));
    } else {
      remote.setDue(null);
    }
    if (startDate > 0) {
      remote.setDtStart(new DtStart(allDay ? new Date(startDate) : getDateTime(startDate)));
    } else {
      remote.setDtStart(null);
    }
    if (task.isCompleted()) {
      remote.setCompletedAt(new Completed(new DateTime(task.getCompletionDate())));
      remote.setStatus(Status.VTODO_COMPLETED);
      remote.setPercentComplete(100);
    } else if (remote.getCompletedAt() != null) {
      remote.setCompletedAt(null);
      remote.setStatus(null);
      remote.setPercentComplete(null);
    }
    if (task.isRecurring()) {
      try {
        RRule rrule = new RRule(task.getRecurrenceWithoutFrom().replace("RRULE:", ""));
        long repeatUntil = task.getRepeatUntil();
        rrule
            .getRecur()
            .setUntil(
                repeatUntil > 0 ? new DateTime(newDateTime(repeatUntil).toUTC().getMillis()) : null);
        String sanitized = Task.sanitizeRRule(rrule.getValue()); // ical4j adds COUNT=-1 if there is an UNTIL value
        remote.setRRule(new RRule(sanitized));
        if (remote.getDtStart() == null) {
          Date date = remote.getDue() != null ? remote.getDue().getDate() : new Date();
          remote.setDtStart(new DtStart(date));
        }
      } catch (ParseException e) {
        Timber.e(e);
      }
    } else {
      remote.setRRule(null);
    }
    remote.setLastModified(newDateTime(task.getModificationDate()).toUTC().getMillis());
    remote.setPriority(toRemote(remote.getPriority(), task.getPriority()));
    iCalendar.Companion.setParent(remote, task.getParent() == 0 ? null : caldavTask.getRemoteParent());

    return remote;
  }

  private static DateTime getDateTime(long timestamp) {
    net.fortuna.ical4j.model.TimeZone tz =
        DateUtils.INSTANCE.ical4jTimeZone(TimeZone.getDefault().getID());
    DateTime dateTime = new DateTime(tz != null
        ? timestamp
        : new org.tasks.time.DateTime(timestamp).toUTC().getMillis());
    dateTime.setTimeZone(tz);
    return dateTime;
  }
}
